package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * Rendering-wide options shared by the selection box, the GT overlay and the crosshair.
 */
public class RenderingScreen extends GuiScreen {

    private static final int TEXTURE_SIZE = 1;
    private static final int ANTI_ALIAS = 2;

    private static final int SAVE = 20;
    private static final int CANCEL = 21;
    private static final int DEFAULTS = 22;

    private static final int[] TEXTURE_SIZES = {0, 128, 256, 512};

    private final GuiScreen parent;
    private Snapshot snapshot;
    private boolean saved;

    public RenderingScreen(GuiScreen parent) {
        this.parent = parent;
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

        buttonList.add(new GuiButton(TEXTURE_SIZE, left, y, 150, 18, textureSizeLabel()));
        buttonList.add(new GuiButton(ANTI_ALIAS, right, y, 150, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.anti_alias",
                        onOff(SelectionBoxConfig.antiAlias))));

        buttonList.add(new GuiButton(SAVE, width / 2 - 145, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.save")));
        buttonList.add(new GuiButton(CANCEL, width / 2 - 45, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.cancel")));
        buttonList.add(new GuiButton(DEFAULTS, width / 2 + 55, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.defaults")));
    }

    private static String textureSizeLabel() {
        int size = SelectionBoxConfig.rainbowTextureSize;
        String value = size <= 0
                ? StatCollector.translateToLocal("crosshairselection.auto")
                : Integer.toString(size);
        return StatCollector.translateToLocalFormatted("crosshairselection.rainbow_texture_size", value);
    }

    private static String onOff(boolean value) {
        return StatCollector.translateToLocal(value ? "crosshairselection.on" : "crosshairselection.off");
    }

    private void cycleTextureSize() {
        int current = SelectionBoxConfig.rainbowTextureSize;
        int index = 0;
        for (int i = 0; i < TEXTURE_SIZES.length; i++) {
            if (TEXTURE_SIZES[i] == current) {
                index = i;
                break;
            }
        }
        SelectionBoxConfig.rainbowTextureSize = TEXTURE_SIZES[(index + 1) % TEXTURE_SIZES.length];
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case TEXTURE_SIZE:
                cycleTextureSize();
                button.displayString = textureSizeLabel();
                break;
            case ANTI_ALIAS:
                SelectionBoxConfig.antiAlias = !SelectionBoxConfig.antiAlias;
                button.displayString = StatCollector.translateToLocalFormatted(
                        "crosshairselection.anti_alias", onOff(SelectionBoxConfig.antiAlias));
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
                SelectionBoxConfig.rainbowTextureSize = 0;
                SelectionBoxConfig.antiAlias = false;
                initGui();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.rendering_settings_title"),
                width / 2, 6, 0xFFFFFF);
        this.drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.rainbow_texture_size_hint"),
                width / 2, height - 48, 0xA0A0A0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        if (!saved && snapshot != null) {
            snapshot.restore();
        }
    }

    private static final class Snapshot {
        private final int textureSize;
        private final boolean antiAlias;

        Snapshot() {
            textureSize = SelectionBoxConfig.rainbowTextureSize;
            antiAlias = SelectionBoxConfig.antiAlias;
        }

        void restore() {
            SelectionBoxConfig.rainbowTextureSize = textureSize;
            SelectionBoxConfig.antiAlias = antiAlias;
        }
    }
}
