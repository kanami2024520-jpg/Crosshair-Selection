package me.kuionc.crosshairselection.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Screen-space mining progress: a ring around the crosshair and/or a percentage read-out.
 *
 * <p>These are HUD elements, so they stay crisp at any distance and are readable while the block
 * face is turned away. The world-space bar stays in {@link BreakProgressRenderer}.
 */
public final class BreakProgressHud {

    private static final float RING_RADIUS = 13.0F;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = McRef.get();
        if (minecraft == null || minecraft.theWorld == null || !minecraft.inGameHasFocus
                || !SelectionBoxConfig.showBreakProgress) {
            return;
        }
        if (minecraft.currentScreen != null || minecraft.gameSettings.hideGUI) {
            return;
        }
        float progress = BreakProgressRenderer.breakProgress();
        if (progress <= 0.0F) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(minecraft,
                minecraft.displayWidth, minecraft.displayHeight);
        float centreX = resolution.getScaledWidth() / 2.0F;
        float centreY = resolution.getScaledHeight() / 2.0F;
        int colour = SelectionBoxConfig.colour;

        RenderBackend.beginGuiOverlay();
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
        // The HUD hook runs outside the normal GUI pass, so scissoring is explicitly disabled.
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (SelectionBoxConfig.breakProgressStyle.showsNumber()) {
            drawNumber(minecraft, centreX, centreY, progress, colour);
        }

        GL11.glPopAttrib();
        RenderBackend.endGuiOverlay();
    }

    /** Track plus filled arc, both drawn as short chords. */
    private static void drawNumber(Minecraft minecraft, float centreX, float centreY,
                                   float progress, int colour) {
        FontRenderer font = minecraft.fontRenderer;
        if (font == null) {
            return;
        }
        String text = Math.round(progress * 100.0F) + "%";
        // 位置可配置，与拉弓提示各自独立。
        float[] at = CrosshairHud.anchor(centreX, centreY, SelectionBoxConfig.breakProgressPosition);
        int x = Math.round(at[0] - font.getStringWidth(text) / 2.0F);
        int y = Math.round(at[1] - font.FONT_HEIGHT / 2.0F);
        font.drawStringWithShadow(text, x, y,
                ColorUtil.withAlpha(ColorUtil.brighten(colour, 1.25F), 255));
    }
}
