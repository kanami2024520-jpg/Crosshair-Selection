package me.kuionc.crosshairselection.render;

import net.minecraft.util.AxisAlignedBB;

/**
 * Shared geometry helpers for selection outlines.
 */
public final class BoxUtil {

    private BoxUtil() {
    }

    /**
     * Expands a box by the given margin, but never past the block cell(s) that contain it.
     * A full 1x1x1 block therefore keeps its original bounds, while a thin pipe or wire can
     * grow until it touches the block edge.
     */
    public static AxisAlignedBB expandWithinBlock(AxisAlignedBB box, double margin) {
        if (box == null || margin <= 0.0D) {
            return box;
        }
        double blockMinX = Math.floor(box.minX);
        double blockMinY = Math.floor(box.minY);
        double blockMinZ = Math.floor(box.minZ);
        double blockMaxX = Math.ceil(box.maxX);
        double blockMaxY = Math.ceil(box.maxY);
        double blockMaxZ = Math.ceil(box.maxZ);
        return AxisAlignedBB.getBoundingBox(
                Math.max(blockMinX, box.minX - margin),
                Math.max(blockMinY, box.minY - margin),
                Math.max(blockMinZ, box.minZ - margin),
                Math.min(blockMaxX, box.maxX + margin),
                Math.min(blockMaxY, box.maxY + margin),
                Math.min(blockMaxZ, box.maxZ + margin));
    }
}
