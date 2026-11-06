package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.gui.widgets.ColorButton;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * 准星「详情」子页面：描边、中心点、交互反馈与蓄力提示。
 *
 * <p>准星主页原本把这些和形状设置挤在一页，控件数量超出屏幕高度；
 * 拆出本页后两页都能完整显示，布局由 {@link FlowLayout} 统一排布。
 */
public class CrosshairExtraScreen extends SettingsScreen {

    private static final int SAVE = 80;
    private static final int CANCEL = 81;
    private static final int DEFAULTS = 82;

    private static final int OUTLINE = 1;
    private static final int OUTLINE_COLOUR = 2;
    private static final int OUTLINE_THICKNESS = 3;
    private static final int DOT = 4;
    private static final int DOT_COLOUR = 5;
    private static final int FEEDBACK_STYLE = 6;
    private static final int FEEDBACK_STRENGTH = 7;
    private static final int EXPAND_SPEED = 8;
    private static final int RETURN_SPEED = 9;
    private static final int BOW_FEEDBACK = 10;
    private static final int BOW_INDICATOR = 11;
    private static final int BOW_POSITION = 12;

    private final GuiScreen parent;
    private Snapshot snapshot;
    private boolean saved;
    private boolean childOpen;

    public CrosshairExtraScreen(GuiScreen parent) {
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
        int half = (colW - Math.max(2, rowH / 10)) / 2;

        FlowLayout left = new FlowLayout(margin, top, colW, rowH);
        FlowLayout right = new FlowLayout(width - colW - margin, top, colW, rowH);

        final Condition crosshairOn = new Condition() {
            @Override
            public boolean test() {
                return CrosshairConfig.enabled;
            }
        };
        final Condition outlineOn = and(crosshairOn, new Condition() {
            @Override
            public boolean test() {
                return CrosshairConfig.outline;
            }
        });
        final Condition dotOn = and(crosshairOn, new Condition() {
            @Override
            public boolean test() {
                return CrosshairConfig.dot;
            }
        });

        // 左列：描边与中心点
        add(left.place(toggle(OUTLINE, 0, 0, "crosshairselection.outline",
                CrosshairConfig.outline), colW), crosshairOn);
        add(left.place(new ColorButton(OUTLINE_COLOUR, 0, 0, colW, 18, CrosshairConfig.outlineColour,
                colourLabel("crosshairselection.colour.crosshair_outline",
                        CrosshairConfig.outlineColour)), colW), outlineOn);
        add(left.place(new OptionSlider(OUTLINE_THICKNESS, 0, 0, colW, 18, 0.0F, 2.0F,
                CrosshairConfig.outlineThickness, false,
                "crosshairselection.outline_thickness", this), colW), outlineOn);
        left.group();
        add(left.place(toggle(DOT, 0, 0, "crosshairselection.dot", CrosshairConfig.dot), colW), crosshairOn);
        add(left.place(new ColorButton(DOT_COLOUR, 0, 0, colW, 18, CrosshairConfig.dotColour,
                colourLabel("crosshairselection.colour.crosshair_dot", CrosshairConfig.dotColour)), colW), dotOn);

        // 右列：交互反馈与蓄力
        add(right.place(new GuiButton(FEEDBACK_STYLE, 0, 0, colW, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.feedback_style",
                        StatCollector.translateToLocal(CrosshairConfig.feedbackStyle.getTranslationKey()))), colW),
                crosshairOn);
        add(right.place(new OptionSlider(FEEDBACK_STRENGTH, 0, 0, colW, 18, 0.0F, 2.0F,
                CrosshairConfig.feedbackStrength, false, 2,
                "crosshairselection.feedback_strength", this), colW), crosshairOn);
        add(right.place(new OptionSlider(EXPAND_SPEED, 0, 0, half, 18, 0.1F, 5.0F,
                CrosshairConfig.expandSpeed, false, 1, "crosshairselection.expand_speed", this), half),
                crosshairOn);
        add(right.place(new OptionSlider(RETURN_SPEED, 0, 0, half, 18, 0.1F, 5.0F,
                CrosshairConfig.returnSpeed, false, 1, "crosshairselection.return_speed", this), half),
                crosshairOn);
        right.group();
        add(right.place(toggle(BOW_FEEDBACK, 0, 0, half, "crosshairselection.bow_feedback_short",
                CrosshairConfig.bowChargeFeedback), half), crosshairOn);
        add(right.place(new GuiButton(BOW_INDICATOR, 0, 0, half, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.bow_indicator",
                        StatCollector.translateToLocal(CrosshairConfig.bowChargeIndicator.getTranslationKey()))),
                half), crosshairOn);
        add(right.place(new GuiButton(BOW_POSITION, 0, 0, colW, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.hud_position",
                        StatCollector.translateToLocal(
                                CrosshairConfig.bowIndicatorPosition.getTranslationKey()))), colW),
                new Condition() {
                    @Override
                    public boolean test() {
                        return CrosshairConfig.bowChargeIndicator
                                != me.kuionc.crosshairselection.config.BowChargeIndicator.NONE;
                    }
                });

        int footerY = FlowLayout.footerTop(height);
        left.fitTo(top, footerY);
        right.fitTo(top, footerY);
        add(new GuiButton(SAVE, width / 2 - 145, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.save")));
        add(new GuiButton(CANCEL, width / 2 - 45, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.back")));
        add(new GuiButton(DEFAULTS, width / 2 + 55, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.defaults")));
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
        return toggle(id, x, y, FlowLayout.columnWidth(width), key, value);
    }

    private GuiButton toggle(int id, int x, int y, int buttonWidth, String key, boolean value) {
        return new GuiButton(id, x, y, buttonWidth, 18,
                StatCollector.translateToLocalFormatted(key, onOff(value)));
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
            case OUTLINE:
                CrosshairConfig.outline = !CrosshairConfig.outline;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.outline", onOff(CrosshairConfig.outline));
                break;
            case DOT:
                CrosshairConfig.dot = !CrosshairConfig.dot;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.dot", onOff(CrosshairConfig.dot));
                break;
            case BOW_FEEDBACK:
                CrosshairConfig.bowChargeFeedback = !CrosshairConfig.bowChargeFeedback;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.bow_feedback_short", onOff(CrosshairConfig.bowChargeFeedback));
                break;
            case FEEDBACK_STYLE:
                CrosshairConfig.feedbackStyle = CrosshairConfig.feedbackStyle.next();
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.feedback_style",
                        StatCollector.translateToLocal(CrosshairConfig.feedbackStyle.getTranslationKey()));
                break;
            case BOW_POSITION:
                CrosshairConfig.bowIndicatorPosition = CrosshairConfig.bowIndicatorPosition.next();
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.hud_position",
                        StatCollector.translateToLocal(
                                CrosshairConfig.bowIndicatorPosition.getTranslationKey()));
                break;
            case BOW_INDICATOR:
                CrosshairConfig.bowChargeIndicator = CrosshairConfig.bowChargeIndicator.next();
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.bow_indicator",
                        StatCollector.translateToLocal(CrosshairConfig.bowChargeIndicator.getTranslationKey()));
                break;
            case OUTLINE_COLOUR:
                childOpen = true;
                mc.displayGuiScreen(new ColorPickerScreen(this,
                        "crosshairselection.colour.title.crosshair_outline", CrosshairConfig.outlineColour,
                        argb -> {
                            CrosshairConfig.outlineColour = argb;
                            refreshColourButton(OUTLINE_COLOUR, argb,
                                    "crosshairselection.colour.crosshair_outline");
                        }));
                break;
            case DOT_COLOUR:
                childOpen = true;
                mc.displayGuiScreen(new ColorPickerScreen(this,
                        "crosshairselection.colour.title.crosshair_dot", CrosshairConfig.dotColour,
                        argb -> {
                            CrosshairConfig.dotColour = argb;
                            refreshColourButton(DOT_COLOUR, argb,
                                    "crosshairselection.colour.crosshair_dot");
                        }));
                break;
            case SAVE:
                ConfigurationManager.save(CrosshairConfig.class);
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
                        StatCollector.translateToLocal("crosshairselection.defaults.confirm.crosshair"),
                        () -> {
                            CrosshairConfig.outline = false;
                            CrosshairConfig.outlineColour = 0xFF000000;
                            CrosshairConfig.outlineThickness = 0.5F;
                            CrosshairConfig.dot = false;
                            CrosshairConfig.dotColour = 0xFF000000;
                            CrosshairConfig.feedbackStyle = me.kuionc.crosshairselection.config.FeedbackStyle.SPREAD;
                            CrosshairConfig.feedbackStrength = 1.0F;
                            CrosshairConfig.expandSpeed = 2.5F;
                            CrosshairConfig.returnSpeed = 1.2F;
                            CrosshairConfig.bowChargeFeedback = true;
                            CrosshairConfig.bowChargeIndicator =
                                    me.kuionc.crosshairselection.config.BowChargeIndicator.NUMBER;
                            mc.displayGuiScreen(new CrosshairExtraScreen(parent));
                        }));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSliderValue(int optionId, float value) {
        switch (optionId) {
            case OUTLINE_THICKNESS:
                CrosshairConfig.outlineThickness = value;
                break;
            case FEEDBACK_STRENGTH:
                CrosshairConfig.feedbackStrength = value;
                break;
            case EXPAND_SPEED:
                CrosshairConfig.expandSpeed = value;
                break;
            case RETURN_SPEED:
                CrosshairConfig.returnSpeed = value;
                break;
            default:
                break;
        }
    }

    private void refreshColourButton(int id, int colour, String key) {
        for (Object object : buttonList) {
            if (object instanceof ColorButton && ((ColorButton) object).id == id) {
                ColorButton button = (ColorButton) object;
                button.setColour(colour);
                button.displayString = colourLabel(key, colour);
                return;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        PreviewPanel.begin(PreviewPanel.Content.CROSSHAIR, CrosshairConfig.flow);
        PreviewPanel.drawCentred(width, height);
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.crosshair_extra"),
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
        private final boolean outline;
        private final int outlineColour;
        private final float outlineThickness;
        private final boolean dot;
        private final int dotColour;
        private final Object feedbackStyle;
        private final float feedbackStrength;
        private final float expandSpeed;
        private final float returnSpeed;
        private final boolean bowFeedback;
        private final Object bowIndicator;

        Snapshot() {
            outline = CrosshairConfig.outline;
            outlineColour = CrosshairConfig.outlineColour;
            outlineThickness = CrosshairConfig.outlineThickness;
            dot = CrosshairConfig.dot;
            dotColour = CrosshairConfig.dotColour;
            feedbackStyle = CrosshairConfig.feedbackStyle;
            feedbackStrength = CrosshairConfig.feedbackStrength;
            expandSpeed = CrosshairConfig.expandSpeed;
            returnSpeed = CrosshairConfig.returnSpeed;
            bowFeedback = CrosshairConfig.bowChargeFeedback;
            bowIndicator = CrosshairConfig.bowChargeIndicator;
        }

        void restore() {
            CrosshairConfig.outline = outline;
            CrosshairConfig.outlineColour = outlineColour;
            CrosshairConfig.outlineThickness = outlineThickness;
            CrosshairConfig.dot = dot;
            CrosshairConfig.dotColour = dotColour;
            CrosshairConfig.feedbackStyle =
                    (me.kuionc.crosshairselection.config.FeedbackStyle) feedbackStyle;
            CrosshairConfig.feedbackStrength = feedbackStrength;
            CrosshairConfig.expandSpeed = expandSpeed;
            CrosshairConfig.returnSpeed = returnSpeed;
            CrosshairConfig.bowChargeFeedback = bowFeedback;
            CrosshairConfig.bowChargeIndicator =
                    (me.kuionc.crosshairselection.config.BowChargeIndicator) bowIndicator;
        }
    }
}
