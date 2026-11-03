package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.compat.GtTargetResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla block target with the actual GT pipe/cable/cover hit. Thin neighbor
 * pipes are otherwise easy to confuse with the full-block bounds used by the vanilla trace.
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "getMouseOver", at = @At("TAIL"))
    private void crosshairselection$resolveGtTarget(float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getMinecraft();
        MovingObjectPosition resolved = GtTargetResolver.resolve(
                minecraft.thePlayer, partialTicks, minecraft.objectMouseOver);
        if (resolved != null) {
            minecraft.objectMouseOver = resolved;
        }
    }
}
