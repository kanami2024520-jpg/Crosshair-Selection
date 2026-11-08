package me.kuionc.crosshairselection.gui.widgets;

import me.kuionc.crosshairselection.render.RenderHelper;
import org.lwjgl.opengl.GL11;

/**
 * 横向取值条：逐像素渐变 + 可拖动标记。
 *
 * <p>和颜色选择器里的透明度条同一套画法，用来替代狭窄的滑杆按钮。
 * 调用方负责给出每个像素的颜色（亮度渐变、棋盘格透明度、密度条纹等）。
 */
public final class ValueBar {

    private static final int MARKER_OVERHANG = 3;

    private ValueBar() {
    }

    /**
     * 画出渐变条。
     *
     * @param colours 至少 {@code width} 个 ARGB 值，从左到右
     */
    public static void draw(int x, int y, int width, int height, int[] colours) {
        if (colours == null) {
            return;
        }
        int count = Math.min(width, colours.length);
        for (int i = 0; i < count; i++) {
            RenderHelper.drawRect(x + i, y, x + i + 1, y + height, colours[i]);
        }
        RenderHelper.drawOutline(x, y, x + width, y + height, 1.0F, 0x60FFFFFF);
    }

    /** 1px 白线 + 1px 黑描边，位于 0..1 的给定值处。 */
    public static void drawMarker(int x, int y, int width, int height, float value) {
        int markerX = x + Math.round(Math.max(0.0F, Math.min(1.0F, value)) * (width - 1));
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        RenderHelper.drawRect(markerX - 2, y - MARKER_OVERHANG,
                markerX + 3, y + height + MARKER_OVERHANG, 0xFF000000);
        RenderHelper.drawRect(markerX - 1, y - MARKER_OVERHANG + 1,
                markerX + 2, y + height + MARKER_OVERHANG - 1, 0xFFFFFFFF);
    }

    /** 鼠标位置 → 0..1。 */
    public static float pick(int x, int width, int mouseX) {
        if (width <= 1) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, (mouseX - x) / (float) (width - 1)));
    }

    /**
     * 透明度条的底色：棋盘格，便于看出透明端。
     */
    public static void drawChecker(int x, int y, int width, int height) {
        RenderHelper.drawCheckerboard(x, y, width, height, 8);
    }
}
