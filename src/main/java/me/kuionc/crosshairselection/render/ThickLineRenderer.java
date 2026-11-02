package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

/**
 * Draws world-space lines as camera-facing quads. OpenGL line width above one pixel is commonly
 * ignored by the shader/GL core pipeline, so quads are used to keep the thickness slider working.
 */
public final class ThickLineRenderer {

    private ThickLineRenderer() {
    }

    public static void drawBox3D(AxisAlignedBB box, double cameraX, double cameraY, double cameraZ,
                                 int colour, float thicknessPixels) {
        drawLine3D(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine3D(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
    }

    public static void drawLine3D(double x1, double y1, double z1, double x2, double y2, double z2,
                                  double cameraX, double cameraY, double cameraZ,
                                  int colour, float thicknessPixels) {
        double ax = x1 - cameraX;
        double ay = y1 - cameraY;
        double az = z1 - cameraZ;
        double bx = x2 - cameraX;
        double by = y2 - cameraY;
        double bz = z2 - cameraZ;

        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double lineLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (lineLength < 1.0E-6D) {
            return;
        }

        double mx = (ax + bx) * 0.5D;
        double my = (ay + by) * 0.5D;
        double mz = (az + bz) * 0.5D;
        double distance = Math.sqrt(mx * mx + my * my + mz * mz);
        if (distance < 1.0E-4D) {
            distance = 1.0E-4D;
        }

        // Perpendicular to both the line and the view direction; this makes the quad face the camera.
        double nx = dy * mz - dz * my;
        double ny = dz * mx - dx * mz;
        double nz = dx * my - dy * mx;
        double normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (normalLength < 1.0E-6D) {
            nx = dy * 0.0D - dz * 1.0D;
            ny = dz * 0.0D - dx * 0.0D;
            nz = dx * 1.0D - dy * 0.0D;
            normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);
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

        double fov = Minecraft.getMinecraft().gameSettings.fovSetting;
        double worldPerPixel = 2.0D * distance * Math.tan(Math.toRadians(fov) * 0.5D)
                / Math.max(1.0D, Minecraft.getMinecraft().displayHeight);
        double halfWidth = Math.max(0.0008D, thicknessPixels * 0.5D * worldPerPixel);
        double unitX = nx;
        double unitY = ny;
        double unitZ = nz;

        RenderHelper.setColor(colour);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        if (SelectionBoxConfig.antiAlias) {
            // Thick quads have hard edges even when MSAA is unavailable. Draw an opaque core and
            // two one-pixel alpha fades so every selection/GT border keeps a smooth silhouette.
            double feather = Math.min(halfWidth * 0.5D, worldPerPixel);
            double core = Math.max(halfWidth - feather, halfWidth * 0.45D);
            int transparent = colour & 0x00FFFFFF;
            double cx = unitX * core;
            double cy = unitY * core;
            double cz = unitZ * core;
            addVertex(tessellator, ax + cx, ay + cy, az + cz, colour);
            addVertex(tessellator, bx + cx, by + cy, bz + cz, colour);
            addVertex(tessellator, bx - cx, by - cy, bz - cz, colour);
            addVertex(tessellator, ax - cx, ay - cy, az - cz, colour);
            addFadeQuad(tessellator, ax, ay, az, bx, by, bz,
                    unitX, unitY, unitZ, core, halfWidth, colour, transparent);
            addFadeQuad(tessellator, ax, ay, az, bx, by, bz,
                    -unitX, -unitY, -unitZ, core, halfWidth, colour, transparent);
        } else {
            nx *= halfWidth;
            ny *= halfWidth;
            nz *= halfWidth;
            addVertex(tessellator, ax + nx, ay + ny, az + nz, colour);
            addVertex(tessellator, bx + nx, by + ny, bz + nz, colour);
            addVertex(tessellator, bx - nx, by - ny, bz - nz, colour);
            addVertex(tessellator, ax - nx, ay - ny, az - nz, colour);
        }
        tessellator.draw();
    }

    private static void addFadeQuad(Tessellator tessellator,
                                    double ax, double ay, double az,
                                    double bx, double by, double bz,
                                    double nx, double ny, double nz,
                                    double core, double halfWidth,
                                    int colour, int transparent) {
        double ox = nx * halfWidth;
        double oy = ny * halfWidth;
        double oz = nz * halfWidth;
        double ix = nx * core;
        double iy = ny * core;
        double iz = nz * core;
        addVertex(tessellator, ax + ix, ay + iy, az + iz, colour);
        addVertex(tessellator, bx + ix, by + iy, bz + iz, colour);
        addVertex(tessellator, bx + ox, by + oy, bz + oz, transparent);
        addVertex(tessellator, ax + ox, ay + oy, az + oz, transparent);
    }

    private static void addVertex(Tessellator tessellator, double x, double y, double z, int colour) {
        RenderHelper.setVertexColor(tessellator, colour);
        tessellator.addVertexWithUV(x, y, z, 0.5D, 0.5D);
    }
}
