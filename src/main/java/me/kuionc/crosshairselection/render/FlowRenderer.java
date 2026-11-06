package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Texture-based rainbow rendering. A 1D HSV texture is sampled with position-dependent UVs, so
 * the GPU produces a smooth per-fragment gradient without splitting geometry into segments.
 */
public final class FlowRenderer {

    private FlowRenderer() {
    }

    public static void drawBox3D(AxisAlignedBB box, double cameraX, double cameraY, double cameraZ,
                                 int colour, long time, float speed, int ignoredSegments,
                                 float thicknessPixels) {
        int alpha = (colour >>> 24) & 255;
        drawLine3D(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
        drawLine3D(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, alpha, time, speed, 1, thicknessPixels);
    }

    public static void drawLine3D(double x1, double y1, double z1, double x2, double y2, double z2,
                                  double cameraX, double cameraY, double cameraZ,
                                  int alpha, long time, float speed, int ignoredSegments,
                                  float thicknessPixels) {
        drawTexturedLine(x1, y1, z1, x2, y2, z2, cameraX, cameraY, cameraZ,
                alpha, time, speed, thicknessPixels);
    }

    public static void drawSegment3D(double x1, double y1, double z1, double x2, double y2, double z2,
                                     double cameraX, double cameraY, double cameraZ, int colour,
                                     float thicknessPixels) {
        drawTexturedLine(x1, y1, z1, x2, y2, z2, cameraX, cameraY, cameraZ,
                (colour >>> 24) & 255, System.currentTimeMillis(),
                SelectionBoxConfig.rainbowSpeed, thicknessPixels);
    }

    private static void drawTexturedLine(double x1, double y1, double z1, double x2, double y2, double z2,
                                         double cameraX, double cameraY, double cameraZ,
                                         int alpha, long time, float speed, float thicknessPixels) {
        double ax = x1 - cameraX;
        double ay = y1 - cameraY;
        double az = z1 - cameraZ;
        double bx = x2 - cameraX;
        double by = y2 - cameraY;
        double bz = z2 - cameraZ;

        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6D) {
            return;
        }
        double mx = (ax + bx) * 0.5D;
        double my = (ay + by) * 0.5D;
        double mz = (az + bz) * 0.5D;
        double distance = Math.sqrt(mx * mx + my * my + mz * mz);
        if (distance < 1.0E-4D) {
            distance = 1.0E-4D;
        }

        double nx = dy * mz - dz * my;
        double ny = dz * mx - dx * mz;
        double nz = dx * my - dy * mx;
        double normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (normalLength < 1.0E-6D) {
            nx = -dz;
            ny = 0.0D;
            nz = dx;
            normalLength = Math.sqrt(nx * nx + nz * nz);
            if (normalLength < 1.0E-6D) {
                nx = 1.0D;
                ny = 0.0D;
                nz = 0.0D;
                normalLength = 1.0D;
            }
        }
        nx /= normalLength;
        ny /= normalLength;
        nz /= normalLength;

        double fov = McRef.get().gameSettings.fovSetting;
        double worldPerPixel = 2.0D * distance * Math.tan(Math.toRadians(fov) * 0.5D)
                / Math.max(1.0D, McRef.get().displayHeight);
        double half = Math.max(0.0008D, thicknessPixels * 0.5D * worldPerPixel);
        double unitX = nx;
        double unitY = ny;
        double unitZ = nz;
        boolean feather = false;

        RainbowTexture.bind();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, (alpha & 255) / 255.0F);
        Tessellator t = TessellatorManager.get();
        t.startDrawingQuads();
        float ua = flowU(x1, y1, z1, time, speed);
        float ub = flowU(x2, y2, z2, time, speed);
        if (feather) {
            double fade = Math.min(half * 0.5D, worldPerPixel);
            double core = Math.max(half - fade, half * 0.45D);
            double cx = unitX * core;
            double cy = unitY * core;
            double cz = unitZ * core;
            addFlowVertex(t, ax + cx, ay + cy, az + cz, ua, alpha);
            addFlowVertex(t, bx + cx, by + cy, bz + cz, ub, alpha);
            addFlowVertex(t, bx - cx, by - cy, bz - cz, ub, alpha);
            addFlowVertex(t, ax - cx, ay - cy, az - cz, ua, alpha);
            addFlowFadeQuad(t, ax, ay, az, bx, by, bz, unitX, unitY, unitZ,
                    core, half, ua, ub, alpha);
            addFlowFadeQuad(t, ax, ay, az, bx, by, bz, -unitX, -unitY, -unitZ,
                    core, half, ua, ub, alpha);
        } else {
            nx *= half;
            ny *= half;
            nz *= half;
            addVertex(t, ax + nx, ay + ny, az + nz, ua);
            addVertex(t, bx + nx, by + ny, bz + nz, ub);
            addVertex(t, bx - nx, by - ny, bz - nz, ub);
            addVertex(t, ax - nx, ay - ny, az - nz, ua);
        }
        t.draw();
    }

    private static void addFlowFadeQuad(Tessellator t,
                                        double ax, double ay, double az,
                                        double bx, double by, double bz,
                                        double nx, double ny, double nz,
                                        double core, double half,
                                        float ua, float ub, int alpha) {
        double ox = nx * half;
        double oy = ny * half;
        double oz = nz * half;
        double ix = nx * core;
        double iy = ny * core;
        double iz = nz * core;
        addFlowVertex(t, ax + ix, ay + iy, az + iz, ua, alpha);
        addFlowVertex(t, bx + ix, by + iy, bz + iz, ub, alpha);
        addFlowVertex(t, bx + ox, by + oy, bz + oz, ub, 0);
        addFlowVertex(t, ax + ox, ay + oy, az + oz, ua, 0);
    }

    private static void addFlowVertex(Tessellator t, double x, double y, double z, float u, int alpha) {
        RenderHelper.setVertexColor(t, 0xFFFFFF | ((alpha & 255) << 24));
        t.addVertexWithUV(x, y, z, u, 0.5D);
    }

    public static void drawFilledBox3D(AxisAlignedBB box, double cameraX, double cameraY, double cameraZ,
                                       int colour, long time, float speed, int ignoredStrips) {
        int alpha = (colour >>> 24) & 255;
        RainbowTexture.bind();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, (alpha & 255) / 255.0F);
        Tessellator t = TessellatorManager.get();
        t.startDrawingQuads();
        addTexturedQuad(t, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, time, speed);
        addTexturedQuad(t, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ,
                box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, time, speed);
        addTexturedQuad(t, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ,
                cameraX, cameraY, cameraZ, time, speed);
        addTexturedQuad(t, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ,
                box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, time, speed);
        addTexturedQuad(t, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ,
                box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, time, speed);
        addTexturedQuad(t, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, time, speed);
        t.draw();
    }

    private static void addTexturedQuad(Tessellator t,
                                        double x1, double y1, double z1, double x2, double y2, double z2,
                                        double x3, double y3, double z3, double x4, double y4, double z4,
                                        double cameraX, double cameraY, double cameraZ,
                                        long time, float speed) {
        addVertex(t, x1 - cameraX, y1 - cameraY, z1 - cameraZ, flowU(x1, y1, z1, time, speed));
        addVertex(t, x2 - cameraX, y2 - cameraY, z2 - cameraZ, flowU(x2, y2, z2, time, speed));
        addVertex(t, x3 - cameraX, y3 - cameraY, z3 - cameraZ, flowU(x3, y3, z3, time, speed));
        addVertex(t, x4 - cameraX, y4 - cameraY, z4 - cameraZ, flowU(x4, y4, z4, time, speed));
    }

    private static void addVertex(Tessellator t, double x, double y, double z, float u) {
        t.addVertexWithUV(x, y, z, u, 0.5D);
    }

    public static void drawQuad3D(double x1, double y1, double z1, double x2, double y2, double z2,
                                  double x3, double y3, double z3, double x4, double y4, double z4,
                                  double cameraX, double cameraY, double cameraZ, int colour) {
        RenderHelper.bindWhiteTexture();
        RenderHelper.setColor(colour);
        Tessellator t = TessellatorManager.get();
        t.startDrawingQuads();
        RenderHelper.setVertexColor(t, colour);
        t.addVertexWithUV(x1 - cameraX, y1 - cameraY, z1 - cameraZ, 0.5D, 0.5D);
        RenderHelper.setVertexColor(t, colour);
        t.addVertexWithUV(x2 - cameraX, y2 - cameraY, z2 - cameraZ, 0.5D, 0.5D);
        RenderHelper.setVertexColor(t, colour);
        t.addVertexWithUV(x3 - cameraX, y3 - cameraY, z3 - cameraZ, 0.5D, 0.5D);
        RenderHelper.setVertexColor(t, colour);
        t.addVertexWithUV(x4 - cameraX, y4 - cameraY, z4 - cameraZ, 0.5D, 0.5D);
        t.draw();
    }

    public static void drawRect2D(float x1, float y1, float x2, float y2, int colour,
                                  long time, float speed, int ignoredStrips) {
        RainbowTexture.bind();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, ((colour >>> 24) & 255) / 255.0F);
        Tessellator t = TessellatorManager.get();
        t.startDrawingQuads();
        addVertex2D(t, x1, y2, flowU2D(x1, y2, time, speed));
        addVertex2D(t, x2, y2, flowU2D(x2, y2, time, speed));
        addVertex2D(t, x2, y1, flowU2D(x2, y1, time, speed));
        addVertex2D(t, x1, y1, flowU2D(x1, y1, time, speed));
        t.draw();
    }

    public static void drawQuad2D(float x1, float y1, float x2, float y2,
                                  float x3, float y3, float x4, float y4, int colour) {
        RenderHelper.bindWhiteTexture();
        RenderHelper.setColor(colour);
        Tessellator t = TessellatorManager.get();
        t.startDrawingQuads();
        RenderHelper.setVertexColor(t, colour);
        t.addVertex(x1, y1, 0.0D);
        RenderHelper.setVertexColor(t, colour);
        t.addVertex(x2, y2, 0.0D);
        RenderHelper.setVertexColor(t, colour);
        t.addVertex(x3, y3, 0.0D);
        RenderHelper.setVertexColor(t, colour);
        t.addVertex(x4, y4, 0.0D);
        t.draw();
    }

    public static void drawLine2D(float x1, float y1, float x2, float y2, float thickness,
                                  int colour, long time, float speed, int segments) {
        GL11.glLineWidth(thickness);
        RenderHelper.setColor(colour);
        Tessellator t = TessellatorManager.get();
        t.startDrawing(GL11.GL_LINES);
        t.addVertex(x1, y1, 0.0D);
        t.addVertex(x2, y2, 0.0D);
        t.draw();
        GL11.glLineWidth(1.0F);
    }

    public static void drawRing2D(float centerX, float centerY, float inner, float outer,
                                  int colour, long time, float speed, int segments) {
        RainbowTexture.bind();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, ((colour >>> 24) & 255) / 255.0F);
        Tessellator t = TessellatorManager.get();
        t.startDrawingQuads();
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2.0D * i / segments;
            double a1 = Math.PI * 2.0D * (i + 1) / segments;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            addVertex2D(t, centerX + c0 * outer, centerY + s0 * outer,
                    flowU2D(centerX + c0 * outer, centerY + s0 * outer, time, speed));
            addVertex2D(t, centerX + c1 * outer, centerY + s1 * outer,
                    flowU2D(centerX + c1 * outer, centerY + s1 * outer, time, speed));
            addVertex2D(t, centerX + c1 * inner, centerY + s1 * inner,
                    flowU2D(centerX + c1 * inner, centerY + s1 * inner, time, speed));
            addVertex2D(t, centerX + c0 * inner, centerY + s0 * inner,
                    flowU2D(centerX + c0 * inner, centerY + s0 * inner, time, speed));
        }
        t.draw();
    }

    private static void addVertex2D(Tessellator t, float x, float y, float u) {
        t.addVertexWithUV(x, y, 0.0D, u, 0.5D);
    }

    private static float flowU(double x, double y, double z, long time, float speed) {
        return flowCoordinate((x + y + z) * 0.2D, time, speed);
    }

    public static float flowU2D(float x, float y, long time, float speed) {
        return flowCoordinate((x + y) * 0.02D, time, speed);
    }

    /**
     * Wraps the time component in double precision, then adds the position component without
     * wrapping it. Casting the unwrapped timestamp first quantises it to a multiple of 64 and
     * leaves the fraction at zero, which makes every fragment sample hue 0 (red). Keeping the
     * position offset unwrapped also avoids a backwards hue sweep between vertices that straddle
     * the 0/1 texture seam; GL_REPEAT handles the wrap during sampling instead.
     */
    private static float flowCoordinate(double position, long time, float speed) {
        double phase = time * 0.001D * speed * 0.001D;
        phase -= Math.floor(phase);
        return (float) (phase + position);
    }
}
