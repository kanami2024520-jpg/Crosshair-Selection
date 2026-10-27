package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EnderIO conduit bundles expose their geometry as {@code List<CollidableComponent>}; each
 * component has a public {@code bound} BoundingBox. The list also contains non-physical helpers
 * (ColorController and ConduitConnectorType.INTERNAL), which are ignored here.
 */
public final class EnderIoShapeProvider implements ShapeProvider {

    private static final Class<?> BUNDLE_INTERFACE = classOrNull("crazypants.enderio.conduit.IConduitBundle");
    private static final Method GET_COMPONENTS = methodOrNull(BUNDLE_INTERFACE, "getCollidableComponents");
    private static Field boundField;
    private static Field dataField;
    private static Field conduitTypeField;

    @Override
    public boolean collect(World world, int x, int y, int z, Block block, List<OutlineShape> out) {
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

            Map<Class<?>, List<AxisAlignedBB>> groups = new LinkedHashMap<Class<?>, List<AxisAlignedBB>>();
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

                Class<?> conduitType = (Class<?>) conduitTypeField.get(component);
                List<AxisAlignedBB> boxes = groups.get(conduitType);
                if (boxes == null) {
                    boxes = new ArrayList<AxisAlignedBB>();
                    groups.put(conduitType, boxes);
                }
                boxes.add(AxisAlignedBB.getBoundingBox(
                        x + minX, y + minY, z + minZ,
                        x + maxX, y + maxY, z + maxZ));
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

    private static boolean isNonPhysical(Object data) {
        if (data == null) {
            return false;
        }
        if ("ColorController".equals(data)) {
            return true;
        }
        return "INTERNAL".equals(data.toString());
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
