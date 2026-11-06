package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EnderIO conduit bundles expose their geometry as {@code List<CollidableComponent>}; each
 * component has a public {@code bound} BoundingBox. A block can contain several conduit types
 * plus connector faces, so selection picks the type whose component contains the crosshair hit
 * instead of drawing every group.
 */
public final class EnderIoShapeProvider implements ShapeProvider {

    private static final Class<?> BUNDLE_INTERFACE = classOrNull("crazypants.enderio.conduit.IConduitBundle");
    private static final Method GET_COMPONENTS = methodOrNull(BUNDLE_INTERFACE, "getCollidableComponents");
    private static Field boundField;
    private static Field dataField;
    private static Field conduitTypeField;

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (BUNDLE_INTERFACE == null || GET_COMPONENTS == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!BUNDLE_INTERFACE.isInstance(tile)) {
            return false;
        }
        try {
            List<?> components = (List<?>) GET_COMPONENTS.invoke(tile);
            if (components == null || components.isEmpty()) {
                return false;
            }

            List<Component> physical = new ArrayList<Component>();
            for (Object component : components) {
                if (boundField == null) {
                    boundField = component.getClass().getField("bound");
                    dataField = component.getClass().getField("data");
                    conduitTypeField = component.getClass().getField("conduitType");
                }

                Object data = dataField.get(component);
                if (isNonPhysical(data)) {
                    continue;
                }

                Object bound = boundField.get(component);
                if (bound == null) {
                    continue;
                }
                Class<?> boxClass = bound.getClass();
                float minX = boxClass.getField("minX").getFloat(bound);
                float minY = boxClass.getField("minY").getFloat(bound);
                float minZ = boxClass.getField("minZ").getFloat(bound);
                float maxX = boxClass.getField("maxX").getFloat(bound);
                float maxY = boxClass.getField("maxY").getFloat(bound);
                float maxZ = boxClass.getField("maxZ").getFloat(bound);
                if (maxX - minX <= 0.0001F || maxY - minY <= 0.0001F || maxZ - minZ <= 0.0001F) {
                    continue;
                }
                physical.add(new Component((Class<?>) conduitTypeField.get(component),
                        AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ)));
            }
            if (physical.isEmpty()) {
                return false;
            }

            // 不再单独描「命中的那一个组件」：EnderIO 的端部盖子和中心核都是很小的盒子，
            // 只描它们会得到线中间/两端的一小截，毫无意义。命中任何组件都按 conduitType
            // 分组，输出该类型的全部组件，得到完整的一整条线。
            Component hit = hitComponent(physical, target, x, y, z);
            Class<?> hitType = hit != null ? hit.type : null;
            Map<Class<?>, List<AxisAlignedBB>> groups = new LinkedHashMap<Class<?>, List<AxisAlignedBB>>();
            for (Component component : physical) {
                if (hitType != null && component.type != hitType) {
                    continue;
                }
                List<AxisAlignedBB> boxes = groups.get(component.type);
                if (boxes == null) {
                    boxes = new ArrayList<AxisAlignedBB>();
                    groups.put(component.type, boxes);
                }
                boxes.add(AxisAlignedBB.getBoundingBox(
                        component.box.minX + x, component.box.minY + y, component.box.minZ + z,
                        component.box.maxX + x, component.box.maxY + y, component.box.maxZ + z));
            }
            if (groups.isEmpty()) {
                return false;
            }

            boolean added = false;
            for (List<AxisAlignedBB> boxes : groups.values()) {
                if (boxes.isEmpty()) {
                    continue;
                }
                if (boxes.size() == 1) {
                    out.add(OutlineShape.box(boxes.get(0), false));
                } else {
                    out.add(OutlineShape.union(boxes));
                }
                added = true;
            }
            return added;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Component hitComponent(List<Component> components, MovingObjectPosition target,
                                          int x, int y, int z) {
        if (target == null || target.hitVec == null) {
            return null;
        }
        double hitX = target.hitVec.xCoord - x;
        double hitY = target.hitVec.yCoord - y;
        double hitZ = target.hitVec.zCoord - z;
        // 按「命中点到盒面的距离」取最近，而不是「体积最小」。
        // 体积最小的往往就是中心核或端盖，会让描边跑到那一小截上去。
        Component best = null;
        double bestDistance = Double.MAX_VALUE;
        double bestVolume = Double.MAX_VALUE;
        for (Component component : components) {
            AxisAlignedBB box = component.box;
            double distance = distanceToBox(box, hitX, hitY, hitZ);
            if (distance > bestDistance + 1.0E-4D) {
                continue;
            }
            double volume = (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
            if (distance < bestDistance - 1.0E-4D || volume < bestVolume) {
                bestDistance = distance;
                bestVolume = volume;
                best = component;
            }
        }
        return best;
    }

    /** 点到 AABB 的距离；点在盒内时为 0。 */
    private static double distanceToBox(AxisAlignedBB box, double x, double y, double z) {
        double dx = Math.max(0.0D, Math.max(box.minX - x, x - box.maxX));
        double dy = Math.max(0.0D, Math.max(box.minY - y, y - box.maxY));
        double dz = Math.max(0.0D, Math.max(box.minZ - z, z - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static boolean isNonPhysical(Object data) {
        if (data == null) {
            return false;
        }
        if ("ColorController".equals(data)) {
            return true;
        }
        return "INTERNAL".equals(data.toString());
    }

    private static final class Component {
        private final Class<?> type;
        private final AxisAlignedBB box;

        private Component(Class<?> type, AxisAlignedBB box) {
            this.type = type;
            this.box = box;
        }
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
