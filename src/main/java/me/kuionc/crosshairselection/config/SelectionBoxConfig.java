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
    public static float thickness = 1.5F;

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

    @Config.Comment("Show how far the targeted block is from breaking.")
    public static boolean showBreakProgress = false;

    @Config.Comment("Appearance of the mining progress indicator.")
    public static BreakProgressStyle breakProgressStyle = BreakProgressStyle.BAR;

    @Config.Comment("Where the break progress percentage is drawn, relative to the crosshair.")
    public static HudPosition breakProgressPosition = HudPosition.BELOW;

    /** Detailed flow and sweep parameters for the selection outline. Not persisted by the config. */
    public static final FlowSettings flow = new FlowSettings();

    @Config.Comment("Enable multisampling where the renderer supports it.")
    public static boolean antiAlias = false;

    @Config.Comment("Link the boxes of doors, beds, double plants and pistons.")
    public static boolean linkBlocks = true;

    @Config.Comment("Draw the selection box through blocks (ignore depth testing).")
    public static boolean showHidden = true;

    @Config.Comment("Animate the selection box when the targeted block changes.")
    public static boolean smoothTransition = true;

    @Config.Comment("Selection transition duration in seconds (0.05 - 0.5).")
    @Config.RangeFloat(min = 0.05F, max = 0.5F)
    public static float transitionDuration = 0.05F;

    @Config.Comment("Draw the inner fill inside the selection box.")
    public static boolean showFill = false;

    @Config.Comment("Log which part the AE cable bus selection picked. Only for troubleshooting.")
    public static boolean debugPartSelection = false;

    @Config.Comment("Log selection target changes (raw hit, lock decision, shapes, animation). "
            + "Only for troubleshooting.")
    public static boolean debugTargetSelection = false;

    @Config.Comment("Animate the inner fill.")
    public static boolean blink = true;

    public static void reset() {
        enabled = true;
        colour = 0xFF236EFF;
        thickness = 1.5F;
        outlineMargin = 0.0F;
        blinkAlpha = 99;
        blinkSpeed = 0.2F;
        rainbow = false;
        rainbowFlow = true;
        rainbowSpeed = 500;
        // 流彩走出厂默认值，选中流光的「重置」时也回到这一套。
        flow.resetToDefault();
        showBreakProgress = false;
        breakProgressStyle = BreakProgressStyle.BAR;
        breakProgressPosition = HudPosition.BELOW;
        antiAlias = false;
        linkBlocks = true;
        showHidden = true;
        smoothTransition = true;
        transitionDuration = 0.05F;
        showFill = false;
        blink = true;
        debugPartSelection = false;
        debugTargetSelection = false;
    }
}
