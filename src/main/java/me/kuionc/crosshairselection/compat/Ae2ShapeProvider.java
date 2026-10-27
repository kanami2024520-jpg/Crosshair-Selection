package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Applied Energistics cable buses. The tile returns the individual cable/facade boxes; keep them
 * as separate outlines so a bus with several cables does not collapse into one outer box.
 */
public final class Ae2ShapeProvider implements ShapeProvider {

    private static final Class<?> CABLE_TILE = classOrNull("appeng.tile.networking.TileCableBus");
    private static final Method GET_BOXES = methodOrNull(CABLE_TILE, "getSelectedBoundingBoxesFromPool",
            World.class, int.class, int.class, int.class,
            net.minecraft.entity.Entity.class, boolean.class);

    @Override
    public boolean collect(World world, int x, int y, int z, Block block, List<OutlineShape> out) {
        if (CABLE_TILE == null || GET_BOXES == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!CABLE_TILE.isInstance(tile)) {
            return false;
        }
        try {
            Object value = GET_BOXES.invoke(tile, world, x, y, z, null, false);
            if (!(value instanceof Iterable)) {
                return false;
            }
            boolean added = false;
            for (Object object : (Iterable<?>) value) {
                if (!(object instanceof AxisAlignedBB)) {
                    continue;
                }
                AxisAlignedBB box = (AxisAlignedBB) object;
                if (box.maxX - box.minX <= 0.0001D || box.maxY - box.minY <= 0.0001D
                        || box.maxZ - box.minZ <= 0.0001D) {
                    continue;
                }
                out.add(OutlineShape.box(AxisAlignedBB.getBoundingBox(
                        box.minX + x, box.minY + y, box.minZ + z,
                        box.maxX + x, box.maxY + y, box.maxZ + z), false));
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

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
