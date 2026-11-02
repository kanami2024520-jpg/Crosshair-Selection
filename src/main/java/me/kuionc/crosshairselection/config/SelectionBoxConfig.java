package me.kuionc.crosshairselection.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Selection box settings. Colours are packed ARGB integers so the same value can be edited by
 * the colour picker and used directly by OpenGL.
 */
@Config(modid = "crosshairselection",
        category = "selection_box",
        configSubDirectory = "crosshairselection",
        filename = "config")
public class SelectionBoxConfig {

    @Config.Comment("Enable the custom block selection box.")
    public static boolean enabled = true;

    @Config.Comment("ARGB colour of the selection box. Default: 0xFF236EFF (blue).")
    public static int colour = 0xFF236EFF;

    @Config.Comment("Outline thickness multiplier (0.1 - 7.0).")
    @Config.RangeFloat(min = 0.1F, max = 7.0F)
    public static float thickness = 1.0F;

    @Config.Comment("Selection box margin in blocks (0.0 - 0.25). 0 hugs the shape exactly.")
    @Config.RangeFloat(min = 0.0F, max = 0.25F)
    public static float outlineMargin = 0.0F;

    @Config.Comment("Alpha of the blinking fill (0 - 255). 0 disables the fill.")
    @Config.RangeInt(min = 0, max = 255)
    public static int blinkAlpha = 99;

    @Config.Comment("Blink speed (0.0 - 1.0). 0 keeps the fill at a constant alpha.")
    @Config.RangeFloat(min = 0.0F, max = 1.0F)
    public static float blinkSpeed = 0.2F;

    @Config.Comment("Cycle the outline through the colour spectrum.")
    public static boolean rainbow = false;

    @Config.Comment("Use a local flowing rainbow gradient instead of one colour for the whole outline.")
    public static boolean rainbowFlow = true;

    @Config.Comment("Rainbow animation speed (0 - 2000).")
    @Config.RangeInt(min = 0, max = 2000)
    public static int rainbowSpeed = 500;

    @Config.Comment("Rainbow texture width: 0 = Auto, 128, 256 or 512.")
    public static int rainbowTextureSize = 0;

    @Config.Comment("Enable multisampling where the renderer supports it.")
    public static boolean antiAlias = false;

    @Config.Comment("Link the boxes of doors, beds, double plants and pistons.")
    public static boolean linkBlocks = false;

    @Config.Comment("Draw the selection box through blocks (ignore depth testing).")
    public static boolean showHidden = false;

    @Config.Comment("Animate the selection box when the targeted block changes.")
    public static boolean smoothTransition = false;

    @Config.Comment("Selection transition duration in seconds (0.05 - 0.5).")
    @Config.RangeFloat(min = 0.05F, max = 0.5F)
    public static float transitionDuration = 0.15F;

    @Config.Comment("Draw the inner fill inside the selection box.")
    public static boolean showFill = true;

    @Config.Comment("Animate the inner fill.")
    public static boolean blink = true;

    public static void reset() {
        enabled = true;
        colour = 0xFF236EFF;
        thickness = 1.0F;
        outlineMargin = 0.0F;
        blinkAlpha = 99;
        blinkSpeed = 0.2F;
        rainbow = false;
        rainbowFlow = true;
        rainbowSpeed = 500;
        rainbowTextureSize = 0;
        antiAlias = false;
        linkBlocks = false;
        showHidden = false;
        smoothTransition = false;
        transitionDuration = 0.15F;
        showFill = true;
        blink = true;
    }
}
