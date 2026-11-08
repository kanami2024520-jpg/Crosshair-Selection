package me.kuionc.crosshairselection.gui.widgets;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

/**
 * Hue/saturation/brightness 色盘。
 *
 * <p>用一张 512×512 的纹理渲染，显示时缩放到需要的尺寸：格子按像素逐块 drawRect 的做法
 * 在 512 分辨率下会变成 26 万个矩形/帧，不可行。
 *
 * <p>纹理只在色相变化时重建，并且做节流（拖动色相把手时不会每帧重算 26 万像素）。
 */
public final class HsvSquare {

    private static final int RESOLUTION = 512;
    /** 色相变化小于这个值就不重建纹理。 */
    private static final float HUE_EPSILON = 1.0F / 1024.0F;
    /** 两次重建之间的最短间隔（毫秒）。 */
    private static final long REBUILD_INTERVAL = 60L;

    private static DynamicTexture texture;
    private static float textureHue = -1.0F;
    private static long lastRebuild;

    private HsvSquare() {
    }

    /** 画出色盘；按给定的宽高拉伸纹理（支持长方形）。 */
    public static void draw(int x, int y, int width, int height, float hue) {
        ensureTexture(hue);
        if (texture == null) {
            return;
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture.getGlTextureId());
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(GL11.GL_BLEND);

        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + height, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(x + width, y + height, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x + width, y, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x, y, 0.0D, 0.0D, 0.0D);
        tessellator.draw();

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /** 1px 十字 + 1px 黑描边。 */
    public static void drawMarker(int x, int y, int width, int height,
                                  float saturation, float brightness) {
        int markerX = x + Math.round(clamp01(saturation) * (width - 1));
        int markerY = y + Math.round((1.0F - clamp01(brightness)) * (height - 1));
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        drawRect(markerX - 4, markerY, markerX + 5, markerY + 1, 0xFF000000);
        drawRect(markerX, markerY - 4, markerX + 1, markerY + 5, 0xFF000000);
        drawRect(markerX - 3, markerY, markerX + 4, markerY + 1, 0xFFFFFFFF);
        drawRect(markerX, markerY - 3, markerX + 1, markerY + 4, 0xFFFFFFFF);
    }

    /** 鼠标位置 → {saturation, brightness}。 */
    public static float[] pick(int x, int y, int width, int height, int mouseX, int mouseY) {
        if (width <= 0 || height <= 0) {
            return new float[]{0.0F, 0.0F};
        }
        return new float[]{
                clamp01((mouseX - x) / (float) width),
                1.0F - clamp01((mouseY - y) / (float) height)};
    }

    private static void ensureTexture(float hue) {
        long now = System.currentTimeMillis();
        boolean hueMoved = Math.abs(hue - textureHue) > HUE_EPSILON;
        if (texture != null && (!hueMoved || now - lastRebuild < REBUILD_INTERVAL)) {
            return;
        }
        if (texture == null) {
            texture = new DynamicTexture(RESOLUTION, RESOLUTION);
        }
        int[] pixels = new int[RESOLUTION * RESOLUTION];
        for (int py = 0; py < RESOLUTION; py++) {
            float brightness = 1.0F - py / (float) (RESOLUTION - 1);
            int row = py * RESOLUTION;
            for (int px = 0; px < RESOLUTION; px++) {
                float saturation = px / (float) (RESOLUTION - 1);
                pixels[row + px] = 0xFF000000 | ColorUtil.hsbToRgb(hue, saturation, brightness);
            }
        }
        int[] target = texture.getTextureData();
        System.arraycopy(pixels, 0, target, 0, pixels.length);
        texture.updateDynamicTexture();
        textureHue = hue;
        lastRebuild = now;
    }

    private static void drawRect(int x0, int y0, int x1, int y1, int colour) {
        float a = (colour >>> 24 & 255) / 255.0F;
        float r = (colour >>> 16 & 255) / 255.0F;
        float g = (colour >>> 8 & 255) / 255.0F;
        float b = (colour & 255) / 255.0F;
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        tessellator.addVertex(x0, y1, 0.0D);
        tessellator.addVertex(x1, y1, 0.0D);
        tessellator.addVertex(x1, y0, 0.0D);
        tessellator.addVertex(x0, y0, 0.0D);
        tessellator.draw();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
