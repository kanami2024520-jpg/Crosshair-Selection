package me.kuionc.crosshairselection.command;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Opens the config screen after the current client tick. GuiChat closes itself with
 * displayGuiScreen(null) right after executing a client command, so opening the screen
 * immediately from the command would be undone in the same key press.
 */
public final class ConfigScreenOpener {

    private static boolean pending;

    public static void request() {
        pending = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !pending) {
            return;
        }
        pending = false;
        McRef.get().displayGuiScreen(new ConfigScreen(null));
    }
}
