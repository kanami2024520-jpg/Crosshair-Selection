package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.render.SelectionBoxRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Inject(method = "drawSelectionBox", at = @At("HEAD"), cancellable = true)
    private void crosshairselection$drawSelectionBox(EntityPlayer player, MovingObjectPosition hitResult,
                                                     int subID, float partialTicks, CallbackInfo ci) {
        if (subID == 0 && SelectionBoxRenderer.render(player, hitResult, partialTicks)) {
            ci.cancel();
        }
    }
}
