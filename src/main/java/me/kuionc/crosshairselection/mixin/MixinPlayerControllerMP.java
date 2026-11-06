package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.compat.GtHitMapper;
import me.kuionc.crosshairselection.render.CrosshairFeedback;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Remaps the local hit vector for GT wrench/cover clicks so the server-side GT logic follows
 * our custom 3x3 grid.
 */
@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    /** {@code curBlockDamageMP}: 0..1 progress of the block currently being mined. */
    @Shadow
    private float field_78770_f;

    public float crosshairselection$getBlockBreakProgress() {
        return field_78770_f;
    }

    /** Every left click reaches this method, including repeated swings. */
    @Inject(method = "clickBlock", at = @At("HEAD"))
    private void crosshairselection$attackFeedback(int x, int y, int z, int side, CallbackInfo ci) {
        CrosshairFeedback.notifyAttack();
    }

    @Inject(method = "onPlayerRightClick", at = @At("HEAD"), cancellable = true)
    private void crosshairselection$mapGtHit(EntityPlayer player, World world, ItemStack stack,
                                             int x, int y, int z, int side, Vec3 hitVec,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!GtHitMapper.map(player, world, stack, x, y, z, side, hitVec)) {
            cir.setReturnValue(true);
        }
    }
}
