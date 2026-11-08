package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.util.ColorUtil;

/**
 * CPU mirror of {@code selection.frag}, used by the settings preview.
 *
 * <p>预览画不了世界着色器，所以这里复刻同一套公式：方向轴、原点、投影范围、
 * 扫光归一化，以及「量子闪烁」的撕裂、色差与扫描线。采样点沿预览线段
 * 逐段求值，方向与游戏中保持一致。
 */
public final class FlowPreview {

    /** 预览立方体的半对角线长度，用于把扫光进度映射到形状范围。 */
    private static final double HALF_DIAGONAL = Math.sqrt(3.0D) * 0.5D;

    private FlowPreview() {
    }

    /**
     * 颜色采样。
     *
     * @param flow       设置；null 或 OFF 时返回基色
     * @param baseColour 未启用流光时的 ARGB
     * @param now        毫秒时间戳
     * @param t          预览线段上的 0..1 位置（旧参数，保留兼容）
     */
    public static int colourAt(FlowSettings flow, int baseColour, long now,
                               double worldX, double worldY, double worldZ, double t) {
        if (flow == null || flow.mode == FlowSettings.Mode.OFF) {
            return baseColour;
        }
        int alpha = ColorUtil.alpha(baseColour);
        double phase = motionPhase(ShaderSupport.phase(flow, now), flow.motion,
                (float) worldX, (float) worldZ);

        double coord = coordinate(flow, worldX, worldY, worldZ);
        int rgb = hueAt(flow, phase, coord);
        if (flow.mode == FlowSettings.Mode.SWEEP) {
            double along = along(flow, coord);
            double d = along - flow.progress;
            double softness = Math.max(flow.sweepSoftness, 0.001D);
            double mask = d <= 0.0D ? 1.0D : 1.0D - smoothstep(0.0D, softness, d);
            rgb = mix(baseColour & 0xFFFFFF, rgb, mask);
        }
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** 相位的单色取值，供纯色预览使用。 */
    public static int solidColour(FlowSettings flow, int baseColour, long now) {
        return colourAt(flow, baseColour, now, 0.0D, 0.0D, 0.0D, 0.5D);
    }

    private static int hue(FlowSettings flow, double phase, double x, double y, double z) {
        return hueAt(flow, phase, coordinate(flow, x, y, z));
    }

    private static int hueAt(FlowSettings flow, double phase, double coord) {
        double hue = fract(flow.hueStart + fract(coord * flow.bandDensity + phase) * flow.hueRange);
        return ColorUtil.hsbToRgb((float) hue, flow.saturation, flow.brightness) & 0xFFFFFF;
    }

    /** 流光坐标系里的取样坐标：角到角沿方向轴投影，中心向外用半径。 */
    private static double coordinate(FlowSettings flow, double x, double y, double z) {
        if (flow.direction == FlowSettings.Direction.CENTER_OUT) {
            return Math.sqrt(x * x + y * y + z * z);
        }
        float[] axis = ShaderSupport.FlowFrame.axisFor(flow);
        return x * axis[0] + y * axis[1] + z * axis[2];
    }

    /** 扫光进度归一化到 0..1。 */
    private static double along(FlowSettings flow, double coord) {
        if (flow.direction == FlowSettings.Direction.CENTER_OUT) {
            return coord / HALF_DIAGONAL;
        }
        return (coord + HALF_DIAGONAL) / (2.0D * HALF_DIAGONAL);
    }

        private static float motionPhase(float x, FlowSettings.Motion motion, float px, float pz) {
        switch (motion) {
            case COSINE:
                // 单向流动，速度平滑变化：单调递增的余弦缓动。
                // 以前用的是 0.5 - 0.5cos(2πx)，和三角波一样是往复振荡，观感几乎一样。
                double wrapped = x - Math.floor(x);
                return (float) (wrapped - Math.sin(6.283185307179586D * wrapped)
                        / 6.283185307179586D);
            case TRIANGLE:
                return Math.abs((float) (x - Math.floor(x)) * 2.0F - 1.0F);
            case NOISE:
                float t = (float) (Math.abs(x - Math.floor(x)) * 2.0D - 1.0D) * 8.0F;
                return (float) (valueNoise(px * 40.0F * 0.05F + t * 0.8F,
                        pz * 40.0F * 0.05F + t * 0.5F) * 0.65D
                        + valueNoise(px * 40.0F * 0.10F + t * 1.6F + 17.3F,
                        pz * 40.0F * 0.10F + t * 1.1F) * 0.35D);
            case LINEAR:
            default:
                return (float) (x - Math.floor(x));
        }
    }

    private static int pack(int r, int g, int b) {
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int scale(int channel, double factor) {
        return clamp((int) Math.round(channel * factor));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int mix(int a, int b, double t) {
        int ar = a >>> 16 & 255;
        int ag = a >>> 8 & 255;
        int ab = a & 255;
        int br = b >>> 16 & 255;
        int bg = b >>> 8 & 255;
        int bb = b & 255;
        return pack((int) Math.round(ar + (br - ar) * t),
                (int) Math.round(ag + (bg - ag) * t),
                (int) Math.round(ab + (bb - ab) * t));
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0.0D, Math.min(1.0D, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

        private static double valueNoise(float x, float z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        float fx = x - ix;
        float fz = z - iz;
        float ux = fx * fx * (3.0F - 2.0F * fx);
        float uz = fz * fz * (3.0F - 2.0F * fz);
        float a = hashCell(ix, iz);
        float b = hashCell(ix + 1, iz);
        float c = hashCell(ix, iz + 1);
        float d = hashCell(ix + 1, iz + 1);
        return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uz;
    }

    private static float hashCell(int x, int z) {
        long h = (long) x * 374761393L + (long) z * 668265263L;
        return hash(h);
    }

    private static float hash(long value) {
        long x = value;
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        x = x ^ (x >>> 33);
        return (float) ((x >>> 40) / (double) (1L << 24));
    }
}
