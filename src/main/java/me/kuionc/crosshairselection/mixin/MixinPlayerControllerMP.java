package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.compat.GtHitMapper;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remaps the local hit vector for GT wrench/cover clicks so the server-side GT logic follows
 * our custom 3x3 grid.
 */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method = "onPlayerRightClick", at = @At("HEAD"), cancellable = true)
    private void crosshairselection$mapGtHit(EntityPlayer player, World world, ItemStack stack,
                                             int x, int y, int z, int side, Vec3 hitVec,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!GtHitMapper.map(player, world, stack, x, y, z, side, hitVec)) {
            cir.setReturnValue(true);
        }
    }
}
