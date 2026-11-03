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
    private static final Method GET_CONNECTORS = methodOrNull(BUNDLE_INTERFACE, "getConnectors");
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
                        AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ), component));
            }
            if (physical.isEmpty()) {
                return false;
            }

            Component hit = hitComponent(physical, target, x, y, z);
            if (hit != null && isConnector(hit.source, tile)) {
                out.add(OutlineShape.box(AxisAlignedBB.getBoundingBox(
                        hit.box.minX + x, hit.box.minY + y, hit.box.minZ + z,
                        hit.box.maxX + x, hit.box.maxY + y, hit.box.maxZ + z), false));
                return true;
            }

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

    private static boolean isConnector(Object source, TileEntity tile) {
        if (GET_CONNECTORS == null || source == null) {
            return false;
        }
        try {
            Object value = GET_CONNECTORS.invoke(tile);
            if (!(value instanceof Iterable)) {
                return false;
            }
            for (Object connector : (Iterable<?>) value) {
                if (connector == source || (connector != null && connector.equals(source))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
            // Treat unknown components as non-connectors.
        }
        return false;
    }

    private static Component hitComponent(List<Component> components, MovingObjectPosition target,
                                          int x, int y, int z) {
        if (target == null || target.hitVec == null) {
            return null;
        }
        double hitX = target.hitVec.xCoord - x;
        double hitY = target.hitVec.yCoord - y;
        double hitZ = target.hitVec.zCoord - z;
        Component best = null;
        double bestVolume = Double.MAX_VALUE;
        for (Component component : components) {
            AxisAlignedBB box = component.box;
            if (hitX >= box.minX - 0.002D && hitX <= box.maxX + 0.002D
                    && hitY >= box.minY - 0.002D && hitY <= box.maxY + 0.002D
                    && hitZ >= box.minZ - 0.002D && hitZ <= box.maxZ + 0.002D) {
                double volume = (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
                if (volume < bestVolume) {
                    bestVolume = volume;
                    best = component;
                }
            }
        }
        return best;
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
        private final Object source;

        private Component(Class<?> type, AxisAlignedBB box, Object source) {
            this.type = type;
            this.box = box;
            this.source = source;
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
