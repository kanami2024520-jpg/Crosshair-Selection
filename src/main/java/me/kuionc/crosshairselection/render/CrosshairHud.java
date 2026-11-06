package me.kuionc.crosshairselection.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.compat.TicCrosshairCompat;
import me.kuionc.crosshairselection.config.BowChargeIndicator;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.HudPosition;
import me.kuionc.crosshairselection.util.ColorUtil;
import me.kuionc.crosshairselection.util.McRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

/**
 * 拉弓提示：百分比数字或向上箭头。
 *
 * <p>位置可配置（八个方位），与破坏进度的百分比互不影响。
 * 两者都相对屏幕中心定位，因此在任何分辨率与 GUI 缩放下偏移一致。
 */
public final class CrosshairHud {

    /** 基础偏移：刚好跨过准星臂。 */
    private static final float BASE_OFFSET = 16.0F;
    /** 斜角位置额外外移的距离。 */
    private static final float DIAGONAL_OFFSET = 10.0F;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = McRef.get();
        if (minecraft == null || minecraft.theWorld == null || !minecraft.inGameHasFocus
                || minecraft.currentScreen != null || minecraft.gameSettings.hideGUI) {
            return;
        }
        // 蓄力提示属于准星的一部分：准星被全局关闭（或按 F1 / 第三人称 /
        // 匠魂自带准星等条件隐藏）时，它也必须一起消失。之前只判断了提示样式，
        // 所以关掉准星后「↑」仍然挂在屏幕中间。
        if (!CrosshairRenderer.shouldRender()) {
            return;
        }
        BowChargeIndicator indicator = CrosshairConfig.bowChargeIndicator;
        if (indicator == BowChargeIndicator.NONE) {
            return;
        }
        float charge = TicCrosshairCompat.progress(minecraft.thePlayer);
        if (charge <= 0.001F) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(minecraft,
                minecraft.displayWidth, minecraft.displayHeight);
        float centreX = resolution.getScaledWidth() / 2.0F;
        float centreY = resolution.getScaledHeight() / 2.0F;

        RenderBackend.beginGuiOverlay();
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (indicator == BowChargeIndicator.ARROW) {
            drawArrow(minecraft, centreX, centreY, charge);
        } else {
            drawNumber(minecraft, centreX, centreY, charge);
        }
        GL11.glPopAttrib();
        RenderBackend.endGuiOverlay();
    }

    /**
     * 相对屏幕中心的锚点。
     *
     * <p>斜角位置再把两个轴各推远一档，避免和准星臂重叠。
     */
    static float[] anchor(float centreX, float centreY, HudPosition position) {
        int h = position.horizontal();
        int v = position.vertical();
        float dx = h * (BASE_OFFSET + DIAGONAL_OFFSET * Math.abs(v));
        float dy = v * (BASE_OFFSET + DIAGONAL_OFFSET * Math.abs(h));
        return new float[]{centreX + dx, centreY + dy};
    }

    private static void drawNumber(Minecraft minecraft, float centreX, float centreY, float charge) {
        FontRenderer font = minecraft.fontRenderer;
        if (font == null) {
            return;
        }
        String text = Math.round(charge * 100.0F) + "%";
        float[] at = anchor(centreX, centreY, CrosshairConfig.bowIndicatorPosition);
        int x = Math.round(at[0] - font.getStringWidth(text) / 2.0F);
        int y = Math.round(at[1] - font.FONT_HEIGHT / 2.0F);
        // 满蓄力转为琥珀色，作为「可以射了」的提示。
        int colour = charge >= 0.95F
                ? ColorUtil.hsbToRgb(0.10F, 0.6F, 1.0F)
                : ColorUtil.hsbToRgb(0.10F, 0.35F, 0.75F + charge * 0.25F);
        font.drawStringWithShadow(text, x, y, colour);
    }

    /**
     * 拉满提示：绘制字符 ↑。
     *
     * <p>只在蓄力达到就绪阈值后出现，作为「可以射了」的明确信号；
     * 未拉满时不占位、不绘制，避免和进度数字重复表达。
     */
    private static void drawArrow(Minecraft minecraft, float centreX, float centreY, float charge) {
        if (charge < READY_THRESHOLD) {
            return;
        }
        FontRenderer font = minecraft.fontRenderer;
        if (font == null) {
            return;
        }
        String text = "\u2191";
        float[] at = anchor(centreX, centreY, CrosshairConfig.bowIndicatorPosition);
        int x = Math.round(at[0] - font.getStringWidth(text) / 2.0F);
        int y = Math.round(at[1] - font.FONT_HEIGHT / 2.0F);
        font.drawStringWithShadow(text, x, y, ColorUtil.hsbToRgb(0.10F, 0.75F, 1.0F));
    }

    /** 就绪阈值：与着色器里的满蓄力判定保持一致。 */
    private static final float READY_THRESHOLD = 0.95F;
}
