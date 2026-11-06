package me.kuionc.crosshairselection.render;

import net.minecraft.util.AxisAlignedBB;

/**
 * 边框线段的统一入口。
 *
 * <p>方块 12 条棱、九格网格线、圆柱母线都从这里进来，
 * 最终都变成 {@link RibbonBuffer} 里的一条胶囊线段，由片段着色器统一抗锯齿。
 * 保留这一层是为了让调用方不必知道底层是 SDF 还是别的实现。
 */
public final class ThickLineRenderer {

    private ThickLineRenderer() {
    }

    public static void drawBox3D(AxisAlignedBB box, double cameraX, double cameraY, double cameraZ,
                                 int colour, float thicknessPixels) {
        OutlineSegmentRenderer.drawBox3D(box, cameraX, cameraY, cameraZ, colour, thicknessPixels);
    }

    public static void drawLine3D(double x1, double y1, double z1, double x2, double y2, double z2,
                                  double cameraX, double cameraY, double cameraZ,
                                  int colour, float thicknessPixels) {
        OutlineSegmentRenderer.drawLine(x1, y1, z1, x2, y2, z2,
                cameraX, cameraY, cameraZ, colour, thicknessPixels);
    }
}
