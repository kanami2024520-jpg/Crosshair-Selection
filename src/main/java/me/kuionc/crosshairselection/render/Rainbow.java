package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.util.ColorUtil;

/**
 * Position-aware rainbow colours used by the flowing rainbow modes. The hue is based on the
 * vertex position and the breathing brightness is based on time.
 */
public final class Rainbow {

    private static final float SATURATION = 0.8F;

    private Rainbow() {
    }

    public static int colorAt(double x, double y, double z, long millis, float speed, int alpha) {
        float position = (float) ((x + y + z) * 0.35D);
        return colorAt(position, millis, speed, alpha);
    }

    public static int colorAt2D(double x, double y, long millis, float speed, int alpha) {
        float position = (float) ((x + y) * 0.02D);
        return colorAt(position, millis, speed, alpha);
    }

    private static int colorAt(float position, long millis, float speed, int alpha) {
        if (speed <= 0.0F) {
            return ColorUtil.withAlpha(ColorUtil.hsbToRgb(position, SATURATION, 0.9F), alpha);
        }
        double seconds = millis / 1000.0D;
        double flow = position + seconds * speed * 0.001D;
        flow -= Math.floor(flow);
        float breath = 0.72F + 0.28F * (0.5F + 0.5F * (float) Math.sin(
                seconds * (0.8D + speed * 0.001D)));
        return ColorUtil.withAlpha(ColorUtil.hsbToRgb((float) flow, SATURATION, breath), alpha);
    }
}
