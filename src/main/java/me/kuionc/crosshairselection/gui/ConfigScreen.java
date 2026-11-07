package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.gui.widgets.ColorButton;
import me.kuionc.crosshairselection.gui.widgets.ColorCallback;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import me.kuionc.crosshairselection.render.CrosshairType;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * Unified settings screen: selection box and crosshair.
 */
public class ConfigScreen extends SettingsScreen {

    private static final int TAB_SELECTION = 100;
    private static final int TAB_CROSSHAIR = 101;
    private static final int SAVE = 200;
    private static final int CANCEL = 201;
    private static final int DEFAULTS = 202;

    private static final int SELECTION_ENABLED = 1;
    private static final int SELECTION_COLOUR = 2;
    private static final int SELECTION_THICKNESS = 3;
    private static final int SELECTION_RAINBOW = 6;
    private static final int SELECTION_LINK = 7;
    private static final int SELECTION_HIDDEN = 8;
    private static final int SELECTION_MARGIN = 19;
    private static final int SELECTION_RAINBOW_SPEED = 11;
    private static final int SELECTION_SMOOTH = 12;
    private static final int SELECTION_TRANSITION = 13;
    private static final int SELECTION_GT_SETTINGS = 14;
    private static final int SELECTION_TEXTURE_SIZE = 15;
    private static final int SELECTION_ANTI_ALIAS = 40;
    private static final int SELECTION_FILL_SETTINGS = 53;
    private static final int CROSSHAIR_VISIBILITY_SETTINGS = 54;
    private static final int CROSSHAIR_EXTRA_SETTINGS = 55;
    private static final int SELECTION_BREAK_PROGRESS = 42;
    private static final int SELECTION_BREAK_STYLE = 47;
    private static final int SELECTION_BREAK_POSITION = 56;


    private static final int CROSSHAIR_ENABLED = 20;
    private static final int CROSSHAIR_TYPE = 21;
    private static final int CROSSHAIR_COLOUR = 22;
    private static final int CROSSHAIR_WIDTH = 23;
    private static final int CROSSHAIR_HEIGHT = 24;
    private static final int CROSSHAIR_GAP = 25;
    private static final int CROSSHAIR_THICKNESS = 26;
    private static final int CROSSHAIR_ROTATION = 27;
    private static final int CROSSHAIR_OUTLINE_THICKNESS = 38;
    private static final int CROSSHAIR_OUTLINE = 28;
    private static final int CROSSHAIR_OUTLINE_COLOUR = 29;
    private static final int CROSSHAIR_DOT = 30;
    private static final int CROSSHAIR_DOT_COLOUR = 31;
    private static final int CROSSHAIR_RAINBOW = 32;
    private static final int CROSSHAIR_RAINBOW_SPEED = 33;
    private static final int CROSSHAIR_VISIBLE = 34;
    private static final int CROSSHAIR_TIC_CROSSHAIR = 39;
    private static final int CROSSHAIR_ANTI_ALIAS = 41;
    private static final int CROSSHAIR_ATTACK_FEEDBACK = 43;
    private static final int CROSSHAIR_PLACE_FEEDBACK = 44;
    private static final int CROSSHAIR_FEEDBACK_STRENGTH = 45;
    private static final int CROSSHAIR_FEEDBACK_STYLE = 46;
    private static final int CROSSHAIR_EXPAND_SPEED = 48;
    private static final int CROSSHAIR_RETURN_SPEED = 49;
    private static final int CROSSHAIR_BOW_FEEDBACK = 50;
    private static final int CROSSHAIR_BOW_INDICATOR = 51;

    private final GuiScreen parent;
    private Snapshot snapshot;
    private int tab;
    private boolean saved;
    private boolean childOpen;

    public ConfigScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void setWorldAndResolution(net.minecraft.client.Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        // Returning from a child screen re-displays this instance; it is no longer "leaving" to
        // a colour picker or sub-page, so Cancel must be allowed to restore the snapshot again.
        childOpen = false;
    }

    @Override
    public void initGui() {
        if (snapshot == null) {
            snapshot = new Snapshot();
        }
        buttonList.clear();

        int tabGap = 2;
        int tabWidth = Math.min(120, Math.max(80, (width - 8) / 2));
        int tabStart = width / 2 - (tabWidth * 2 + tabGap) / 2;
        int tabY = Math.max(16, Math.round(height * 0.028F));
        int tabH = FlowLayout.tabHeight(height);
        int footerY = FlowLayout.footerTop(height);
        buttonList.add(new GuiButton(TAB_SELECTION, tabStart, tabY, tabWidth, tabH,
                StatCollector.translateToLocal("crosshairselection.tab.selection")));
        buttonList.add(new GuiButton(TAB_CROSSHAIR, tabStart + tabWidth + tabGap, tabY, tabWidth, tabH,
                StatCollector.translateToLocal("crosshairselection.tab.crosshair")));

        buttonList.add(new GuiButton(SAVE, width / 2 - 145, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.save")));
        buttonList.add(new GuiButton(CANCEL, width / 2 - 45, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.back")));
        buttonList.add(new GuiButton(DEFAULTS, width / 2 + 55, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.defaults")));

        if (tab == 0) {
            initSelectionButtons();
        } else {
            initCrosshairButtons();
        }
    }

    private void initSelectionButtons() {
        int scale = FlowLayout.guiScale();
        int colW = FlowLayout.columnWidth(width);
        int rowH = FlowLayout.rowHeightFor(scale);
        int top = FlowLayout.contentTop(height);
        int margin = 4;
        boolean single = FlowLayout.singleColumn(width, colW);

        FlowLayout left = new FlowLayout(margin, top, colW, rowH);
        FlowLayout right = new FlowLayout(width - colW - margin, top, colW, rowH);

        Condition selectionOn = new Condition() {
            @Override
            public boolean test() {
                return SelectionBoxConfig.enabled;
            }
        };
        Condition flowOn = and(selectionOn, new Condition() {
            @Override
            public boolean test() {
                return SelectionBoxConfig.flow.mode != FlowSettings.Mode.OFF;
            }
        });

        int half = (colW - Math.max(2, rowH / 10)) / 2;

        // 左列：外观与流动
        add(left.place(toggle(SELECTION_ENABLED, 0, 0, "crosshairselection.enabled",
                SelectionBoxConfig.enabled), colW));
        add(left.place(new ColorButton(SELECTION_COLOUR, 0, 0, colW, 18, SelectionBoxConfig.colour,
                colourLabel("crosshairselection.colour.selection_box", SelectionBoxConfig.colour)), colW),
                selectionOn);
        add(left.place(new OptionSlider(SELECTION_THICKNESS, 0, 0, colW, 18, 0.1F, 7.0F,
                SelectionBoxConfig.thickness, false, "crosshairselection.thickness", this), colW),
                selectionOn);
        left.group();
        add(left.place(new GuiButton(SELECTION_FILL_SETTINGS, 0, 0, colW, 18,
                StatCollector.translateToLocal("crosshairselection.fill_settings")), colW), selectionOn);
        add(left.place(flowButton(SELECTION_RAINBOW, 0, 0, colW, SelectionBoxConfig.flow), colW));
        add(left.place(new OptionSlider(SELECTION_RAINBOW_SPEED, 0, 0, colW, 18, 0, 2000,
                SelectionBoxConfig.flow.speed, true, "crosshairselection.flow_speed", this), colW), flowOn);
        left.group();
        add(left.place(new GuiButton(SELECTION_GT_SETTINGS, 0, 0, colW, 18,
                StatCollector.translateToLocal("crosshairselection.gt_overlay_settings")), colW));

        // 右列：行为
        add(right.place(toggle(SELECTION_LINK, 0, 0, "crosshairselection.link_blocks",
                SelectionBoxConfig.linkBlocks), colW), selectionOn);
        add(right.place(toggle(SELECTION_HIDDEN, 0, 0, "crosshairselection.see_through",
                SelectionBoxConfig.showHidden), colW), selectionOn);
        right.group();
        add(right.place(toggle(SELECTION_SMOOTH, 0, 0, "crosshairselection.smooth_transition",
                SelectionBoxConfig.smoothTransition), colW), selectionOn);
        add(right.place(new OptionSlider(SELECTION_TRANSITION, 0, 0, colW, 18, 0.05F, 0.5F,
                SelectionBoxConfig.transitionDuration, false, 2, "crosshairselection.transition_duration", this),
                colW), and(selectionOn, new Condition() {
                    @Override
                    public boolean test() {
                        return SelectionBoxConfig.smoothTransition;
                    }
                }));
        right.group();
        add(right.place(new OptionSlider(SELECTION_MARGIN, 0, 0, colW, 18, 0.0F, 0.25F,
                SelectionBoxConfig.outlineMargin, false, 3, 0.005F,
                "crosshairselection.outline_margin", this), colW), selectionOn);
        // 破坏进度整组：开关、样式、位置各占一整行，文字不会被截断。
        add(right.place(toggle(SELECTION_BREAK_PROGRESS, 0, 0, colW, "crosshairselection.break_progress",
                SelectionBoxConfig.showBreakProgress), colW), selectionOn);
        add(right.place(new GuiButton(SELECTION_BREAK_STYLE, 0, 0, colW, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.break_style",
                        StatCollector.translateToLocal(SelectionBoxConfig.breakProgressStyle.getTranslationKey()))),
                colW), new Condition() {
                    @Override
                    public boolean test() {
                        return SelectionBoxConfig.enabled && SelectionBoxConfig.showBreakProgress;
                    }
                });
        add(right.place(new GuiButton(SELECTION_BREAK_POSITION, 0, 0, colW, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.hud_position",
                        StatCollector.translateToLocal(
                                SelectionBoxConfig.breakProgressPosition.getTranslationKey()))), colW),
                new Condition() {
                    @Override
                    public boolean test() {
                        return SelectionBoxConfig.enabled && SelectionBoxConfig.showBreakProgress;
                    }
                });

        int footer = FlowLayout.footerTop(height);
        left.fitTo(FlowLayout.contentTop(height), footer);
        right.fitTo(FlowLayout.contentTop(height), footer);
        refreshEnabledStates();
    }

    private void initCrosshairButtons() {
        int scale = FlowLayout.guiScale();
        int colW = FlowLayout.columnWidth(width);
        int rowH = FlowLayout.rowHeightFor(scale);
        int top = FlowLayout.contentTop(height);
        int margin = 4;

        FlowLayout left = new FlowLayout(margin, top, colW, rowH);
        FlowLayout right = new FlowLayout(width - colW - margin, top, colW, rowH);

        Condition crosshairOn = new Condition() {
            @Override
            public boolean test() {
                return CrosshairConfig.enabled && CrosshairConfig.type != CrosshairType.DEFAULT;
            }
        };
        Condition outlineOn = and(crosshairOn, new Condition() {
            @Override
            public boolean test() {
                return CrosshairConfig.outline;
            }
        });
        Condition dotOn = and(crosshairOn, new Condition() {
            @Override
            public boolean test() {
                return CrosshairConfig.dot;
            }
        });
        Condition flowOn = and(crosshairOn, new Condition() {
            @Override
            public boolean test() {
                return CrosshairConfig.flow.mode != FlowSettings.Mode.OFF;
            }
        });

        int half = (colW - Math.max(2, rowH / 10)) / 2;

        // 左列：形状
        add(left.place(toggle(CROSSHAIR_ENABLED, 0, 0, "crosshairselection.enabled",
                CrosshairConfig.enabled), colW));
        add(left.place(new GuiButton(CROSSHAIR_TYPE, 0, 0, colW, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.type",
                        StatCollector.translateToLocal(CrosshairConfig.type.getTranslationKey()))), colW),
                new Condition() {
                    @Override
                    public boolean test() {
                        return CrosshairConfig.enabled;
                    }
                });
        add(left.place(new ColorButton(CROSSHAIR_COLOUR, 0, 0, colW, 18, CrosshairConfig.colour,
                colourLabel("crosshairselection.colour.crosshair", CrosshairConfig.colour)), colW), crosshairOn);
        add(left.place(new OptionSlider(CROSSHAIR_WIDTH, 0, 0, colW, 18, 0.0F, 50.0F,
                CrosshairConfig.width, false, "crosshairselection.width", this), colW), crosshairOn);
        add(left.place(new OptionSlider(CROSSHAIR_HEIGHT, 0, 0, colW, 18, 0.0F, 50.0F,
                CrosshairConfig.height, false, "crosshairselection.height", this), colW), crosshairOn);
        add(left.place(new OptionSlider(CROSSHAIR_GAP, 0, 0, colW, 18, 0.0F, 50.0F,
                CrosshairConfig.gap, false, "crosshairselection.gap", this), colW), crosshairOn);
        add(left.place(new OptionSlider(CROSSHAIR_THICKNESS, 0, 0, colW, 18, 0.7F, 10.0F,
                CrosshairConfig.thickness / 4.0F, false, "crosshairselection.thickness", this), colW), crosshairOn);
        add(left.place(new OptionSlider(CROSSHAIR_ROTATION, 0, 0, colW, 18, 0, 360,
                CrosshairConfig.rotation, true, "crosshairselection.rotation", this), colW), crosshairOn);

        // 右列：显示条件与流光入口，其余细节在「准星详情」子页面
        add(right.place(new GuiButton(CROSSHAIR_VISIBILITY_SETTINGS, 0, 0, colW, 18,
                StatCollector.translateToLocal("crosshairselection.visibility_settings")), colW));
        add(right.place(new GuiButton(CROSSHAIR_EXTRA_SETTINGS, 0, 0, colW, 18,
                StatCollector.translateToLocal("crosshairselection.crosshair_extra")), colW));
        right.group();
        add(right.place(flowButton(CROSSHAIR_RAINBOW, 0, 0, colW, CrosshairConfig.flow), colW));
        add(right.place(new OptionSlider(CROSSHAIR_RAINBOW_SPEED, 0, 0, colW, 18, 0, 2000,
                CrosshairConfig.flow.speed, true, "crosshairselection.flow_speed", this), colW), flowOn);
        right.group();
        add(right.place(toggle(CROSSHAIR_TIC_CROSSHAIR, 0, 0, colW, "crosshairselection.tic_short",
                CrosshairConfig.ticKeepCustomCrosshair), colW), crosshairOn);

        int footer = FlowLayout.footerTop(height);
        left.fitTo(FlowLayout.contentTop(height), footer);
        right.fitTo(FlowLayout.contentTop(height), footer);
        refreshEnabledStates();
    }

    private static Condition and(final Condition first, final Condition second) {
        return new Condition() {
            @Override
            public boolean test() {
                return first.test() && second.test();
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

    private static GuiButton flowButton(int id, int x, int y, FlowSettings flow) {
        return flowButton(id, x, y, 150, flow);
    }

    private static GuiButton flowButton(int id, int x, int y, int width, FlowSettings flow) {
        return new GuiButton(id, x, y, width, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.rainbow", flowModeName(flow)));
    }

    private static String flowModeName(FlowSettings flow) {
        switch (flow.mode) {
            case OFF:
                return StatCollector.translateToLocal("crosshairselection.flow.off");
            case SWEEP:
                return StatCollector.translateToLocal("crosshairselection.flow.sweep");
            case FLOW:
            default:
                return StatCollector.translateToLocal("crosshairselection.flow.flow");
        }
    }

    private static String onOff(boolean value) {
        return StatCollector.translateToLocal(value ? "crosshairselection.on" : "crosshairselection.off");
    }

    private static String colourLabel(String key, int colour) {
        return StatCollector.translateToLocalFormatted(key, ColorUtil.hex(colour));
    }

    private static String colourLabelKey(int id) {
        switch (id) {
            case SELECTION_COLOUR:
                return "crosshairselection.colour.selection_box";
            case CROSSHAIR_OUTLINE_COLOUR:
                return "crosshairselection.colour.crosshair_outline";
            case CROSSHAIR_DOT_COLOUR:
                return "crosshairselection.colour.crosshair_dot";
            case CROSSHAIR_COLOUR:
            default:
                return "crosshairselection.colour.crosshair";
        }
    }

    @Override
    protected void onButtonPressed(GuiButton button) {
        switch (button.id) {
            case TAB_SELECTION:
                tab = 0;
                initGui();
                break;
            case TAB_CROSSHAIR:
                tab = 1;
                initGui();
                break;
            case SAVE:
                ConfigurationManager.save(SelectionBoxConfig.class, CrosshairConfig.class);
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
                // Only the page being edited is reset, and the prompt names that scope.
                final boolean resetCrosshair = tab == 1;
                mc.displayGuiScreen(new ConfirmScreen(this,
                        StatCollector.translateToLocal(resetCrosshair
                                ? "crosshairselection.defaults.confirm.crosshair"
                                : "crosshairselection.defaults.confirm.selection"),
                        new Runnable() {
                            @Override
                            public void run() {
                                ConfigScreen reopened = new ConfigScreen(parent);
                                reopened.tab = resetCrosshair ? 1 : 0;
                                if (resetCrosshair) {
                                    CrosshairConfig.reset();
                                } else {
                                    SelectionBoxConfig.reset();
                                }
                                mc.displayGuiScreen(reopened);
                            }
                        }));
                break;
            case SELECTION_ENABLED:
                SelectionBoxConfig.enabled = !SelectionBoxConfig.enabled;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.enabled",
                        onOff(SelectionBoxConfig.enabled));
                refreshEnabledStates();
                break;
            case SELECTION_RAINBOW:
                childOpen = true;
                mc.displayGuiScreen(new FlowScreen(this, false));
                break;
            case SELECTION_LINK:
                SelectionBoxConfig.linkBlocks = !SelectionBoxConfig.linkBlocks;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.link_blocks",
                        onOff(SelectionBoxConfig.linkBlocks));
                refreshEnabledStates();
                break;
            case SELECTION_HIDDEN:
                SelectionBoxConfig.showHidden = !SelectionBoxConfig.showHidden;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.see_through",
                        onOff(SelectionBoxConfig.showHidden));
                refreshEnabledStates();
                break;
            case SELECTION_COLOUR:
                openColourPicker("crosshairselection.colour.title.selection_box", SelectionBoxConfig.colour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        SelectionBoxConfig.colour = argb;
                        refreshColourButton(SELECTION_COLOUR, argb);
                    }
                });
                break;
            case CROSSHAIR_ENABLED:
                CrosshairConfig.enabled = !CrosshairConfig.enabled;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.enabled",
                        onOff(CrosshairConfig.enabled));
                refreshEnabledStates();
                break;
            case CROSSHAIR_TYPE:
                CrosshairConfig.type = CrosshairConfig.type.next();
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.type",
                        StatCollector.translateToLocal(CrosshairConfig.type.getTranslationKey()));
                break;
            case CROSSHAIR_OUTLINE:
                CrosshairConfig.outline = !CrosshairConfig.outline;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.outline",
                        onOff(CrosshairConfig.outline));
                refreshEnabledStates();
                break;
            case CROSSHAIR_DOT:
                CrosshairConfig.dot = !CrosshairConfig.dot;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.dot",
                        onOff(CrosshairConfig.dot));
                refreshEnabledStates();
                break;
            case CROSSHAIR_RAINBOW:
                childOpen = true;
                mc.displayGuiScreen(new FlowScreen(this, true));
                break;
            case CROSSHAIR_VISIBLE:
                CrosshairConfig.visibleDefault = !CrosshairConfig.visibleDefault;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.visible",
                        onOff(CrosshairConfig.visibleDefault));
                break;
            case CROSSHAIR_COLOUR:
                openColourPicker("crosshairselection.colour.title.crosshair", CrosshairConfig.colour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        CrosshairConfig.colour = argb;
                        refreshColourButton(CROSSHAIR_COLOUR, argb);
                    }
                });
                break;
            case CROSSHAIR_OUTLINE_COLOUR:
                openColourPicker("crosshairselection.colour.title.crosshair_outline", CrosshairConfig.outlineColour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        CrosshairConfig.outlineColour = argb;
                        refreshColourButton(CROSSHAIR_OUTLINE_COLOUR, argb);
                    }
                });
                break;
            case CROSSHAIR_DOT_COLOUR:
                openColourPicker("crosshairselection.colour.title.crosshair_dot", CrosshairConfig.dotColour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        CrosshairConfig.dotColour = argb;
                        refreshColourButton(CROSSHAIR_DOT_COLOUR, argb);
                    }
                });
                break;
            case SELECTION_BREAK_POSITION:
                SelectionBoxConfig.breakProgressPosition = SelectionBoxConfig.breakProgressPosition.next();
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.hud_position",
                        StatCollector.translateToLocal(
                                SelectionBoxConfig.breakProgressPosition.getTranslationKey()));
                break;
            case SELECTION_BREAK_STYLE:
                SelectionBoxConfig.breakProgressStyle = SelectionBoxConfig.breakProgressStyle.next();
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.break_style",
                        StatCollector.translateToLocal(SelectionBoxConfig.breakProgressStyle.getTranslationKey()));
                break;
            case SELECTION_BREAK_PROGRESS:
                SelectionBoxConfig.showBreakProgress = !SelectionBoxConfig.showBreakProgress;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.break_progress", onOff(SelectionBoxConfig.showBreakProgress));
                break;
            case CROSSHAIR_FEEDBACK_STYLE:
                CrosshairConfig.feedbackStyle = CrosshairConfig.feedbackStyle.next();
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.feedback_style",
                        StatCollector.translateToLocal(CrosshairConfig.feedbackStyle.getTranslationKey()));
                initGui();
                break;
            case CROSSHAIR_BOW_FEEDBACK:
                CrosshairConfig.bowChargeFeedback = !CrosshairConfig.bowChargeFeedback;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.bow_feedback_short", onOff(CrosshairConfig.bowChargeFeedback));
                break;
            case CROSSHAIR_BOW_INDICATOR:
                CrosshairConfig.bowChargeIndicator = CrosshairConfig.bowChargeIndicator.next();
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.bow_indicator",
                        StatCollector.translateToLocal(CrosshairConfig.bowChargeIndicator.getTranslationKey()));
                break;
            case CROSSHAIR_ATTACK_FEEDBACK:
                CrosshairConfig.attackFeedback = !CrosshairConfig.attackFeedback;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.attack_feedback", onOff(CrosshairConfig.attackFeedback));
                break;
            case CROSSHAIR_PLACE_FEEDBACK:
                CrosshairConfig.placeFeedback = !CrosshairConfig.placeFeedback;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.place_feedback", onOff(CrosshairConfig.placeFeedback));
                break;
            case CROSSHAIR_TIC_CROSSHAIR:
                CrosshairConfig.ticKeepCustomCrosshair = !CrosshairConfig.ticKeepCustomCrosshair;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.tic_short", onOff(CrosshairConfig.ticKeepCustomCrosshair));
                break;
            case SELECTION_SMOOTH:
                SelectionBoxConfig.smoothTransition = !SelectionBoxConfig.smoothTransition;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.smooth_transition", onOff(SelectionBoxConfig.smoothTransition));
                break;
            case SELECTION_FILL_SETTINGS:
                childOpen = true;
                mc.displayGuiScreen(new FillScreen(this));
                break;
            case CROSSHAIR_EXTRA_SETTINGS:
                childOpen = true;
                mc.displayGuiScreen(new CrosshairExtraScreen(this));
                break;
            case CROSSHAIR_VISIBILITY_SETTINGS:
                childOpen = true;
                mc.displayGuiScreen(new VisibilityScreen(this));
                break;
            case SELECTION_GT_SETTINGS:
                childOpen = true;
                mc.displayGuiScreen(new GtOverlayScreen(this));
                break;
            default:
                break;
        }
    }

    private void openColourPicker(String titleKey, int initial, ColorCallback callback) {
        // displayGuiScreen() calls onGuiClosed() on this screen; do not let that revert the
        // values we are about to pass to the child colour picker.
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
    protected void onSliderValue(int optionId, float value) {
        switch (optionId) {
            case SELECTION_THICKNESS:
                SelectionBoxConfig.thickness = value;
                break;
            case SELECTION_MARGIN:
                SelectionBoxConfig.outlineMargin = Math.round(value * 1000.0F) / 1000.0F;
                break;
            case SELECTION_RAINBOW_SPEED:
                SelectionBoxConfig.flow.speed = Math.round(value);
                break;
            case CROSSHAIR_WIDTH:
                CrosshairConfig.width = value;
                break;
            case CROSSHAIR_HEIGHT:
                CrosshairConfig.height = value;
                break;
            case CROSSHAIR_GAP:
                CrosshairConfig.gap = value;
                break;
            case CROSSHAIR_THICKNESS:
                // 低于 0.7 GUI 像素时线条不足一个物理像素，观感已经不可控，直接钳住。
                CrosshairConfig.thickness = Math.max(3, Math.round(value * 4.0F));
                break;
            case CROSSHAIR_ROTATION:
                CrosshairConfig.rotation = Math.round(value);
                break;
            case CROSSHAIR_RAINBOW_SPEED:
                CrosshairConfig.flow.speed = Math.round(value);
                break;
            case CROSSHAIR_FEEDBACK_STRENGTH:
                CrosshairConfig.feedbackStrength = value;
                break;
            case CROSSHAIR_EXPAND_SPEED:
                CrosshairConfig.expandSpeed = value;
                break;
            case CROSSHAIR_RETURN_SPEED:
                CrosshairConfig.returnSpeed = value;
                break;
            case CROSSHAIR_OUTLINE_THICKNESS:
                CrosshairConfig.outlineThickness = value;
                break;
            case SELECTION_TRANSITION:
                SelectionBoxConfig.transitionDuration = value;
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj, "Crosshair-Selection", width / 2, 6, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        PreviewPanel.begin(tab == 1 ? PreviewPanel.Content.CROSSHAIR : PreviewPanel.Content.CUBE,
                selectionFlowForPreview());
        PreviewPanel.drawCentred(width, height);
    }

    private static me.kuionc.crosshairselection.config.FlowSettings selectionFlowForPreview() {
        return SelectionBoxConfig.flow;
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

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class Snapshot {

        private final boolean selectionEnabled;
        private final int selectionColour;
        private final float selectionThickness;
        private final float selectionOutlineMargin;
        private final int selectionBlinkAlpha;
        private final float selectionBlinkSpeed;
        private final boolean selectionLink;
        private final boolean selectionHidden;
        private final boolean selectionSmooth;
        private final float selectionTransitionDuration;
        private final boolean selectionFill;
        private final boolean selectionBlink;

        private final boolean crosshairEnabled;
        private final Object crosshairType;
        private final int crosshairColour;
        private final float crosshairWidth;
        private final float crosshairHeight;
        private final float crosshairGap;
        private final int crosshairThickness;
        private final int crosshairRotation;
        private final boolean crosshairOutline;
        private final int crosshairOutlineColour;
        private final float crosshairOutlineThickness;
        private final float crosshairExpandSpeed;
        private final float crosshairReturnSpeed;
        private final boolean crosshairBowFeedback;
        private final me.kuionc.crosshairselection.config.BowChargeIndicator crosshairBowIndicator;
        private final boolean crosshairDot;
        private final int crosshairDotColour;
        private final me.kuionc.crosshairselection.config.FlowSettings crosshairFlow = new me.kuionc.crosshairselection.config.FlowSettings();
        private final me.kuionc.crosshairselection.config.FlowSettings selectionFlow = new me.kuionc.crosshairselection.config.FlowSettings();
        private final boolean crosshairUseTicBowCrosshair;
        private final boolean crosshairAttackFeedback;
        private final boolean crosshairPlaceFeedback;
        private final float crosshairFeedbackStrength;
        private final me.kuionc.crosshairselection.config.FeedbackStyle crosshairFeedbackStyle;
        private final boolean selectionBreakProgress;
        private final me.kuionc.crosshairselection.config.BreakProgressStyle selectionBreakStyle;
        private final boolean crosshairVisible;
        private final boolean crosshairVisibleF1;
        private final boolean crosshairVisibleDebug;
        private final boolean crosshairVisibleThird;

        Snapshot() {
            selectionEnabled = SelectionBoxConfig.enabled;
            selectionColour = SelectionBoxConfig.colour;
            selectionThickness = SelectionBoxConfig.thickness;
            selectionOutlineMargin = SelectionBoxConfig.outlineMargin;
            selectionBlinkAlpha = SelectionBoxConfig.blinkAlpha;
            selectionBlinkSpeed = SelectionBoxConfig.blinkSpeed;
            selectionFlow.copyFrom(SelectionBoxConfig.flow);

            selectionLink = SelectionBoxConfig.linkBlocks;
            selectionHidden = SelectionBoxConfig.showHidden;
            selectionSmooth = SelectionBoxConfig.smoothTransition;
            selectionTransitionDuration = SelectionBoxConfig.transitionDuration;
            selectionFill = SelectionBoxConfig.showFill;
            selectionBlink = SelectionBoxConfig.blink;

            crosshairEnabled = CrosshairConfig.enabled;
            crosshairType = CrosshairConfig.type;
            crosshairColour = CrosshairConfig.colour;
            crosshairWidth = CrosshairConfig.width;
            crosshairHeight = CrosshairConfig.height;
            crosshairGap = CrosshairConfig.gap;
            crosshairThickness = CrosshairConfig.thickness;
            crosshairRotation = CrosshairConfig.rotation;
            crosshairOutline = CrosshairConfig.outline;
            crosshairOutlineColour = CrosshairConfig.outlineColour;
            crosshairOutlineThickness = CrosshairConfig.outlineThickness;
            crosshairExpandSpeed = CrosshairConfig.expandSpeed;
            crosshairReturnSpeed = CrosshairConfig.returnSpeed;
            crosshairBowFeedback = CrosshairConfig.bowChargeFeedback;
            crosshairBowIndicator = CrosshairConfig.bowChargeIndicator;
            crosshairDot = CrosshairConfig.dot;
            crosshairDotColour = CrosshairConfig.dotColour;
            crosshairFlow.copyFrom(CrosshairConfig.flow);
            crosshairUseTicBowCrosshair = CrosshairConfig.ticKeepCustomCrosshair;
            crosshairAttackFeedback = CrosshairConfig.attackFeedback;
            crosshairPlaceFeedback = CrosshairConfig.placeFeedback;
            crosshairFeedbackStrength = CrosshairConfig.feedbackStrength;
            crosshairFeedbackStyle = CrosshairConfig.feedbackStyle;
            selectionBreakProgress = SelectionBoxConfig.showBreakProgress;
            selectionBreakStyle = SelectionBoxConfig.breakProgressStyle;
            crosshairVisible = CrosshairConfig.visibleDefault;
            crosshairVisibleF1 = CrosshairConfig.visibleHiddenGui;
            crosshairVisibleDebug = CrosshairConfig.visibleDebug;
            crosshairVisibleThird = CrosshairConfig.visibleThirdPerson;
        }

        void restore() {
            SelectionBoxConfig.enabled = selectionEnabled;
            SelectionBoxConfig.colour = selectionColour;
            SelectionBoxConfig.thickness = selectionThickness;
            SelectionBoxConfig.outlineMargin = selectionOutlineMargin;
            SelectionBoxConfig.blinkAlpha = selectionBlinkAlpha;
            SelectionBoxConfig.blinkSpeed = selectionBlinkSpeed;
            SelectionBoxConfig.flow.copyFrom(selectionFlow);

            SelectionBoxConfig.linkBlocks = selectionLink;
            SelectionBoxConfig.showHidden = selectionHidden;
            SelectionBoxConfig.smoothTransition = selectionSmooth;
            SelectionBoxConfig.transitionDuration = selectionTransitionDuration;
            SelectionBoxConfig.showFill = selectionFill;
            SelectionBoxConfig.blink = selectionBlink;

            CrosshairConfig.enabled = crosshairEnabled;
            CrosshairConfig.type = (me.kuionc.crosshairselection.render.CrosshairType) crosshairType;
            CrosshairConfig.colour = crosshairColour;
            CrosshairConfig.width = crosshairWidth;
            CrosshairConfig.height = crosshairHeight;
            CrosshairConfig.gap = crosshairGap;
            CrosshairConfig.thickness = crosshairThickness;
            CrosshairConfig.rotation = crosshairRotation;
            CrosshairConfig.outline = crosshairOutline;
            CrosshairConfig.outlineColour = crosshairOutlineColour;
            CrosshairConfig.outlineThickness = crosshairOutlineThickness;
            CrosshairConfig.expandSpeed = crosshairExpandSpeed;
            CrosshairConfig.returnSpeed = crosshairReturnSpeed;
            CrosshairConfig.bowChargeFeedback = crosshairBowFeedback;
            CrosshairConfig.bowChargeIndicator = crosshairBowIndicator;
            CrosshairConfig.dot = crosshairDot;
            CrosshairConfig.dotColour = crosshairDotColour;
            CrosshairConfig.flow.copyFrom(crosshairFlow);
            CrosshairConfig.ticKeepCustomCrosshair = crosshairUseTicBowCrosshair;
            CrosshairConfig.attackFeedback = crosshairAttackFeedback;
            CrosshairConfig.placeFeedback = crosshairPlaceFeedback;
            CrosshairConfig.feedbackStrength = crosshairFeedbackStrength;
            CrosshairConfig.feedbackStyle = crosshairFeedbackStyle;
            SelectionBoxConfig.showBreakProgress = selectionBreakProgress;
            SelectionBoxConfig.breakProgressStyle = selectionBreakStyle;
            CrosshairConfig.visibleDefault = crosshairVisible;
            CrosshairConfig.visibleHiddenGui = crosshairVisibleF1;
            CrosshairConfig.visibleDebug = crosshairVisibleDebug;
            CrosshairConfig.visibleThirdPerson = crosshairVisibleThird;
        }
    }
}
