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
 */
public class GtOverlayScreen extends GuiScreen implements SliderHost {

    private static final int BOX_ENABLED = 1;
    private static final int USE_SELECTION_STYLE = 2;
    private static final int SHOW_GRID = 3;
    private static final int BOX_COLOUR = 4;
    private static final int GRID_COLOUR = 5;
    private static final int BOX_THICKNESS = 6;
    private static final int BOX_MARGIN = 7;
    private static final int GRID_THICKNESS = 8;
    private static final int GRID_INSET = 9;
    private static final int SMOOTH_TRANSITION = 11;
    private static final int TRANSITION_DURATION = 12;
    private static final int ALL_FACES = 13;
    private static final int SHOW_INPUT_X = 14;
    private static final int STRICT_GRID = 16;
    private static final int GRID_CELL_SIZE = 17;
    private static final int X_STYLE = 18;

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
        int left = 4;
        int right = width - 154;
        int y = 46;
        buttonList.add(toggle(BOX_ENABLED, left, y, "crosshairselection.enabled", GtOverlayConfig.enabled));
        buttonList.add(toggle(USE_SELECTION_STYLE, left, y + 20, "crosshairselection.gt_use_selection_style",
                GtOverlayConfig.useSelectionStyle));
        buttonList.add(toggle(SHOW_GRID, left, y + 40, "crosshairselection.gt_show_grid", GtOverlayConfig.showGrid));
        buttonList.add(toggle(ALL_FACES, left, y + 60, "crosshairselection.gt_all_faces", GtOverlayConfig.gridAllFaces));
        buttonList.add(toggle(SHOW_INPUT_X, left, y + 80, "crosshairselection.gt_show_input_x",
                GtOverlayConfig.showInputFaceX));
        ColorButton boxColourButton = new ColorButton(BOX_COLOUR, left, y + 100, 150, 18,
                GtOverlayConfig.colour, colourLabel("crosshairselection.colour.gt_box", GtOverlayConfig.colour));
        ColorButton gridColourButton = new ColorButton(GRID_COLOUR, left, y + 120, 150, 18,
                GtOverlayConfig.gridColour, colourLabel("crosshairselection.colour.gt_grid_x", GtOverlayConfig.gridColour));
        boxColourButton.visible = !GtOverlayConfig.useSelectionStyle;
        boxColourButton.enabled = !GtOverlayConfig.useSelectionStyle;
        gridColourButton.visible = !GtOverlayConfig.useSelectionStyle;
        gridColourButton.enabled = !GtOverlayConfig.useSelectionStyle;
        buttonList.add(boxColourButton);
        buttonList.add(gridColourButton);
        buttonList.add(new OptionSlider(GRID_CELL_SIZE, left, y + 140, 150, 18, 0.05F, 0.45F,
                GtOverlayConfig.gridCellSize, false, 2, "crosshairselection.gt_grid_cell_size", this));


        buttonList.add(new OptionSlider(BOX_THICKNESS, right, y, 150, 18, 0.1F, 7.0F,
                GtOverlayConfig.thickness, false, "crosshairselection.thickness", this));
        buttonList.add(new OptionSlider(BOX_MARGIN, right, y + 20, 150, 18, 0.0F, 0.02F,
                GtOverlayConfig.margin, false, 3, "crosshairselection.outline_margin", this));
        buttonList.add(new OptionSlider(GRID_THICKNESS, right, y + 40, 150, 18, 0.1F, 7.0F,
                GtOverlayConfig.gridThickness, false, "crosshairselection.gt_grid_thickness", this));
        buttonList.add(new OptionSlider(GRID_INSET, right, y + 60, 150, 18, 0.0F, 0.25F,
                GtOverlayConfig.gridInset, false, 2, "crosshairselection.gt_grid_inset", this));
        buttonList.add(new GuiButton(X_STYLE, right, y + 80, 150, 18, xStyleLabel()));
        buttonList.add(toggle(STRICT_GRID, right, y + 100, "crosshairselection.gt_strict_grid",
                GtOverlayConfig.strictGridClicks));
        buttonList.add(toggle(SMOOTH_TRANSITION, right, y + 120, "crosshairselection.smooth_transition",
                GtOverlayConfig.smoothTransition));
        buttonList.add(new OptionSlider(TRANSITION_DURATION, right, y + 140, 150, 18, 0.05F, 0.5F,
                GtOverlayConfig.transitionDuration, false, 2, "crosshairselection.transition_duration", this));

        buttonList.add(new GuiButton(SAVE, width / 2 - 145, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.save")));
        buttonList.add(new GuiButton(CANCEL, width / 2 - 45, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.cancel")));
        buttonList.add(new GuiButton(DEFAULTS, width / 2 + 55, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.defaults")));
    }

    private GuiButton toggle(int id, int x, int y, String key, boolean value) {
        return new GuiButton(id, x, y, 150, 18,
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

    private static String colourLabelKey(int id) {
        return id == GRID_COLOUR
                ? "crosshairselection.colour.gt_grid_x"
                : "crosshairselection.colour.gt_box";
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BOX_ENABLED:
                GtOverlayConfig.enabled = !GtOverlayConfig.enabled;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.enabled",
                        onOff(GtOverlayConfig.enabled));
                break;
            case USE_SELECTION_STYLE:
                GtOverlayConfig.useSelectionStyle = !GtOverlayConfig.useSelectionStyle;
                initGui();
                break;
            case SHOW_GRID:
                GtOverlayConfig.showGrid = !GtOverlayConfig.showGrid;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.gt_show_grid", onOff(GtOverlayConfig.showGrid));
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
                openColourPicker("crosshairselection.colour.title.gt_box", GtOverlayConfig.colour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        GtOverlayConfig.colour = argb;
                        refreshColourButton(BOX_COLOUR, argb);
                    }
                });
                break;
            case GRID_COLOUR:
                openColourPicker("crosshairselection.colour.title.gt_grid_x", GtOverlayConfig.gridColour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        GtOverlayConfig.gridColour = argb;
                        refreshColourButton(GRID_COLOUR, argb);
                    }
                });
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
                GtOverlayConfig.reset();
                initGui();
                break;
            default:
                break;
        }
    }

    @Override
    public void onSliderChanged(int optionId, float value) {
        switch (optionId) {
            case BOX_THICKNESS:
                GtOverlayConfig.thickness = value;
                break;
            case BOX_MARGIN:
                GtOverlayConfig.margin = Math.round(value * 1000.0F) / 1000.0F;
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

    private void openColourPicker(String titleKey, int initial, ColorCallback callback) {
        childOpen = true;
        mc.displayGuiScreen(new ColorPickerScreen(this, titleKey, initial, callback));
    }

    private void refreshColourButton(int id, int colour) {
        for (Object object : buttonList) {
            if (object instanceof ColorButton && ((ColorButton) object).id == id) {
                ColorButton button = (ColorButton) object;
                button.setColour(colour);
                button.displayString = colourLabel(colourLabelKey(id), colour);
                return;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.gt_overlay_settings"),
                width / 2, 6, 0xFFFFFF);
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
        private final boolean showGrid;
        private final int colour;
        private final int gridColour;
        private final float thickness;
        private final float margin;
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
            showGrid = GtOverlayConfig.showGrid;
            colour = GtOverlayConfig.colour;
            gridColour = GtOverlayConfig.gridColour;
            thickness = GtOverlayConfig.thickness;
            margin = GtOverlayConfig.margin;
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
            GtOverlayConfig.showGrid = showGrid;
            GtOverlayConfig.colour = colour;
            GtOverlayConfig.gridColour = gridColour;
            GtOverlayConfig.thickness = thickness;
            GtOverlayConfig.margin = margin;
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
