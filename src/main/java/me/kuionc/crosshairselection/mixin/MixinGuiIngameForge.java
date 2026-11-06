package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.compat.TicCrosshairCompat;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Suppresses the vanilla crosshair while the custom crosshair is enabled. The custom crosshair
 * itself is drawn from RenderTickEvent so it can still be shown with F1 if configured.
 */
@Mixin(value = GuiIngameForge.class, remap = false)
public class MixinGuiIngameForge {

    @Inject(method = "renderCrosshairs", at = @At("HEAD"), cancellable = true, remap = false)
    private void crosshairselection$hideVanillaCrosshair(int width, int height, CallbackInfo ci) {
        if (CrosshairConfig.enabled
                && !TicCrosshairCompat.shouldUseTicCrosshair(
                        McRef.get() == null ? null : McRef.get().thePlayer)) {
            ci.cancel();
        }
    }
}
