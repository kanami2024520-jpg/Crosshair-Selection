package me.kuionc.crosshairselection.mixin;

import gregtech.client.BlockOverlayRenderer;
import me.kuionc.crosshairselection.compat.GtOverlayCompat;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Takes over GT's wrench/cover grid at its source. Injecting here avoids depending on Forge
 * event priority, which is unreliable against GT's own high-priority handler.
 */
@Mixin(value = BlockOverlayRenderer.class)
public class MixinGtBlockOverlayRenderer {

    @Inject(method = "drawGrid", at = @At("HEAD"), cancellable = true, remap = false)
    private static void crosshairselection$replaceGtGrid(DrawBlockHighlightEvent event,
                                                         boolean first, boolean second, boolean third,
                                                         CallbackInfo ci) {
        if (GtOverlayCompat.tryRender(event)) {
            ci.cancel();
        }
    }
}
