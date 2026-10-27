package me.kuionc.crosshairselection.render;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import java.util.List;

/**
 * Provides accurate selection shapes for specific blocks. Implementations return {@code true}
 * when they recognised the block; if they add no shapes the collector falls back to the AABB.
 */
public interface ShapeProvider {

    boolean collect(World world, int x, int y, int z, Block block, List<OutlineShape> out);
}
