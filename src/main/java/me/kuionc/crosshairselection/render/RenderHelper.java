package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL11;

/**
 * Thin drawing helpers shared by the crosshair and the selection box. All geometry goes through
 * GTNHLib's {@link TessellatorManager}, which keeps the draw compatible with Angelica's
 * Tessellator interception.
 */
public final class RenderHelper {

    private RenderHelper() {
    }

    public static void setColor(int argb) {
        GL11.glColor4f((argb >> 16 & 255) / 255.0F,
                (argb >> 8 & 255) / 255.0F,
                (argb & 255) / 255.0F,
                (argb >>> 24 & 255) / 255.0F);
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int argb) {
        setColor(argb);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        tessellator.addVertex(x1, y2, 0.0D);
        tessellator.addVertex(x2, y2, 0.0D);
        tessellator.addVertex(x2, y1, 0.0D);
        tessellator.addVertex(x1, y1, 0.0D);
        tessellator.draw();
    }

    public static void drawOutline(float x1, float y1, float x2, float y2, float thickness, int argb) {
        drawRect(x1, y1, x2, y1 + thickness, argb);
        drawRect(x1, y2 - thickness, x2, y2, argb);
        drawRect(x1, y1 + thickness, x1 + thickness, y2 - thickness, argb);
        drawRect(x2 - thickness, y1 + thickness, x2, y2 - thickness, argb);
    }

    public static void drawCircle(float centerX, float centerY, float radius, float thickness, int argb) {
        float inner = Math.max(0.0F, radius - thickness / 2.0F);
        float outer = radius + thickness / 2.0F;
        setColor(argb);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_TRIANGLE_STRIP);
        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            tessellator.addVertex(centerX + cos * outer, centerY + sin * outer, 0.0D);
            tessellator.addVertex(centerX + cos * inner, centerY + sin * inner, 0.0D);
        }
        tessellator.draw();
    }

    public static void drawLines(float[] points, float thickness, int argb) {
        setColor(argb);
        GL11.glLineWidth(thickness);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_LINES);
        for (int i = 0; i + 3 < points.length; i += 4) {
            tessellator.addVertex(points[i], points[i + 1], 0.0D);
            tessellator.addVertex(points[i + 2], points[i + 3], 0.0D);
        }
        tessellator.draw();
        GL11.glLineWidth(1.0F);
    }

    public static void drawCheckerboard(int x, int y, int width, int height, int tile) {
        for (int ix = 0; ix < width / tile; ix++) {
            for (int iy = 0; iy < height / tile; iy++) {
                int argb = ((ix + iy) & 1) == 0 ? 0xFF3A3A3A : 0xFF4A4A4A;
                drawRect(x + ix * tile, y + iy * tile, x + (ix + 1) * tile, y + (iy + 1) * tile, argb);
            }
        }
    }
}
