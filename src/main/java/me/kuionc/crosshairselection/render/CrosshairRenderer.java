package me.kuionc.crosshairselection.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Draws the custom crosshair and hides the vanilla one through a Mixin.
 */
public class CrosshairRenderer {

    private static final ResourceLocation VANILLA_ICONS = new ResourceLocation("textures/gui/icons.png");
    private final Minecraft minecraft = Minecraft.getMinecraft();
    private int guiScale = 1;

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
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glPushMatrix();
        GL11.glTranslatef(drawX, drawY, 0.0F);
        if (CrosshairConfig.rotation != 0) {
            GL11.glRotatef(CrosshairConfig.rotation, 0.0F, 0.0F, 1.0F);
        }
        GL11.glTranslatef(-drawX, -drawY, 0.0F);

        switch (CrosshairConfig.type) {
            case CIRCLE:
                drawCircle(drawX, drawY, colour);
                break;
            case SQUARE:
                drawSquare(drawX, drawY, colour);
                break;
            case DEFAULT:
                drawVanilla(drawX, drawY);
                break;
            case ARROW:
                drawArrow(drawX, drawY, colour);
                break;
            case TRIANGLE:
                drawTriangle(drawX, drawY, colour);
                break;
            case CROSS:
            default:
                drawCross(drawX, drawY, colour);
                break;
        }

        if (CrosshairConfig.dot) {
            float dotHalf = Math.max(thickness(), 1.0F / this.guiScale) / 2.0F;
            RenderHelper.drawRect(drawX - dotHalf, drawY - dotHalf, drawX + dotHalf, drawY + dotHalf,
                    CrosshairConfig.dotColour);
        }

        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private int currentColour() {
        int base = CrosshairConfig.colour;
        if (CrosshairConfig.rainbow) {
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

    private float outlineThickness() {
        return Math.max(CrosshairConfig.outlineThickness, 1.0F / this.guiScale);
    }

    private void drawCross(int drawX, int drawY, int colour) {
        float line = thickness();
        float half = line / 2.0F;
        float gap = CrosshairConfig.gap;
        float width = CrosshairConfig.width;
        float height = CrosshairConfig.height;
        if (CrosshairConfig.outline && CrosshairConfig.outlineThickness > 0.01F) {
            float outline = outlineThickness();
            int outlineColour = CrosshairConfig.outlineColour;
            drawOutlineAroundBar(drawX - half, drawY - gap - height, drawX + half, drawY - gap, outline, outlineColour);
            drawOutlineAroundBar(drawX - half, drawY + gap, drawX + half, drawY + gap + height, outline, outlineColour);
            drawOutlineAroundBar(drawX - gap - width, drawY - half, drawX - gap, drawY + half, outline, outlineColour);
            drawOutlineAroundBar(drawX + gap, drawY - half, drawX + gap + width, drawY + half, outline, outlineColour);
        }
        RenderHelper.drawRect(drawX - half, drawY - gap - height, drawX + half, drawY - gap, colour);
        RenderHelper.drawRect(drawX - half, drawY + gap, drawX + half, drawY + gap + height, colour);
        RenderHelper.drawRect(drawX - gap - width, drawY - half, drawX - gap, drawY + half, colour);
        RenderHelper.drawRect(drawX + gap, drawY - half, drawX + gap + width, drawY + half, colour);
    }

    private static void drawOutlineAroundBar(float x1, float y1, float x2, float y2, float margin, int colour) {
        RenderHelper.drawOutline(x1 - margin, y1 - margin, x2 + margin, y2 + margin, margin, colour);
    }

    private void drawCircle(int drawX, int drawY, int colour) {
        float line = thickness();
        float radius = CrosshairConfig.gap + line / 2.0F;
        if (CrosshairConfig.outline && CrosshairConfig.outlineThickness > 0.01F) {
            float outline = outlineThickness();
            RenderHelper.drawCircle(drawX, drawY, radius + line / 2.0F + outline / 2.0F,
                    outline, CrosshairConfig.outlineColour);
            RenderHelper.drawCircle(drawX, drawY, radius - line / 2.0F - outline / 2.0F,
                    outline, CrosshairConfig.outlineColour);
        }
        RenderHelper.drawCircle(drawX, drawY, radius, line, colour);
    }

    private void drawSquare(int drawX, int drawY, int colour) {
        float gap = CrosshairConfig.gap;
        float line = thickness();
        if (CrosshairConfig.outline && CrosshairConfig.outlineThickness > 0.01F) {
            float outline = outlineThickness();
            RenderHelper.drawOutline(drawX - gap, drawY - gap, drawX + gap, drawY + gap,
                    outline, CrosshairConfig.outlineColour);
            RenderHelper.drawOutline(drawX - gap - line - outline, drawY - gap - line - outline,
                    drawX + gap + line + outline, drawY + gap + line + outline,
                    outline, CrosshairConfig.outlineColour);
        }
        RenderHelper.drawRect(drawX - gap - line, drawY - gap - line,
                drawX + gap + line, drawY - gap, colour);
        RenderHelper.drawRect(drawX - gap - line, drawY + gap,
                drawX + gap + line, drawY + gap + line, colour);
        RenderHelper.drawRect(drawX - gap - line, drawY - gap,
                drawX - gap, drawY + gap, colour);
        RenderHelper.drawRect(drawX + gap, drawY - gap,
                drawX + gap + line, drawY + gap, colour);
    }

    private void drawArrow(int drawX, int drawY, int colour) {
        float width = CrosshairConfig.width;
        float height = CrosshairConfig.height;
        RenderHelper.drawLines(new float[]{
                drawX - width, drawY + height, drawX, drawY,
                drawX, drawY, drawX + width, drawY + height
        }, Math.max(1.0F, thickness() * this.guiScale), colour);
    }

    private void drawTriangle(int drawX, int drawY, int colour) {
        float width = CrosshairConfig.width / 2.0F;
        float height = CrosshairConfig.height / 2.0F;
        float gap = CrosshairConfig.gap;
        float tipY = drawY - gap - height;
        float bottomY = drawY + gap + height;
        if (CrosshairConfig.outline && CrosshairConfig.outlineThickness > 0.01F) {
            float outline = outlineThickness();
            RenderHelper.drawLines(new float[]{
                    drawX, tipY - outline, drawX - gap - width - outline, bottomY + outline / 2.0F,
                    drawX - gap - width - outline, bottomY + outline / 2.0F,
                    drawX + gap + width + outline, bottomY + outline / 2.0F,
                    drawX + gap + width + outline, bottomY + outline / 2.0F, drawX, tipY - outline
            }, Math.max(1.0F, outline * this.guiScale), CrosshairConfig.outlineColour);
        }
        RenderHelper.drawLines(new float[]{
                drawX, tipY, drawX - gap - width, bottomY,
                drawX - gap - width, bottomY, drawX + gap + width, bottomY,
                drawX + gap + width, bottomY, drawX, tipY
        }, Math.max(1.0F, thickness() * this.guiScale), colour);
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
