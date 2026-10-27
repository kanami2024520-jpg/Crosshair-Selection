package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * GregTech pipes and cables. GT renders them as square beams, so the outline uses the same
 * box bounds. Connected pipes are collected up to a configurable range and drawn as one union
 * to avoid overlapping internal boxes and seams at block boundaries.
 */
public final class GtPipeShapeProvider implements ShapeProvider {

    private static final int CONNECTED_DOWN = 1;
    private static final int CONNECTED_UP = 1 << 1;
    private static final int CONNECTED_NORTH = 1 << 2;
    private static final int CONNECTED_SOUTH = 1 << 3;
    private static final int CONNECTED_WEST = 1 << 4;
    private static final int CONNECTED_EAST = 1 << 5;

    private static final int[] POSITIVE = {CONNECTED_EAST, CONNECTED_UP, CONNECTED_SOUTH};
    private static final int[] NEGATIVE = {CONNECTED_WEST, CONNECTED_DOWN, CONNECTED_NORTH};

    private static final Class<?> PIPE_INTERFACE = classOrNull(
            "gregtech.api.interfaces.tileentity.IPipeRenderedTileEntity");
    private static final Method GET_THICKNESS = methodOrNull(PIPE_INTERFACE, "getThickNess");
    private static final Method GET_CONNECTIONS = methodOrNull(PIPE_INTERFACE, "getConnections");

    public static boolean isPipeTile(World world, int x, int y, int z) {
        if (PIPE_INTERFACE == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return PIPE_INTERFACE.isInstance(tile);
    }

    @Override
    public boolean collect(World world, int x, int y, int z, Block block, List<OutlineShape> out) {
        if (PIPE_INTERFACE == null || GET_THICKNESS == null || GET_CONNECTIONS == null) {
            return false;
        }
        List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
        if (collectTileBoxes(world, x, y, z, boxes) < 0 || boxes.isEmpty()) {
            return false;
        }
        out.add(OutlineShape.union(boxes));
        return true;
    }

    /**
     * @return connection bitmask, or -1 if the tile is not a usable pipe
     */
    private static int collectTileBoxes(World world, int x, int y, int z, List<AxisAlignedBB> boxes) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!PIPE_INTERFACE.isInstance(tile)) {
            return -1;
        }
        try {
            float thickness = ((Number) GET_THICKNESS.invoke(tile)).floatValue();
            int connections = ((Number) GET_CONNECTIONS.invoke(tile)).intValue() & 0xFF;
            if (thickness <= 0.001F || thickness >= 1.0F) {
                return -1;
            }
            double half = thickness / 2.0D;
            double center = 0.5D;
            // GT always renders a central cube; without it L/T/cross joints lose the
            // corner quadrant that is not covered by any arm.
            boxes.add(AxisAlignedBB.getBoundingBox(
                    x + center - half, y + center - half, z + center - half,
                    x + center + half, y + center + half, z + center + half));
            for (int axis = 0; axis < 3; axis++) {
                boolean pos = (connections & POSITIVE[axis]) != 0;
                boolean neg = (connections & NEGATIVE[axis]) != 0;
                if (!pos && !neg) {
                    continue;
                }
                double min = pos && neg ? 0.0D : pos ? 0.5D : 0.0D;
                double max = pos && neg ? 1.0D : pos ? 1.0D : 0.5D;
                double base = axis == 0 ? x : axis == 1 ? y : z;
                double low = base + min;
                double high = base + max;
                AxisAlignedBB box;
                if (axis == 0) {
                    box = AxisAlignedBB.getBoundingBox(low, y + center - half, z + center - half,
                            high, y + center + half, z + center + half);
                } else if (axis == 1) {
                    box = AxisAlignedBB.getBoundingBox(x + center - half, low, z + center - half,
                            x + center + half, high, z + center + half);
                } else {
                    box = AxisAlignedBB.getBoundingBox(x + center - half, y + center - half, low,
                            x + center + half, y + center + half, high);
                }
                boxes.add(box);
            }
            return connections;
        } catch (Throwable ignored) {
            return -1;
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
