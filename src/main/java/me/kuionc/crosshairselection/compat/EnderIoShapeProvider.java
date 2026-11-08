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
import static me.kuionc.crosshairselection.compat.Reflect.*;

/**
 * EnderIO conduit bundles expose their geometry as {@code List<CollidableComponent>}; each
 * component has a public {@code bound} BoundingBox. A block can contain several conduit types
 * plus connector faces, so selection picks the type whose component contains the crosshair hit
 * instead of drawing every group.
 */
public final class EnderIoShapeProvider implements ShapeProvider {

    private static final Class<?> BUNDLE_INTERFACE = classOrNull("crazypants.enderio.conduit.IConduitBundle");
    private static final Method GET_COMPONENTS = methodOrNull(BUNDLE_INTERFACE, "getCollidableComponents");
    private static final Method GET_CONNECTORS = methodOrNull(BUNDLE_INTERFACE, "getConnectors");
    /** 端盖/盘面比其他组件稍厚一点，描边才看得清（单位：格）。 */
    private static final double CONNECTOR_GROW = 0.008D;
    private static Field boundField;
    private static Field dataField;
    private static Field conduitTypeField;
    private static Field dirField;

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
                    dirField = component.getClass().getField("dir");
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
                        AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ),
                        (net.minecraftforge.common.util.ForgeDirection) dirField.get(component),
                        component));
            }
            if (physical.isEmpty()) {
                return false;
            }

            // 命中任何组件都按 conduitType 分组，输出该类型的全部组件，得到完整的一整条线。
            // 但端盖/盘面是个例外：每个方向都有一个，全部画出来会「点一个面的盘面，
            // 其他面的盘面也一起亮」。所以连接面组件只保留被命中的那一个。
            Component hit = hitComponent(physical, target, x, y, z);
            Class<?> hitType = hit != null ? hit.type : null;
            List<?> connectors = connectors(tile);
            Map<Class<?>, List<AxisAlignedBB>> groups = new LinkedHashMap<Class<?>, List<AxisAlignedBB>>();
            for (Component component : physical) {
                if (hitType != null && component.type != hitType) {
                    continue;
                }
                if (hitType == null && hit != null && component.dir != hit.dir) {
                    continue;
                }
                boolean connector = isConnector(component.source, connectors);
                if (connector && (hit == null || !sameSource(component.source, hit.source))) {
                    continue;
                }
                AxisAlignedBB box = component.box;
                if (connector) {
                    box = box.expand(CONNECTOR_GROW, CONNECTOR_GROW, CONNECTOR_GROW);
                }
                List<AxisAlignedBB> boxes = groups.get(component.type);
                if (boxes == null) {
                    boxes = new ArrayList<AxisAlignedBB>();
                    groups.put(component.type, boxes);
                }
                boxes.add(AxisAlignedBB.getBoundingBox(
                        box.minX + x, box.minY + y, box.minZ + z,
                        box.maxX + x, box.maxY + y, box.maxZ + z));
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

    /** 当前线束的连接面/端盖组件；拿不到时返回空列表。 */
    private static List<?> connectors(TileEntity tile) {
        if (GET_CONNECTORS == null) {
            return java.util.Collections.emptyList();
        }
        try {
            Object value = GET_CONNECTORS.invoke(tile);
            return value instanceof List ? (List<?>) value : java.util.Collections.emptyList();
        } catch (Throwable ignored) {
            return java.util.Collections.emptyList();
        }
    }

    private static boolean isConnector(Object source, List<?> connectors) {
        for (Object connector : connectors) {
            if (sameSource(source, connector)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameSource(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
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
        private final net.minecraftforge.common.util.ForgeDirection dir;
        private final Object source;

        private Component(Class<?> type, AxisAlignedBB box,
                          net.minecraftforge.common.util.ForgeDirection dir, Object source) {
            this.type = type;
            this.box = box;
            this.dir = dir;
            this.source = source;
        }
    }

}
