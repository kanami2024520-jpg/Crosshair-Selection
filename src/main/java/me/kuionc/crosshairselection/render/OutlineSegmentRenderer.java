package me.kuionc.crosshairselection.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import me.kuionc.crosshairselection.util.McRef;

/**
 * 把世界坐标的线段换算成「朝向相机的四边形」参数，交给 {@link RibbonBuffer}。
 *
 * <p>注意事项：
 * <ul>
 *   <li>四边形是真实世界几何，尺寸由线宽换算得出；不能改成在着色器里按屏幕空间展开，
 *       那样线段一旦与屏幕轴平行就会退化成零面积而整条消失。</li>
 *   <li>端点不向外延伸：胶囊 SDF 的圆头会在共享角点处自然重叠并补满拐角。</li>
 *   <li>线宽按「物理像素 → 世界单位」换算，依赖当前 FOV 与窗口高度，
 *       所以同一个厚度设置在窗口大小变化后视觉粗细保持不变。</li>
 * </ul>
 */
public final class OutlineSegmentRenderer {

    private OutlineSegmentRenderer() {
    }

    public static void drawBox3D(AxisAlignedBB box, double cameraX, double cameraY, double cameraZ,
                                 int colour, float thicknessPixels) {
        drawLine(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
        drawLine(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
    }

    /**
     * @param thicknessPixels requested width in physical pixels
     */
    public static void drawLine(double x1, double y1, double z1, double x2, double y2, double z2,
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
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6D) {
            return;
        }

        // 法线 = 线段方向 × 视点方向，垂直于视线，于是四边形正对相机。
        double mx = (ax + bx) * 0.5D;
        double my = (ay + by) * 0.5D;
        double mz = (az + bz) * 0.5D;
        double nx = dy * mz - dz * my;
        double ny = dz * mx - dx * mz;
        double nz = dx * my - dy * mx;
        double normalLength = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (normalLength < 1.0E-6D) {
            nx = 1.0D;
            ny = 0.0D;
            nz = 0.0D;
            normalLength = 1.0D;
        }
        nx /= normalLength;
        ny /= normalLength;
        nz /= normalLength;

        // 像素→世界单位的换算：屏幕高度上的世界跨度 = 2·距离·tan(fov/2)。
        double distance = Math.sqrt(mx * mx + my * my + mz * mz);
        double fov = McRef.get().gameSettings.fovSetting;
        double worldPerPixel = 2.0D * Math.max(distance, 1.0E-4D)
                * Math.tan(Math.toRadians(fov) * 0.5D)
                / Math.max(1.0D, McRef.get().displayHeight);
        double halfWidth = Math.max(0.0008D, Math.max(thicknessPixels * 0.5D, 0.35D) * worldPerPixel);

        // 端点保持方块顶点原样，靠胶囊圆头补角。
        RibbonBuffer.addSegment(ax, ay, az, bx, by, bz,
                nx, ny, nz, halfWidth, thicknessPixels);
    }
}
