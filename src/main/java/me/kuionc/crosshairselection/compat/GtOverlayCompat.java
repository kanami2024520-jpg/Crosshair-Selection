package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.render.GtOverlayRenderer;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;

/**
 * Replaces GT's wrench/cover grid for GregTech blocks. The actual entry point is a Mixin into
 * gregtech.client.BlockOverlayRenderer.drawGrid(); this class only decides whether to take over
 * and draws the replacement.
 */
public final class GtOverlayCompat {

    private static final Class<?> GT_TILE = classOrNull("gregtech.api.interfaces.tileentity.IGregTechTileEntity");

    private GtOverlayCompat() {
    }

    public static boolean tryRender(DrawBlockHighlightEvent event) {
        if (!GtOverlayConfig.enabled || event == null || event.player == null || event.target == null
                || event.target.typeOfHit != net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }
        // 九格只信 GT 自己的判定：GT 已经在 drawGrid 前筛过“可扳手方块 + 命中面”。
        // 这里不能再套 TargetLock，否则会把上一帧锁定的管道/面拿来画。
        net.minecraft.util.MovingObjectPosition target = event.target;
        if (target == null) {
            return false;
        }
        Block block = event.player.worldObj.getBlock(target.blockX, target.blockY, target.blockZ);
        if (!isGtTarget(event.player.worldObj, target.blockX, target.blockY,
                target.blockZ, block)) {
            return false;
        }
        if (!GtOverlayRenderer.render(event.player, target, event.partialTicks)) {
            return false;
        }
        event.setCanceled(true);
        return true;
    }

    public static boolean isGtTarget(net.minecraft.world.World world, int x, int y, int z, Block block) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && GT_TILE != null && GT_TILE.isInstance(tile)) {
            return true;
        }
        return block != null && block.getClass().getName().startsWith("gregtech.");
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
