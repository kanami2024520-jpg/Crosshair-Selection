package me.kuionc.crosshairselection.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Extra boxes for the "link blocks" option: doors, beds, double plants and pistons.
 */
final class LinkedBoxes {

    private static final int[][] HORIZONTALS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

    private LinkedBoxes() {
    }

    static List<AxisAlignedBB> collect(World world, Block block, int x, int y, int z, AxisAlignedBB shape) {
        try {
            if (block instanceof BlockDoor) {
                return mergeVerticalHalf(world, block, x, y, z, shape, true);
            }
            if (block instanceof BlockDoublePlant) {
                return mergeVerticalHalf(world, block, x, y, z, shape, false);
            }
            if (block instanceof BlockBed) {
                return mergeBed(world, block, x, y, z, shape);
            }
            if (block instanceof BlockPistonBase) {
                return pistonPair(world, x, y, z, true);
            }
            if (block instanceof BlockPistonExtension) {
                return pistonPair(world, x, y, z, false);
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    private static List<AxisAlignedBB> mergeVerticalHalf(World world, Block block, int x, int y, int z,
                                                         AxisAlignedBB shape, boolean door) {
        int meta = world.getBlockMetadata(x, y, z);
        boolean upper = (meta & 8) != 0;
        int otherY = upper ? y - 1 : y + 1;
        if (world.getBlock(x, otherY, z) != block) {
            return Collections.emptyList();
        }
        int otherMeta = world.getBlockMetadata(x, otherY, z);
        if (((otherMeta & 8) != 0) == upper) {
            return Collections.emptyList();
        }
        if (door && (meta & 7) != (otherMeta & 7)) {
            return Collections.emptyList();
        }
        return merged(world, block, x, otherY, z, shape);
    }

    private static List<AxisAlignedBB> mergeBed(World world, Block block, int x, int y, int z, AxisAlignedBB shape) {
        boolean head = (world.getBlockMetadata(x, y, z) & 8) != 0;
        for (int[] offset : HORIZONTALS) {
            int otherX = x + offset[0];
            int otherZ = z + offset[1];
            if (world.getBlock(otherX, y, otherZ) != block) {
                continue;
            }
            if (((world.getBlockMetadata(otherX, y, otherZ) & 8) != 0) == head) {
                continue;
            }
            return merged(world, block, otherX, y, otherZ, shape);
        }
        return Collections.emptyList();
    }

    private static List<AxisAlignedBB> merged(World world, Block block, int x, int y, int z, AxisAlignedBB shape) {
        AxisAlignedBB other = SelectionBoxRenderer.getSelectedBox(world, block, x, y, z);
        if (other == null) {
            return Collections.emptyList();
        }
        List<AxisAlignedBB> result = new ArrayList<AxisAlignedBB>(1);
        result.add(AxisAlignedBB.getBoundingBox(
                Math.min(shape.minX, other.minX), Math.min(shape.minY, other.minY), Math.min(shape.minZ, other.minZ),
                Math.max(shape.maxX, other.maxX), Math.max(shape.maxY, other.maxY), Math.max(shape.maxZ, other.maxZ)));
        return result;
    }

    private static List<AxisAlignedBB> pistonPair(World world, int x, int y, int z, boolean base) {
        int meta = world.getBlockMetadata(x, y, z);
        if (base && (meta & 8) == 0) {
            return Collections.emptyList();
        }
        int[] offset = orientationOffset(meta & 7);
        int otherX = base ? x + offset[0] : x - offset[0];
        int otherY = base ? y + offset[1] : y - offset[1];
        int otherZ = base ? z + offset[2] : z - offset[2];
        Block other = world.getBlock(otherX, otherY, otherZ);
        if (base) {
            if (!(other instanceof BlockPistonExtension)
                    || (world.getBlockMetadata(otherX, otherY, otherZ) & 7) != (meta & 7)) {
                return Collections.emptyList();
            }
        } else {
            if (!(other instanceof BlockPistonBase)
                    || (world.getBlockMetadata(otherX, otherY, otherZ) & 7) != (meta & 7)
                    || (world.getBlockMetadata(otherX, otherY, otherZ) & 8) == 0) {
                return Collections.emptyList();
            }
        }
        AxisAlignedBB otherBox = SelectionBoxRenderer.getSelectedBox(world, other, otherX, otherY, otherZ);
        if (otherBox == null) {
            return Collections.emptyList();
        }
        List<AxisAlignedBB> result = new ArrayList<AxisAlignedBB>(1);
        result.add(otherBox);
        return result;
    }

    private static int[] orientationOffset(int orientation) {
        switch (orientation) {
            case 0:
                return new int[]{0, -1, 0};
            case 1:
                return new int[]{0, 1, 0};
            case 2:
                return new int[]{0, 0, -1};
            case 3:
                return new int[]{0, 0, 1};
            case 4:
                return new int[]{-1, 0, 0};
            case 5:
                return new int[]{1, 0, 0};
            default:
                return new int[]{0, 0, 0};
        }
    }
}
