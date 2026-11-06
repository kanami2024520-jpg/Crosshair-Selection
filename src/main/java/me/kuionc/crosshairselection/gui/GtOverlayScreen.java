package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.gui.widgets.ColorButton;
import me.kuionc.crosshairselection.gui.widgets.ColorCallback;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import me.kuionc.crosshairselection.gui.widgets.SliderHost;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * Settings for the custom GT wrench/cover grid overlay.
 *
 * <p>The pipe outline always follows the selection box colour and width, so this page only
 * configures what is specific to the 3x3 grid and the input marker.
 */
public class GtOverlayScreen extends SettingsScreen {

    private static final int BOX_ENABLED = 1;
    private static final int USE_SELECTION_STYLE = 2;
    private static final int SHOW_GRID = 3;
    private static final int BOX_COLOUR = 4;
    private static final int BOX_THICKNESS = 6;
    private static final int GRID_COLOUR = 5;
    private static final int GRID_THICKNESS = 8;
    private static final int GRID_INSET = 9;
    private static final int SMOOTH_TRANSITION = 11;
    private static final int TRANSITION_DURATION = 12;
    private static final int ALL_FACES = 13;
    private static final int SHOW_INPUT_X = 14;
    private static final int STRICT_GRID = 16;
    private static final int GRID_CELL_SIZE = 17;
    private static final int X_STYLE = 18;
    private static final int SHOW_BLOCK_OUTLINE = 19;

    private static final int SAVE = 20;
    private static final int CANCEL = 21;
    private static final int DEFAULTS = 22;

    private final GuiScreen parent;
    private Snapshot snapshot;
    private boolean childOpen;
    private boolean saved;

    public GtOverlayScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void setWorldAndResolution(net.minecraft.client.Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        childOpen = false;
    }

    @Override
    public void initGui() {
        if (snapshot == null) {
            snapshot = new Snapshot();
        }
        buttonList.clear();
        int scale = FlowLayout.guiScale();
        int colW = FlowLayout.columnWidth(width);
        int rowH = FlowLayout.rowHeightFor(scale);
        int top = FlowLayout.contentTop(height);
        int margin = 4;

        FlowLayout left = new FlowLayout(margin, top, colW, rowH);
        FlowLayout right = new FlowLayout(width - colW - margin, top, colW, rowH);
        int half = (colW - Math.max(2, rowH / 10)) / 2;

        final SettingsScreen.Condition customStyle = new SettingsScreen.Condition() {
            @Override
            public boolean test() {
                return !GtOverlayConfig.useSelectionStyle;
            }
        };
        final SettingsScreen.Condition gridEnabled = gridEnabled();
        final SettingsScreen.Condition gridColourEditable = new SettingsScreen.Condition() {
            @Override
            public boolean test() {
                return GtOverlayConfig.showGrid && !GtOverlayConfig.useSelectionStyle;
            }
        };

        add(left.place(toggle(BOX_ENABLED, 0, 0, "crosshairselection.enabled",
                GtOverlayConfig.enabled), colW));
        add(left.place(toggle(USE_SELECTION_STYLE, 0, 0, "crosshairselection.gt_use_selection_style",
                GtOverlayConfig.useSelectionStyle), colW));
        add(left.place(toggle(SHOW_GRID, 0, 0, half, "crosshairselection.gt_show_grid",
                GtOverlayConfig.showGrid), half));
        add(left.place(toggle(SHOW_BLOCK_OUTLINE, 0, 0, half, "crosshairselection.gt_block_outline",
                GtOverlayConfig.showBlockOutline), half));
        add(left.place(toggle(ALL_FACES, 0, 0, "crosshairselection.gt_all_faces",
                GtOverlayConfig.gridAllFaces), colW), gridEnabled);
        add(left.place(toggle(SHOW_INPUT_X, 0, 0, "crosshairselection.gt_show_input_x",
                GtOverlayConfig.showInputFaceX), colW), gridEnabled);
        left.group();
        add(left.place(new ColorButton(BOX_COLOUR, 0, 0, colW, 18, GtOverlayConfig.colour,
                colourLabel("crosshairselection.colour.gt_box", GtOverlayConfig.colour)), colW), customStyle);
        add(left.place(new ColorButton(GRID_COLOUR, 0, 0, colW, 18, GtOverlayConfig.gridColour,
                colourLabel("crosshairselection.colour.gt_grid_x", GtOverlayConfig.gridColour)), colW),
                gridColourEditable);
        add(left.place(new OptionSlider(GRID_CELL_SIZE, 0, 0, colW, 18, 0.05F, 0.45F,
                GtOverlayConfig.gridCellSize, false, 2, "crosshairselection.gt_grid_cell_size", this), colW),
                gridEnabled);

        add(right.place(new OptionSlider(BOX_THICKNESS, 0, 0, colW, 18, 0.1F, 7.0F,
                GtOverlayConfig.thickness, false, "crosshairselection.gt_box_thickness", this), colW), customStyle);
        add(right.place(new OptionSlider(GRID_THICKNESS, 0, 0, colW, 18, 0.1F, 7.0F,
                GtOverlayConfig.gridThickness, false, "crosshairselection.gt_grid_thickness", this), colW),
                gridEnabled);
        add(right.place(new OptionSlider(GRID_INSET, 0, 0, colW, 18, 0.0F, 0.25F,
                GtOverlayConfig.gridInset, false, 2, "crosshairselection.gt_grid_inset", this), colW),
                gridEnabled);
        add(right.place(new GuiButton(X_STYLE, 0, 0, colW, 18, xStyleLabel()), colW),
                new SettingsScreen.Condition() {
                    @Override
                    public boolean test() {
                        return GtOverlayConfig.showGrid && GtOverlayConfig.showInputFaceX;
                    }
                });
        right.group();
        add(right.place(toggle(STRICT_GRID, 0, 0, colW, "crosshairselection.gt_strict_grid",
                GtOverlayConfig.strictGridClicks), colW), gridEnabled);
        add(right.place(toggle(SMOOTH_TRANSITION, 0, 0, colW, "crosshairselection.smooth_transition",
                GtOverlayConfig.smoothTransition), colW));
        add(right.place(new OptionSlider(TRANSITION_DURATION, 0, 0, colW, 18, 0.05F, 0.5F,
                GtOverlayConfig.transitionDuration, false, 2, "crosshairselection.transition_duration", this),
                colW), new SettingsScreen.Condition() {
                    @Override
                    public boolean test() {
                        return GtOverlayConfig.smoothTransition;
                    }
                });

        int footerY = FlowLayout.footerTop(height);
        left.fitTo(FlowLayout.contentTop(height), footerY);
        right.fitTo(FlowLayout.contentTop(height), footerY);
        add(new GuiButton(SAVE, width / 2 - 145, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.save")));
        add(new GuiButton(CANCEL, width / 2 - 45, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.back")));
        add(new GuiButton(DEFAULTS, width / 2 + 55, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.defaults")));

        refreshEnabledStates();
    }

    private static SettingsScreen.Condition gridEnabled() {
        return new SettingsScreen.Condition() {
            @Override
            public boolean test() {
                return GtOverlayConfig.showGrid;
            }
        };
    }

    private GuiButton toggle(int id, int x, int y, String key, boolean value) {
        return toggle(id, x, y, 150, key, value);
    }

    private GuiButton toggle(int id, int x, int y, int width, String key, boolean value) {
        return new GuiButton(id, x, y, width, 18,
                StatCollector.translateToLocalFormatted(key, onOff(value)));
    }

    private static String xStyleLabel() {
        return StatCollector.translateToLocalFormatted("crosshairselection.gt_x_style",
                StatCollector.translateToLocal(GtOverlayConfig.xStyle.getTranslationKey()));
    }

    private static String onOff(boolean value) {
        return StatCollector.translateToLocal(value ? "crosshairselection.on" : "crosshairselection.off");
    }

    private static String colourLabel(String key, int colour) {
        return StatCollector.translateToLocalFormatted(key, ColorUtil.hex(colour));
    }

    @Override
    protected void onButtonPressed(GuiButton button) {
        switch (button.id) {
            case BOX_ENABLED:
                GtOverlayConfig.enabled = !GtOverlayConfig.enabled;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.enabled",
                        onOff(GtOverlayConfig.enabled));
                break;
            case USE_SELECTION_STYLE:
                GtOverlayConfig.useSelectionStyle = !GtOverlayConfig.useSelectionStyle;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.gt_use_selection_style", onOff(GtOverlayConfig.useSelectionStyle));
                break;
            case SHOW_GRID:
                GtOverlayConfig.showGrid = !GtOverlayConfig.showGrid;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.gt_show_grid", onOff(GtOverlayConfig.showGrid));
                break;
            case SHOW_BLOCK_OUTLINE:
                GtOverlayConfig.showBlockOutline = !GtOverlayConfig.showBlockOutline;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.gt_block_outline", onOff(GtOverlayConfig.showBlockOutline));
                break;
            case SMOOTH_TRANSITION:
                GtOverlayConfig.smoothTransition = !GtOverlayConfig.smoothTransition;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.smooth_transition", onOff(GtOverlayConfig.smoothTransition));
                break;
            case ALL_FACES:
                GtOverlayConfig.gridAllFaces = !GtOverlayConfig.gridAllFaces;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.gt_all_faces", onOff(GtOverlayConfig.gridAllFaces));
                break;
            case SHOW_INPUT_X:
                GtOverlayConfig.showInputFaceX = !GtOverlayConfig.showInputFaceX;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.gt_show_input_x", onOff(GtOverlayConfig.showInputFaceX));
                break;
            case X_STYLE:
                GtOverlayConfig.xStyle = GtOverlayConfig.xStyle.next();
                button.displayString = xStyleLabel();
                break;
            case STRICT_GRID:
                GtOverlayConfig.strictGridClicks = !GtOverlayConfig.strictGridClicks;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.gt_strict_grid", onOff(GtOverlayConfig.strictGridClicks));
                break;
            case BOX_COLOUR:
                childOpen = true;
                mc.displayGuiScreen(new ColorPickerScreen(this,
                        "crosshairselection.colour.title.gt_box", GtOverlayConfig.colour,
                        new ColorCallback() {
                            @Override
                            public void accept(int argb) {
                                GtOverlayConfig.colour = argb;
                                refreshColourButton(BOX_COLOUR, argb);
                            }
                        }));
                break;
            case GRID_COLOUR:
                childOpen = true;
                mc.displayGuiScreen(new ColorPickerScreen(this,
                        "crosshairselection.colour.title.gt_grid_x", GtOverlayConfig.gridColour,
                        new ColorCallback() {
                            @Override
                            public void accept(int argb) {
                                GtOverlayConfig.gridColour = argb;
                                refreshColourButton(GRID_COLOUR, argb);
                            }
                        }));
                break;
            case SAVE:
                ConfigurationManager.save(GtOverlayConfig.class);
                saved = true;
                snapshot = new Snapshot();
                mc.displayGuiScreen(parent);
                break;
            case CANCEL:
                saved = true;
                snapshot.restore();
                mc.displayGuiScreen(parent);
                break;
            case DEFAULTS:
                mc.displayGuiScreen(new ConfirmScreen(this,
                        StatCollector.translateToLocal("crosshairselection.defaults.confirm.gt"),
                        new Runnable() {
                            @Override
                            public void run() {
                                GtOverlayConfig.reset();
                                mc.displayGuiScreen(new GtOverlayScreen(parent));
                            }
                        }));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSliderValue(int optionId, float value) {
        switch (optionId) {
            case BOX_THICKNESS:
                GtOverlayConfig.thickness = value;
                break;
            case GRID_THICKNESS:
                GtOverlayConfig.gridThickness = value;
                break;
            case GRID_INSET:
                GtOverlayConfig.gridInset = value;
                break;
            case GRID_CELL_SIZE:
                GtOverlayConfig.gridCellSize = value;
                break;
            case TRANSITION_DURATION:
                GtOverlayConfig.transitionDuration = value;
                break;
            default:
                break;
        }
    }

    private void refreshColourButton(int id, int colour) {
        for (Object object : buttonList) {
            if (object instanceof ColorButton && ((ColorButton) object).id == id) {
                ColorButton button = (ColorButton) object;
                button.setColour(colour);
                button.displayString = colourLabel(id == BOX_COLOUR
                        ? "crosshairselection.colour.gt_box"
                        : "crosshairselection.colour.gt_grid_x", colour);
                return;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        PreviewPanel.begin(PreviewPanel.Content.FACE,
                me.kuionc.crosshairselection.config.SelectionBoxConfig.flow);
        PreviewPanel.drawCentred(width, height);
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.gt_overlay_settings"),
                width / 2, 6, 0xFFFFFF);
        // Explains the click switch right where it is, instead of leaving it to the config file.
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.gt_strict_grid.hint"),
                width / 2, height - 44, 0xA0A0A0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        if (childOpen) {
            childOpen = false;
            return;
        }
        if (!saved && snapshot != null) {
            snapshot.restore();
        }
    }

    private static final class Snapshot {
        private final boolean enabled;
        private final boolean useSelectionStyle;
        private final int colour;
        private final float thickness;
        private final boolean showGrid;
        private final boolean showBlockOutline;
        private final int gridColour;
        private final float gridThickness;
        private final float gridInset;
        private final float gridCellSize;
        private final boolean gridAllFaces;
        private final boolean showInputFaceX;
        private final Object xStyle;
        private final boolean strictGridClicks;
        private final boolean smoothTransition;
        private final float transitionDuration;

        Snapshot() {
            enabled = GtOverlayConfig.enabled;
            useSelectionStyle = GtOverlayConfig.useSelectionStyle;
            colour = GtOverlayConfig.colour;
            thickness = GtOverlayConfig.thickness;
            showGrid = GtOverlayConfig.showGrid;
            showBlockOutline = GtOverlayConfig.showBlockOutline;
            gridColour = GtOverlayConfig.gridColour;
            gridThickness = GtOverlayConfig.gridThickness;
            gridInset = GtOverlayConfig.gridInset;
            gridCellSize = GtOverlayConfig.gridCellSize;
            gridAllFaces = GtOverlayConfig.gridAllFaces;
            showInputFaceX = GtOverlayConfig.showInputFaceX;
            xStyle = GtOverlayConfig.xStyle;
            strictGridClicks = GtOverlayConfig.strictGridClicks;
            smoothTransition = GtOverlayConfig.smoothTransition;
            transitionDuration = GtOverlayConfig.transitionDuration;
        }

        void restore() {
            GtOverlayConfig.enabled = enabled;
            GtOverlayConfig.useSelectionStyle = useSelectionStyle;
            GtOverlayConfig.colour = colour;
            GtOverlayConfig.thickness = thickness;
            GtOverlayConfig.showGrid = showGrid;
            GtOverlayConfig.showBlockOutline = showBlockOutline;
            GtOverlayConfig.gridColour = gridColour;
            GtOverlayConfig.gridThickness = gridThickness;
            GtOverlayConfig.gridInset = gridInset;
            GtOverlayConfig.gridCellSize = gridCellSize;
            GtOverlayConfig.gridAllFaces = gridAllFaces;
            GtOverlayConfig.showInputFaceX = showInputFaceX;
            GtOverlayConfig.xStyle = (me.kuionc.crosshairselection.config.GtXStyle) xStyle;
            GtOverlayConfig.strictGridClicks = strictGridClicks;
            GtOverlayConfig.smoothTransition = smoothTransition;
            GtOverlayConfig.transitionDuration = transitionDuration;
        }
    }
}
