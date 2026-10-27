package me.kuionc.crosshairselection.util;

/**
 * Small ARGB/HSB helper used by both renderers and the colour picker.
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static int pack(int red, int green, int blue, int alpha) {
        return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255);
    }

    public static int alpha(int colour) {
        return colour >>> 24 & 255;
    }

    public static int red(int colour) {
        return colour >> 16 & 255;
    }

    public static int green(int colour) {
        return colour >> 8 & 255;
    }

    public static int blue(int colour) {
        return colour & 255;
    }

    public static int withAlpha(int colour, int alpha) {
        return (alpha & 255) << 24 | (colour & 0x00FFFFFF);
    }

    public static int hsbToRgb(float hue, float saturation, float brightness) {
        hue = hue - (float) Math.floor(hue);
        int r;
        int g;
        int b;
        if (saturation <= 0.0F) {
            r = g = b = (int) (brightness * 255.0F + 0.5F);
        } else {
            float h = (hue - (float) Math.floor(hue)) * 6.0F;
            float f = h - (float) Math.floor(h);
            float p = brightness * (1.0F - saturation);
            float q = brightness * (1.0F - saturation * f);
            float t = brightness * (1.0F - saturation * (1.0F - f));
            switch ((int) h) {
                case 0:
                    r = (int) (brightness * 255.0F + 0.5F);
                    g = (int) (t * 255.0F + 0.5F);
                    b = (int) (p * 255.0F + 0.5F);
                    break;
                case 1:
                    r = (int) (q * 255.0F + 0.5F);
                    g = (int) (brightness * 255.0F + 0.5F);
                    b = (int) (p * 255.0F + 0.5F);
                    break;
                case 2:
                    r = (int) (p * 255.0F + 0.5F);
                    g = (int) (brightness * 255.0F + 0.5F);
                    b = (int) (t * 255.0F + 0.5F);
                    break;
                case 3:
                    r = (int) (p * 255.0F + 0.5F);
                    g = (int) (q * 255.0F + 0.5F);
                    b = (int) (brightness * 255.0F + 0.5F);
                    break;
                case 4:
                    r = (int) (t * 255.0F + 0.5F);
                    g = (int) (p * 255.0F + 0.5F);
                    b = (int) (brightness * 255.0F + 0.5F);
                    break;
                case 5:
                default:
                    r = (int) (brightness * 255.0F + 0.5F);
                    g = (int) (p * 255.0F + 0.5F);
                    b = (int) (q * 255.0F + 0.5F);
                    break;
            }
        }
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * @return {hue, saturation, brightness}
     */
    public static float[] rgbToHsb(int colour) {
        float r = red(colour) / 255.0F;
        float g = green(colour) / 255.0F;
        float b = blue(colour) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float brightness = max;
        float saturation = max == 0.0F ? 0.0F : (max - min) / max;
        float hue = 0.0F;
        if (max != min) {
            float delta = max - min;
            if (max == r) {
                hue = (g - b) / delta;
            } else if (max == g) {
                hue = 2.0F + (b - r) / delta;
            } else {
                hue = 4.0F + (r - g) / delta;
            }
            hue /= 6.0F;
            if (hue < 0.0F) {
                hue += 1.0F;
            }
        }
        return new float[]{hue, saturation, brightness};
    }

    public static String hex(int colour) {
        return String.format("#%02X%02X%02X", red(colour), green(colour), blue(colour));
    }

    public static int rainbowColour(long millis, float saturation, float brightness, int alpha) {
        float hue = (millis % 10000L) / 10000.0F;
        return withAlpha(hsbToRgb(hue, saturation, brightness), alpha);
    }
}
