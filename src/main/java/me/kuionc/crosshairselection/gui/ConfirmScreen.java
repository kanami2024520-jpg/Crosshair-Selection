package me.kuionc.crosshairselection.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * Small yes/no prompt used for destructive actions such as restoring defaults.
 */
public class ConfirmScreen extends GuiScreen {

    private static final int CONFIRM = 1;
    private static final int BACK = 2;

    private final GuiScreen parent;
    private final String message;
    private final Runnable action;

    public ConfirmScreen(GuiScreen parent, String message, Runnable action) {
        this.parent = parent;
        this.message = message;
        this.action = action;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(new GuiButton(CONFIRM, width / 2 - 105, height / 2 + 10, 100, 20,
                StatCollector.translateToLocal("crosshairselection.confirm")));
        buttonList.add(new GuiButton(BACK, width / 2 + 5, height / 2 + 10, 100, 20,
                StatCollector.translateToLocal("crosshairselection.back")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == CONFIRM) {
            action.run();
        } else {
            mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, message, width / 2, height / 2 - 10, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }
}
