package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * 准星的「显示条件」子页面。
 *
 * <p>四个可见性开关原先平铺在准星主页的右列，占满四行；
 * 收进子页面后主页可以把空间留给形状与反馈设置。
 */
public class VisibilityScreen extends SettingsScreen {

    private static final int SAVE = 70;
    private static final int CANCEL = 71;
    private static final int DEFAULTS = 72;

    private static final int VISIBLE = 1;
    private static final int VISIBLE_F1 = 2;
    private static final int VISIBLE_DEBUG = 3;
    private static final int VISIBLE_THIRD = 4;

    private final GuiScreen parent;
    private Snapshot snapshot;
    private boolean saved;
    private boolean childOpen;

    public VisibilityScreen(GuiScreen parent) {
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

        add(flow.place(toggle(VISIBLE, 0, 0, "crosshairselection.visible", CrosshairConfig.visibleDefault), colW));
        add(flow.place(toggle(VISIBLE_F1, 0, 0, "crosshairselection.visible_f1",
                CrosshairConfig.visibleHiddenGui), colW));
        add(flow.place(toggle(VISIBLE_DEBUG, 0, 0, "crosshairselection.visible_debug",
                CrosshairConfig.visibleDebug), colW));
        add(flow.place(toggle(VISIBLE_THIRD, 0, 0, "crosshairselection.visible_third",
                CrosshairConfig.visibleThirdPerson), colW));

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
            case VISIBLE:
                CrosshairConfig.visibleDefault = !CrosshairConfig.visibleDefault;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.visible", onOff(CrosshairConfig.visibleDefault));
                break;
            case VISIBLE_F1:
                CrosshairConfig.visibleHiddenGui = !CrosshairConfig.visibleHiddenGui;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.visible_f1", onOff(CrosshairConfig.visibleHiddenGui));
                break;
            case VISIBLE_DEBUG:
                CrosshairConfig.visibleDebug = !CrosshairConfig.visibleDebug;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.visible_debug", onOff(CrosshairConfig.visibleDebug));
                break;
            case VISIBLE_THIRD:
                CrosshairConfig.visibleThirdPerson = !CrosshairConfig.visibleThirdPerson;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.visible_third", onOff(CrosshairConfig.visibleThirdPerson));
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
                        StatCollector.translateToLocal("crosshairselection.defaults.confirm.visibility"),
                        new Runnable() {
                            @Override
                            public void run() {
                                CrosshairConfig.visibleDefault = true;
                                CrosshairConfig.visibleHiddenGui = true;
                                CrosshairConfig.visibleDebug = true;
                                CrosshairConfig.visibleThirdPerson = false;
                                mc.displayGuiScreen(new VisibilityScreen(parent));
                            }
                        }));
                break;
            default:
                break;
        }
    }

    @Override
    protected void onSliderValue(int optionId, float value) {
        // 本页没有滑杆。
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        PreviewPanel.begin(PreviewPanel.Content.CROSSHAIR, CrosshairConfig.flow);
        PreviewPanel.drawCentred(width, height);
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.visibility_settings"),
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
        private final boolean visible;
        private final boolean f1;
        private final boolean debug;
        private final boolean third;

        Snapshot() {
            visible = CrosshairConfig.visibleDefault;
            f1 = CrosshairConfig.visibleHiddenGui;
            debug = CrosshairConfig.visibleDebug;
            third = CrosshairConfig.visibleThirdPerson;
        }

        void restore() {
            CrosshairConfig.visibleDefault = visible;
            CrosshairConfig.visibleHiddenGui = f1;
            CrosshairConfig.visibleDebug = debug;
            CrosshairConfig.visibleThirdPerson = third;
        }
    }
}
