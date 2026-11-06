package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.compat.GtTargetResolver;
import me.kuionc.crosshairselection.util.McRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.MovingObjectPosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 目标解析与客户端实例捕获。
 *
 * <p>{@code getMouseOver} 每帧执行且已被 refmap 收录，是这里最可靠的注入点。
 * 实例通过 {@link AccessorEntityRenderer} 读取，绝不调用 {@code Minecraft.getMinecraft()}。
 */
@Mixin(EntityRenderer.class)
public class MixinEntityRenderer {

    @Inject(method = "getMouseOver", at = @At("TAIL"))
    private void crosshairselection$resolveGtTarget(float partialTicks, CallbackInfo ci) {
        Minecraft minecraft = ((AccessorEntityRenderer) (Object) this).crosshairselection$mc();
        if (minecraft == null) {
            return;
        }
        // 每帧登记一次，保证所有渲染回调拿到的都是当前实例。
        McRef.setInstance(minecraft);
        MovingObjectPosition resolved = GtTargetResolver.resolve(
                minecraft.thePlayer, partialTicks, minecraft.objectMouseOver);
        if (resolved != null) {
            minecraft.objectMouseOver = resolved;
        }
    }
}
