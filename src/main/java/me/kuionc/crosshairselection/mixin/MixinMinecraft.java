package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.compat.GtTargetResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Refresh the GT target immediately before the right-click handler runs, so the block that is
 * clicked is the same one whose selection box was drawn.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Inject(method = "func_147121_ag", at = @At("HEAD"))
    private void crosshairselection$refreshGtTarget(CallbackInfo ci) {
        Minecraft minecraft = McRef.get();
        MovingObjectPosition resolved = GtTargetResolver.resolve(
                minecraft.thePlayer, 1.0F, minecraft.objectMouseOver);
        if (resolved != null) {
            minecraft.objectMouseOver = resolved;
        }
    }
}
