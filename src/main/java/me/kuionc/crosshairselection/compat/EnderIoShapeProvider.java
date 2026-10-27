package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * EnderIO conduit bundles expose their real collision geometry as
 * {@code List<CollidableComponent>}; each component has a public {@code bound} BoundingBox.
 */
public final class EnderIoShapeProvider implements ShapeProvider {

    private static final Class<?> BUNDLE_INTERFACE = classOrNull("crazypants.enderio.conduit.IConduitBundle");
    private static final Method GET_COMPONENTS = methodOrNull(BUNDLE_INTERFACE, "getCollidableComponents");
    private static Field boundField;

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
            boolean added = false;
            for (Object component : components) {
                if (boundField == null) {
                    boundField = component.getClass().getField("bound");
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
                out.add(OutlineShape.box(AxisAlignedBB.getBoundingBox(
                        x + minX, y + minY, z + minZ,
                        x + maxX, y + maxY, z + maxZ), false));
                added = true;
            }
            return added;
        } catch (Throwable ignored) {
            return false;
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
