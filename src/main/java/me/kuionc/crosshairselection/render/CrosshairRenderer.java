package me.kuionc.crosshairselection.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.compat.TicCrosshairCompat;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Draws the custom crosshair and hides the vanilla one through a Mixin.
 */
public class CrosshairRenderer {

    private static final ResourceLocation VANILLA_ICONS = new ResourceLocation("textures/gui/icons.png");
    private final Minecraft minecraft = Minecraft.getMinecraft();
    private int guiScale = 1;
    private float bowSpread;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (this.minecraft.theWorld == null || !this.minecraft.inGameHasFocus) {
            return;
        }
        if (!shouldRender()) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(this.minecraft,
                this.minecraft.displayWidth, this.minecraft.displayHeight);
        if (this.minecraft.gameSettings.hideGUI) {
            setupHiddenGuiOverlay(resolution);
        }
        draw(resolution.getScaledWidth() / 2, resolution.getScaledHeight() / 2);
    }

    public static boolean shouldRender() {
        if (!CrosshairConfig.enabled || !CrosshairConfig.visibleDefault) {
            return false;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.thirdPersonView > 0 && !CrosshairConfig.visibleThirdPerson) {
            return false;
        }
        if (minecraft.gameSettings.hideGUI && !CrosshairConfig.visibleHiddenGui) {
            return false;
        }
        if (TicCrosshairCompat.shouldUseTicCrosshair(minecraft.thePlayer)) {
            return false;
        }
        return !minecraft.gameSettings.showDebugInfo || CrosshairConfig.visibleDebug;
    }

    /**
     * Draws the crosshair at the given GUI coordinates. Used by the HUD hook and the settings
     * preview.
     */
    public void draw(int drawX, int drawY) {
        ScaledResolution resolution = new ScaledResolution(this.minecraft,
                this.minecraft.displayWidth, this.minecraft.displayHeight);
        this.guiScale = Math.max(1, resolution.getScaleFactor());
        int colour = currentColour();
        boolean flow = CrosshairConfig.rainbow && CrosshairConfig.rainbowFlow;
        float flowSpeed = CrosshairConfig.rainbowSpeed;
        boolean drawingBow = TicCrosshairCompat.isDrawingBow(this.minecraft.thePlayer);
        this.bowSpread = drawingBow
                ? (1.0F - TicCrosshairCompat.progress(this.minecraft.thePlayer)) * 25.0F
                : 0.0F;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        if (SelectionBoxConfig.antiAlias) {
            GL11.glEnable(GL13.GL_MULTISAMPLE);
        } else {
            GL11.glDisable(GL13.GL_MULTISAMPLE);
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(drawX, drawY, 0.0F);
        if (CrosshairConfig.rotation != 0) {
            GL11.glRotatef(CrosshairConfig.rotation, 0.0F, 0.0F, 1.0F);
        }
        GL11.glTranslatef(-drawX, -drawY, 0.0F);

        switch (CrosshairConfig.type) {
            case DEFAULT:
                drawVanilla(drawX, drawY);
                break;
            case CROSS:
            default:
                drawCross(drawX, drawY, colour, flow, flowSpeed);
                break;
        }

        if (CrosshairConfig.dot) {
            float dotHalf = Math.max(thickness(), 1.0F / this.guiScale) / 2.0F;
            RenderHelper.drawRect(drawX - dotHalf, drawY - dotHalf, drawX + dotHalf, drawY + dotHalf,
                    CrosshairConfig.dotColour, flow, flowSpeed);
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private int currentColour() {
        int base = CrosshairConfig.colour;
        if (CrosshairConfig.rainbow && !CrosshairConfig.rainbowFlow) {
            float hue = (System.currentTimeMillis() % 100000L) / 100000.0F
                    * CrosshairConfig.rainbowSpeed / 100.0F;
            int rgb = ColorUtil.hsbToRgb(hue, 0.8F, 1.0F);
            return ColorUtil.withAlpha(rgb, ColorUtil.alpha(base));
        }
        return base;
    }

    /**
     * The crosshair is configured in GUI pixels, but a quad thinner than one physical
     * pixel can fall between pixel centres and disappear entirely. Enforce a minimum of
     * one physical pixel here.
     */
    private float thickness() {
        return Math.max(CrosshairConfig.thickness / 4.0F, 1.0F / this.guiScale);
    }

    private float gap() {
        return CrosshairConfig.gap + this.bowSpread;
    }

    private float outlineThickness() {
        return Math.max(CrosshairConfig.outlineThickness, 1.0F / this.guiScale);
    }

    private void drawCross(int drawX, int drawY, int colour, boolean flow, float flowSpeed) {
        float line = thickness();
        float half = line / 2.0F;
        float gap = gap();
        float width = CrosshairConfig.width;
        float height = CrosshairConfig.height;
        if (CrosshairConfig.outline && CrosshairConfig.outlineThickness > 0.01F) {
            float outline = outlineThickness();
            int outlineColour = CrosshairConfig.outlineColour;
            drawOutlineAroundBar(drawX - half, drawY - gap - height, drawX + half, drawY - gap, outline, outlineColour, flow, flowSpeed);
            drawOutlineAroundBar(drawX - half, drawY + gap, drawX + half, drawY + gap + height, outline, outlineColour, flow, flowSpeed);
            drawOutlineAroundBar(drawX - gap - width, drawY - half, drawX - gap, drawY + half, outline, outlineColour, flow, flowSpeed);
            drawOutlineAroundBar(drawX + gap, drawY - half, drawX + gap + width, drawY + half, outline, outlineColour, flow, flowSpeed);
        }
        RenderHelper.drawRect(drawX - half, drawY - gap - height, drawX + half, drawY - gap, colour, flow, flowSpeed);
        RenderHelper.drawRect(drawX - half, drawY + gap, drawX + half, drawY + gap + height, colour, flow, flowSpeed);
        RenderHelper.drawRect(drawX - gap - width, drawY - half, drawX - gap, drawY + half, colour, flow, flowSpeed);
        RenderHelper.drawRect(drawX + gap, drawY - half, drawX + gap + width, drawY + half, colour, flow, flowSpeed);
    }

    private static void drawOutlineAroundBar(float x1, float y1, float x2, float y2, float margin,
                                              int colour, boolean flow, float flowSpeed) {
        RenderHelper.drawOutline(x1 - margin, y1 - margin, x2 + margin, y2 + margin, margin,
                colour, flow, flowSpeed);
    }

    private void drawVanilla(int drawX, int drawY) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
        this.minecraft.getTextureManager().bindTexture(VANILLA_ICONS);
        this.minecraft.ingameGUI.drawTexturedModalRect(drawX - 7, drawY - 7, 0, 0, 16, 16);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static void setupHiddenGuiOverlay(ScaledResolution resolution) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, resolution.getScaledWidth_double(),
                resolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
    }
}
