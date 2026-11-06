package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.config.GtXStyle;

import java.util.List;

/**
 * CPU mirror of {@code selection.frag} used by the settings preview.
 *
 * <p>The preview cannot run the world shader, so it reproduces the same hue formula: the band
 * coordinate is the dot product of the world position with the diagonal, scaled by the band
 * density, plus the animated phase. Sampling this along each previewed segment keeps the colour
 * sweep in the preview pointing the way it does in game.
 */
public final class FlowPreview {

    private static final double INV_SQRT3 = 0.5773502691896258D;

    private FlowPreview() {
    }

    /**
     * Colour at a camera-independent world position.
     *
     * @param flow    settings; may be null or OFF, which returns the base colour
     * @param baseColour ARGB used when the flow is inactive
     * @param now     milliseconds timestamp
     * @param t       0..1 position along the previewed segment, used by the centre-out sweep
     */
    public static int colourAt(FlowSettings flow, int baseColour, long now,
                               double worldX, double worldY, double worldZ, double t) {
        if (flow == null || flow.mode == FlowSettings.Mode.OFF) {
            return baseColour;
        }
        float phase = motionPhase(phase(flow, now), flow.motion, (float) worldX, (float) worldZ);
        double band = (worldX + worldY + worldZ) * INV_SQRT3 * flow.bandDensity;
        double hue = fract(flow.hueStart + fract(band + phase) * flow.hueRange);
        int rgb = me.kuionc.crosshairselection.util.ColorUtil.hsbToRgb(
                (float) hue, flow.saturation, flow.brightness);

        if (flow.mode == FlowSettings.Mode.SWEEP) {
            double along = flow.direction == FlowSettings.Direction.CENTER_OUT
                    ? Math.abs(t * 2.0D - 1.0D)
                    : sweepAlong(flow, worldX, worldY, worldZ);
            double d = along - flow.progress;
            double softness = Math.max(flow.sweepSoftness, 0.001D);
            double mask = d <= 0.0D ? 1.0D : 1.0D - smoothstep(0.0D, softness, d);
            rgb = mix(baseColour & 0xFFFFFF, rgb, mask);
        }
        return (baseColour & 0xFF000000) | (rgb & 0xFFFFFF);
    }

    /** Phases a solid colour the way the shader does, for single-colour previews. */
    public static int solidColour(FlowSettings flow, int baseColour, long now) {
        return colourAt(flow, baseColour, now, 0.0D, 0.0D, 0.0D, 0.5D);
    }

    private static double sweepAlong(FlowSettings flow, double x, double y, double z) {
        float[] axis = ShaderSupport.sweepAxis(flow);
        return x * axis[0] + y * axis[1] + z * axis[2];
    }

    private static float phase(FlowSettings flow, long now) {
        return ShaderSupport.phase(flow, now);
    }

    private static float motionPhase(float x, FlowSettings.Motion motion, float px, float pz) {
        switch (motion) {
            case COSINE:
                return (float) (0.5D - 0.5D * Math.cos(6.283185307179586D * x));
            case TRIANGLE:
                return Math.abs((float) (x - Math.floor(x)) * 2.0F - 1.0F);
            case NOISE:
                // Mirrors the shader: a triangle-wave time axis sampled diagonally in noise space.
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

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0.0D, Math.min(1.0D, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0D - 2.0D * t);
    }

    private static int mix(int base, int flow, double t) {
        int r = (int) Math.round(((base >> 16) & 255) * (1.0D - t) + ((flow >> 16) & 255) * t);
        int g = (int) Math.round(((base >> 8) & 255) * (1.0D - t) + ((flow >> 8) & 255) * t);
        int b = (int) Math.round((base & 255) * (1.0D - t) + (flow & 255) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static float hash21(float x, float y) {
        float px = (float) fract(x * 123.34F);
        float py = (float) fract(y * 456.21F);
        float dot = px * px + py * py + px * 45.32F + py * 45.32F;
        px += dot;
        py += dot;
        return (float) fract(px * py);
    }

    private static float valueNoise(float x) {
        return valueNoise(x, x * 0.37F);
    }

    private static float valueNoise(float x, float y) {
        float ix = (float) Math.floor(x);
        float iy = (float) Math.floor(y);
        float fx = x - ix;
        float fy = y - iy;
        float ux = fx * fx * (3.0F - 2.0F * fx);
        float uy = fy * fy * (3.0F - 2.0F * fy);
        float a = hash21(ix, iy);
        float b = hash21(ix + 1.0F, iy);
        float c = hash21(ix, iy + 1.0F);
        float d = hash21(ix + 1.0F, iy + 1.0F);
        return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uy;
    }
}
