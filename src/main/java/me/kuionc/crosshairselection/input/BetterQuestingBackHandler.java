package me.kuionc.crosshairselection.input;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.compat.BetterQuestingCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

/**
 * Mouse side-button support for the BetterQuesting back key.
 *
 * <p>Mouse buttons never reach {@code GuiScreen} as key events, and FML input events are not fired
 * while a GUI is open, so the button is polled once per client tick with an edge check. Only
 * BetterQuesting canvases are affected.
 */
public final class BetterQuestingBackHandler {

    private static final int MOUSE_KEY_OFFSET = 100;
    private static final int MAX_MOUSE_BUTTONS = 32;

    private boolean wasDown;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        int button = mouseButtonOf(BetterQuestingKeybinds.BACK.getKeyCode());
        boolean down = button >= 0 && Mouse.isButtonDown(button);
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed) {
            return;
        }
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        BetterQuestingCompat.goBack(screen);
    }

    /** Mouse buttons are stored as {@code button - 100}, so a side button 4 is saved as -96. */
    private static int mouseButtonOf(int keyCode) {
        if (keyCode >= 0 || keyCode < -MOUSE_KEY_OFFSET - MAX_MOUSE_BUTTONS) {
            return -1;
        }
        return keyCode + MOUSE_KEY_OFFSET;
    }

    public static void register() {
        FMLCommonHandler.instance().bus().register(new BetterQuestingBackHandler());
    }
}
