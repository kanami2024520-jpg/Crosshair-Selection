package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Thin drawing helpers shared by the crosshair and the selection box. All geometry goes through
 * GTNHLib's {@link TessellatorManager}, which keeps the draw compatible with Angelica's
 * Tessellator interception.
 */
public final class RenderHelper {

    private static final ResourceLocation WHITE_TEXTURE =
            new ResourceLocation("crosshairselection:textures/white.png");

    private RenderHelper() {
    }

    /**
     * Shader packs always sample the bound texture, even when the fixed-function texture stage is
     * disabled. World-space lines therefore need an opaque texture with valid UVs.
     */
    public static void bindWhiteTexture() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(WHITE_TEXTURE);
    }

    public static void setColor(int argb) {
        GL11.glColor4f((argb >> 16 & 255) / 255.0F,
                (argb >> 8 & 255) / 255.0F,
                (argb & 255) / 255.0F,
                (argb >>> 24 & 255) / 255.0F);
    }

    public static void setVertexColor(Tessellator tessellator, int argb) {
        tessellator.setColorRGBA_F((argb >> 16 & 255) / 255.0F,
                (argb >> 8 & 255) / 255.0F,
                (argb & 255) / 255.0F,
                (argb >>> 24 & 255) / 255.0F);
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int argb) {
        drawRect(x1, y1, x2, y2, argb, false, 0.0F);
    }

    public static void drawRect(float x1, float y1, float x2, float y2, int argb,
                                boolean flow, float speed) {
        if (flow) {
            FlowRenderer.drawRect2D(x1, y1, x2, y2, argb, System.currentTimeMillis(), speed, 8);
            return;
        }
        setColor(argb);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        addVertex2D(tessellator, x1, y2, argb, flow, speed);
        addVertex2D(tessellator, x2, y2, argb, flow, speed);
        addVertex2D(tessellator, x2, y1, argb, flow, speed);
        addVertex2D(tessellator, x1, y1, argb, flow, speed);
        tessellator.draw();
    }

    public static void drawOutline(float x1, float y1, float x2, float y2, float thickness, int argb) {
        drawOutline(x1, y1, x2, y2, thickness, argb, false, 0.0F);
    }

    public static void drawOutline(float x1, float y1, float x2, float y2, float thickness, int argb,
                                   boolean flow, float speed) {
        drawRect(x1, y1, x2, y1 + thickness, argb, flow, speed);
        drawRect(x1, y2 - thickness, x2, y2, argb, flow, speed);
        drawRect(x1, y1 + thickness, x1 + thickness, y2 - thickness, argb, flow, speed);
        drawRect(x2 - thickness, y1 + thickness, x2, y2 - thickness, argb, flow, speed);
    }

    public static void drawCircle(float centerX, float centerY, float radius, float thickness, int argb) {
        drawCircle(centerX, centerY, radius, thickness, argb, false, 0.0F);
    }

    public static void drawCircle(float centerX, float centerY, float radius, float thickness, int argb,
                                  boolean flow, float speed) {
        float inner = Math.max(0.0F, radius - thickness / 2.0F);
        float outer = radius + thickness / 2.0F;
        if (flow) {
            FlowRenderer.drawRing2D(centerX, centerY, inner, outer, argb,
                    System.currentTimeMillis(), speed, 64);
            return;
        }
        setColor(argb);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_TRIANGLE_STRIP);
        int segments = 64;
        for (int i = 0; i <= segments; i++) {
            double angle = Math.PI * 2.0D * i / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            addVertex2D(tessellator, centerX + cos * outer, centerY + sin * outer, argb, flow, speed);
            addVertex2D(tessellator, centerX + cos * inner, centerY + sin * inner, argb, flow, speed);
        }
        tessellator.draw();
    }

    public static void drawLines(float[] points, float thickness, int argb) {
        drawLines(points, thickness, argb, false, 0.0F);
    }

    public static void drawLines(float[] points, float thickness, int argb, boolean flow, float speed) {
        if (flow) {
            long now = System.currentTimeMillis();
            for (int i = 0; i + 3 < points.length; i += 4) {
                FlowRenderer.drawLine2D(points[i], points[i + 1], points[i + 2], points[i + 3],
                        thickness, argb, now, speed, 8);
            }
            return;
        }
        setColor(argb);
        GL11.glLineWidth(thickness);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_LINES);
        for (int i = 0; i + 3 < points.length; i += 4) {
            addVertex2D(tessellator, points[i], points[i + 1], argb, flow, speed);
            addVertex2D(tessellator, points[i + 2], points[i + 3], argb, flow, speed);
        }
        tessellator.draw();
        GL11.glLineWidth(1.0F);
    }

    private static void addVertex2D(Tessellator tessellator, float x, float y, int argb,
                                    boolean flow, float speed) {
        if (flow) {
            setVertexColor(tessellator, Rainbow.colorAt2D(x, y, System.currentTimeMillis(), speed,
                    ColorUtil.alpha(argb)));
        }
        tessellator.addVertex(x, y, 0.0D);
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
