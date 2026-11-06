package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * 选框「填充」子页面：填充开关、闪烁开关、闪烁透明度与速度。
 *
 * <p>这些设置原先是混在选框主页里的，和流式、链接、GT 等条目挤在一起；
 * 单独成页后主页只保留一个入口按钮，布局更稳定。
 */
public class FillScreen extends SettingsScreen {

    private static final int SAVE = 60;
    private static final int CANCEL = 61;
    private static final int DEFAULTS = 62;

    private static final int FILL = 1;
    private static final int BLINK = 2;
    private static final int BLINK_ALPHA = 3;
    private static final int BLINK_SPEED = 4;

    private final GuiScreen parent;
    private Snapshot snapshot;
    private boolean saved;
    private boolean childOpen;

    public FillScreen(GuiScreen parent) {
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
        FlowLayout flow = new FlowLayout(4, FlowLayout.contentTop(height), colW, rowH);

        final Condition fillOn = new Condition() {
            @Override
            public boolean test() {
                return SelectionBoxConfig.showFill;
            }
        };
        final Condition blinkOn = new Condition() {
            @Override
            public boolean test() {
                return SelectionBoxConfig.showFill && SelectionBoxConfig.blink;
            }
        };

        add(flow.place(toggle(FILL, 0, 0, "crosshairselection.fill", SelectionBoxConfig.showFill), colW));
        add(flow.place(toggle(BLINK, 0, 0, "crosshairselection.blink", SelectionBoxConfig.blink), colW),
                fillOn);
        add(flow.place(new OptionSlider(BLINK_ALPHA, 0, 0, colW, 18, 0, 255,
                SelectionBoxConfig.blinkAlpha, true, "crosshairselection.blink_alpha", this), colW), fillOn);
        add(flow.place(new OptionSlider(BLINK_SPEED, 0, 0, colW, 18, 0.0F, 1.0F,
                SelectionBoxConfig.blinkSpeed, false, "crosshairselection.blink_speed", this), colW), blinkOn);

        int footerY = FlowLayout.footerTop(height);
        flow.fitTo(FlowLayout.contentTop(height), footerY);
        add(new GuiButton(SAVE, width / 2 - 145, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.save")));
        add(new GuiButton(CANCEL, width / 2 - 45, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.back")));
        add(new GuiButton(DEFAULTS, width / 2 + 55, footerY, 90, 18,
                StatCollector.translateToLocal("crosshairselection.defaults")));
        refreshEnabledStates();
    }

    private GuiButton toggle(int id, int x, int y, String key, boolean value) {
        return new GuiButton(id, x, y, FlowLayout.columnWidth(width), 18,
                StatCollector.translateToLocalFormatted(key, onOff(value)));
    }

    private static String onOff(boolean value) {
        return StatCollector.translateToLocal(value ? "crosshairselection.on" : "crosshairselection.off");
    }

    @Override
    protected void onButtonPressed(GuiButton button) {
        switch (button.id) {
            case FILL:
                SelectionBoxConfig.showFill = !SelectionBoxConfig.showFill;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.fill", onOff(SelectionBoxConfig.showFill));
                break;
            case BLINK:
                SelectionBoxConfig.blink = !SelectionBoxConfig.blink;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.blink", onOff(SelectionBoxConfig.blink));
                break;
            case SAVE:
                ConfigurationManager.save(SelectionBoxConfig.class);
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
                        StatCollector.translateToLocal("crosshairselection.defaults.confirm.fill"),
                        new Runnable() {
                            @Override
                            public void run() {
                                FeedbackDefaults.reset();
                                mc.displayGuiScreen(new FillScreen(parent));
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
            case BLINK_ALPHA:
                SelectionBoxConfig.blinkAlpha = Math.round(value);
                break;
            case BLINK_SPEED:
                SelectionBoxConfig.blinkSpeed = value;
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        PreviewPanel.begin(PreviewPanel.Content.CUBE, SelectionBoxConfig.flow);
        PreviewPanel.drawCentred(width, height);
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.fill_settings"),
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

    /** 本页默认值：与选框页保持一致，只重置填充相关字段。 */
    private static final class FeedbackDefaults {
        static void reset() {
            SelectionBoxConfig.showFill = true;
            SelectionBoxConfig.blink = true;
            SelectionBoxConfig.blinkAlpha = 99;
            SelectionBoxConfig.blinkSpeed = 0.2F;
        }
    }

    private static final class Snapshot {
        private final boolean fill;
        private final boolean blink;
        private final int blinkAlpha;
        private final float blinkSpeed;

        Snapshot() {
            fill = SelectionBoxConfig.showFill;
            blink = SelectionBoxConfig.blink;
            blinkAlpha = SelectionBoxConfig.blinkAlpha;
            blinkSpeed = SelectionBoxConfig.blinkSpeed;
        }

        void restore() {
            SelectionBoxConfig.showFill = fill;
            SelectionBoxConfig.blink = blink;
            SelectionBoxConfig.blinkAlpha = blinkAlpha;
            SelectionBoxConfig.blinkSpeed = blinkSpeed;
        }
    }
}
