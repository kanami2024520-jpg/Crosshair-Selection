package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * Editor for the flowing colour parameters.
 *
 * <p>The selection box and the crosshair keep separate settings, so the tab at the top decides
 * which {@link FlowSettings} instance the sliders edit. Values apply immediately, which keeps the
 * live preview behind the screen up to date.
 */
public class FlowScreen extends SettingsScreen {

    private static final int TAB_SELECTION = 1;
    private static final int TAB_CROSSHAIR = 2;
    private static final int BACK = 3;

    private static final int MODE = 10;
    private static final int HUE_START = 11;
    private static final int HUE_RANGE = 12;
    private static final int SATURATION = 13;

    private static final int SPEED = 15;
    private static final int DIRECTION = 16;
    private static final int MOTION = 17;

    private static final int PROGRESS = 26;
    private static final int SWEEP_SOFTNESS = 27;
    private static final int PULSE_STRENGTH = 30;
    private static final int PULSE_THICKNESS = 31;
    private static final int PULSE_WIDTH = 32;
    private static final int COLOUR_EDITOR = 40;
    private static final int START_CORNER = 41;
    private static final int END_CORNER = 42;

    private static final int GROUP_Y = 46;

    private final GuiScreen parent;
    private int tab;

    public FlowScreen(GuiScreen parent, boolean crosshair) {
        this.parent = parent;
        this.tab = crosshair ? TAB_CROSSHAIR : TAB_SELECTION;
    }

    private FlowSettings settings() {
        return tab == TAB_CROSSHAIR ? CrosshairConfig.flow : SelectionBoxConfig.flow;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        FlowSettings flow = settings();
        int tabWidth = Math.min(120, Math.max(80, (width - 8) / 2));
        int tabStart = width / 2 - tabWidth;
        add(modeButton(TAB_SELECTION, tabStart, 20, tabWidth,
                "crosshairselection.tab.selection", tab == TAB_SELECTION));
        add(modeButton(TAB_CROSSHAIR, tabStart + tabWidth, 20, tabWidth,
                "crosshairselection.tab.crosshair", tab == TAB_CROSSHAIR));

        int left = 4;
        int right = width - 154;
        int y = GROUP_Y;
        int row = SettingsLayout.ROW_STEP;

        // Left column: what the gradient is and which way it travels.
        add(new GuiButton(MODE, left, y, SettingsLayout.COLUMN_WIDTH, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.flow_mode",
                        StatCollector.translateToLocal(modeKey(flow.mode)))));
        add(new GuiButton(COLOUR_EDITOR, left, y + row, SettingsLayout.COLUMN_WIDTH, 18,
                StatCollector.translateToLocal("crosshairselection.flow_colour")));
        add(new GuiButton(DIRECTION, left, y + row * 2, SettingsLayout.COLUMN_WIDTH, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.flow_direction",
                        StatCollector.translateToLocal(directionKey(flow.direction)))));

        // Right column: timing, motion curve and colour layout.
        add(new OptionSlider(SPEED, right, y, SettingsLayout.COLUMN_WIDTH, 18, 0.0F, 2000.0F,
                flow.speed, true, "crosshairselection.flow_speed", this));
        add(new GuiButton(MOTION, right, y + row, SettingsLayout.COLUMN_WIDTH, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.flow_motion",
                        StatCollector.translateToLocal(motionKey(flow.motion)))));
        // 亮度 / 透明度 / 色带密度都在「颜色」子页面里，和色盘对齐。

        if (tab == TAB_SELECTION) {
            int groupY = y + row * 4;
            add(new GuiButton(START_CORNER, left, groupY, 73, 18,
                    StatCollector.translateToLocalFormatted("crosshairselection.flow_corner_start",
                            flow.startCorner + 1)));
            add(new GuiButton(END_CORNER, left + 77, groupY, 73, 18,
                    StatCollector.translateToLocalFormatted("crosshairselection.flow_corner_end",
                            flow.endCorner + 1)));

            // Sweep settings only make sense in sweep mode; they grey out otherwise.
            add(new OptionSlider(PROGRESS, left, groupY + row, SettingsLayout.COLUMN_WIDTH, 18, 0.0F, 1.0F,
                    flow.progress, false, 2, "crosshairselection.flow_progress", this));
            add(new OptionSlider(SWEEP_SOFTNESS, left, groupY + row * 2, SettingsLayout.COLUMN_WIDTH, 18,
                    0.01F, 1.0F, flow.sweepSoftness, false, 2,
                    "crosshairselection.flow_sweep_softness", this));

        } else if (flow.mode == FlowSettings.Mode.SWEEP) {
            // Older saves may still hold sweep/glint values for the crosshair; fall back to flow.
            flow.mode = FlowSettings.Mode.FLOW;
        }

        add(new GuiButton(BACK, width / 2 - 75, height - 26, 150, 18,
                StatCollector.translateToLocal("crosshairselection.back")));
        applyDependencies(flow);
        refreshEnabledStates();
    }

    /** Greys out parameters that the selected mode does not use. */
    private void applyDependencies(FlowSettings flow) {
        for (GuiButton button : buttonList) {
            int id = button.id;
            if (id < MODE || id == BACK) {
                continue;
            }
            boolean common = id == MODE;
            boolean cornerOnly = id == START_CORNER || id == END_CORNER;
            boolean sweepOnly = id == PROGRESS || id == SWEEP_SOFTNESS;
            boolean active = flow.mode != FlowSettings.Mode.OFF;
            if (cornerOnly) {
                active = flow.direction == FlowSettings.Direction.CORNER_TO_CORNER;
            } else if (sweepOnly) {
                active = flow.mode == FlowSettings.Mode.SWEEP;
            }
            button.enabled = common || active;
        }
    }

    private GuiButton modeButton(int id, int x, int y, int width, String key, boolean selected) {
        GuiButton button = new GuiButton(id, x, y, width, 18, StatCollector.translateToLocal(key));
        button.enabled = !selected;
        return button;
    }

    private static String modeKey(FlowSettings.Mode mode) {
        switch (mode) {
            case OFF:
                return "crosshairselection.flow.off";
            case SWEEP:
                return "crosshairselection.flow.sweep";

            case FLOW:
            default:
                return "crosshairselection.flow.flow";
        }
    }

    /** The crosshair skips corner-to-corner because it has no cube to travel around. */
    private static FlowSettings.Direction nextDirection(FlowSettings.Direction current,
                                                        boolean crosshair) {
        if (crosshair) {
            switch (current) {
                case FORWARD:
                    return FlowSettings.Direction.REVERSE;
                case REVERSE:
                    return FlowSettings.Direction.CENTER_OUT;
                case CENTER_OUT:
                default:
                    return FlowSettings.Direction.FORWARD;
            }
        }
        return FlowSettings.cycle(FlowSettings.Direction.values(), current);
    }

    private static String directionKey(FlowSettings.Direction direction) {
        switch (direction) {
            case REVERSE:
                return "crosshairselection.flow_direction.reverse";
            case CORNER_TO_CORNER:
                return "crosshairselection.flow_direction.corner";
            case CENTER_OUT:
                return "crosshairselection.flow_direction.centre";
            case FORWARD:
            default:
                return "crosshairselection.flow_direction.forward";
        }
    }

    private static String motionKey(FlowSettings.Motion motion) {
        switch (motion) {
            case COSINE:
                return "crosshairselection.flow.motion.cosine";
            case TRIANGLE:
                return "crosshairselection.flow.motion.triangle";
            case NOISE:
                return "crosshairselection.flow.motion.noise";
            case LINEAR:
            default:
                return "crosshairselection.flow.motion.linear";
        }
    }

    @Override
    protected void onButtonPressed(GuiButton button) {
        FlowSettings flow = settings();
        switch (button.id) {
            case TAB_SELECTION:
                tab = TAB_SELECTION;
                initGui();
                break;
            case TAB_CROSSHAIR:
                tab = TAB_CROSSHAIR;
                initGui();
                break;
            case BACK:
                mc.displayGuiScreen(parent);
                break;
            case COLOUR_EDITOR:
                mc.displayGuiScreen(new FlowColorScreen(this, settings(), tab == TAB_CROSSHAIR));
                break;
            case MODE:
                if (tab == TAB_CROSSHAIR) {
                    flow.mode = flow.mode == FlowSettings.Mode.FLOW
                            ? FlowSettings.Mode.OFF : FlowSettings.Mode.FLOW;
                } else {
                    flow.mode = FlowSettings.cycle(FlowSettings.Mode.values(), flow.mode);
                }
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.flow_mode", StatCollector.translateToLocal(modeKey(flow.mode)));
                applyDependencies(flow);
                break;
            case DIRECTION:
                flow.direction = nextDirection(flow.direction, tab == TAB_CROSSHAIR);
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.flow_direction",
                        StatCollector.translateToLocal(directionKey(flow.direction)));
                applyDependencies(flow);
                break;
            case START_CORNER:
                flow.startCorner = (flow.startCorner + 1) % 8;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.flow_corner_start", flow.startCorner + 1);
                break;
            case END_CORNER:
                flow.endCorner = (flow.endCorner + 1) % 8;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.flow_corner_end", flow.endCorner + 1);
                break;
            case MOTION:
                flow.motion = FlowSettings.cycle(FlowSettings.Motion.values(), flow.motion);
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.flow_motion", StatCollector.translateToLocal(motionKey(flow.motion)));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSliderValue(int optionId, float value) {
        FlowSettings flow = settings();
        switch (optionId) {
            case SPEED:
                flow.speed = Math.round(value);
                break;
            case SWEEP_SOFTNESS:
                flow.sweepSoftness = value;
                break;
            case PROGRESS:
                flow.progress = value;
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("crosshairselection.flow.title"),
                width / 2, 6, 0xFFFFFF);
        drawLivePreview();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** Shared preview panel, centred on screen and kept in the same place on every page. */
    private void drawLivePreview() {
        PreviewPanel.begin(tab == TAB_CROSSHAIR
                ? PreviewPanel.Content.CROSSHAIR : PreviewPanel.Content.CUBE, settings());
        PreviewPanel.drawCentred(width, height);
    }
}
