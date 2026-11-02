package me.kuionc.crosshairselection.config;

import com.gtnewhorizon.gtnhlib.config.Config;

/**
 * Appearance of the custom replacement for GT's wrench/cover grid overlay.
 */
@Config(modid = "crosshairselection",
        category = "gt_overlay",
        configSubDirectory = "crosshairselection",
        filename = "config")
public class GtOverlayConfig {

    @Config.Comment("Replace GT's wrench/cover grid overlay with this mod's overlay.")
    public static boolean enabled = true;

    @Config.Comment("Reuse the normal selection box colour, thickness and margin for the GT overlay.")
    public static boolean useSelectionStyle = true;

    @Config.Comment("ARGB colour of the GT overlay box.")
    public static int colour = 0xFF000000;

    @Config.Comment("GT overlay box thickness in GUI pixels.")
    @Config.RangeFloat(min = 0.1F, max = 7.0F)
    public static float thickness = 2.5F;

    @Config.Comment("Extra margin around the GT overlay box in blocks.")
    @Config.RangeFloat(min = 0.0F, max = 0.02F)
    public static float margin = 0.002F;

    @Config.Comment("Draw the 3x3 interaction grid on the targeted face.")
    public static boolean showGrid = true;

    @Config.Comment("Draw the 3x3 grid on all six faces.")
    public static boolean gridAllFaces = false;

    @Config.Comment("Draw markers on cells that map to the machine output or pipe connections.")
    public static boolean showInputFaceX = true;

    @Config.Comment("Appearance of the marked cell.")
    public static GtXStyle xStyle = GtXStyle.CROSS;

    @Config.Comment("ARGB colour of the 3x3 grid.")
    public static int gridColour = 0xFF000000;

    @Config.Comment("3x3 grid line thickness in GUI pixels.")
    @Config.RangeFloat(min = 0.1F, max = 7.0F)
    public static float gridThickness = 2.5F;

    @Config.Comment("Inset of the 3x3 grid from the face edge.")
    @Config.RangeFloat(min = 0.0F, max = 0.25F)
    public static float gridInset = 0.0F;

    @Config.Comment("Width of each outer cell as a fraction of the face.")
    @Config.RangeFloat(min = 0.05F, max = 0.45F)
    public static float gridCellSize = 1.0F / 3.0F;

    @Config.Comment("Only accept GT tool clicks inside the rendered grid; clicks outside do nothing.")
    public static boolean strictGridClicks = true;

    @Config.Comment("Animate the GT overlay when the targeted block changes.")
    public static boolean smoothTransition = false;

    @Config.Comment("GT overlay transition duration in seconds (0.05 - 0.5).")
    @Config.RangeFloat(min = 0.05F, max = 0.5F)
    public static float transitionDuration = 0.15F;

    public static void reset() {
        enabled = true;
        useSelectionStyle = true;
        colour = 0xFF000000;
        thickness = 2.5F;
        margin = 0.002F;
        showGrid = true;
        gridAllFaces = false;
        showInputFaceX = true;
        xStyle = GtXStyle.CROSS;
        gridColour = 0xFF000000;
        gridThickness = 2.5F;
        gridInset = 0.0F;
        gridCellSize = 1.0F / 3.0F;
        strictGridClicks = true;
        smoothTransition = false;
        transitionDuration = 0.15F;
    }
}
