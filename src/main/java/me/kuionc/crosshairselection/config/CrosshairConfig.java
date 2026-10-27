package me.kuionc.crosshairselection.config;

import com.gtnewhorizon.gtnhlib.config.Config;
import me.kuionc.crosshairselection.render.CrosshairType;

/**
 * Crosshair settings.
 */
@Config(modid = "crosshairselection",
        category = "crosshair",
        configSubDirectory = "crosshairselection",
        filename = "config")
public class CrosshairConfig {

    @Config.Comment("Enable the custom crosshair. Vanilla crosshair is hidden while this is on.")
    public static boolean enabled = true;

    @Config.Comment("Crosshair shape.")
    public static CrosshairType type = CrosshairType.CROSS;

    @Config.Comment("ARGB base colour. Default: white.")
    public static int colour = 0xFFFFFFFF;

    @Config.Comment("Arm length on the horizontal axis (GUI pixels, decimals allowed).")
    @Config.RangeFloat(min = 0.0F, max = 50.0F)
    public static float width = 5.0F;

    @Config.Comment("Arm length on the vertical axis (GUI pixels, decimals allowed).")
    @Config.RangeFloat(min = 0.0F, max = 50.0F)
    public static float height = 5.0F;

    @Config.Comment("Gap between the centre and the arms, or the circle/square radius (GUI pixels).")
    @Config.RangeFloat(min = 0.0F, max = 50.0F)
    public static float gap = 3.0F;

    @Config.Comment("Thickness of the crosshair arms in quarter GUI pixels (1 = 0.25, 4 = 1.0).")
    @Config.RangeInt(min = 1, max = 40)
    public static int thickness = 3;

    @Config.Comment("Rotation in degrees.")
    @Config.RangeInt(min = 0, max = 360)
    public static int rotation = 0;

    @Config.Comment("Draw an outline around the crosshair.")
    public static boolean outline = false;

    @Config.Comment("ARGB outline colour.")
    public static int outlineColour = 0xFF000000;

    @Config.Comment("Outline thickness in GUI pixels (0 disables the outline).")
    @Config.RangeFloat(min = 0.0F, max = 2.0F)
    public static float outlineThickness = 0.5F;

    @Config.Comment("Draw a centre dot.")
    public static boolean dot = false;

    @Config.Comment("ARGB centre dot colour.")
    public static int dotColour = 0xFF000000;

    @Config.Comment("Cycle the crosshair through the colour spectrum.")
    public static boolean rainbow = false;

    @Config.Comment("Rainbow animation speed (0 - 2000).")
    @Config.RangeInt(min = 0, max = 2000)
    public static int rainbowSpeed = 500;

    @Config.Comment("Show the crosshair during normal gameplay.")
    public static boolean visibleDefault = true;

    @Config.Comment("Show the crosshair while the HUD is hidden with F1.")
    public static boolean visibleHiddenGui = true;

    @Config.Comment("Show the crosshair while the F3 debug screen is open.")
    public static boolean visibleDebug = true;

    @Config.Comment("Show the crosshair in third person.")
    public static boolean visibleThirdPerson = false;

    public static void reset() {
        enabled = true;
        type = CrosshairType.CROSS;
        colour = 0xFFFFFFFF;
        width = 5.0F;
        height = 5.0F;
        gap = 3.0F;
        thickness = 3;
        rotation = 0;
        outline = false;
        outlineColour = 0xFF000000;
        outlineThickness = 0.5F;
        dot = false;
        dotColour = 0xFF000000;
        rainbow = false;
        rainbowSpeed = 500;
        visibleDefault = true;
        visibleHiddenGui = true;
        visibleDebug = true;
        visibleThirdPerson = false;
    }
}
