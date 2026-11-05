package me.kuionc.crosshairselection.input;

import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

/**
 * Back key for BetterQuesting pages, registered under BetterQuesting's own key category so it
 * appears next to Open Quests. BetterQuesting hardcodes Backspace; this binding replaces it and
 * defaults to Backspace so behaviour stays the same until the player changes it.
 */
public final class BetterQuestingKeybinds {

    public static final KeyBinding BACK = new KeyBinding(
            "key.betterquesting.back", Keyboard.KEY_BACK, "BetterQuesting");

    /** Minecraft stores mouse buttons as {@code button - 100}, the same convention vanilla uses. */
    private static final int MOUSE_KEY_OFFSET = 100;

    private BetterQuestingKeybinds() {
    }

    public static boolean isBackKey(int keyCode) {
        int bound = BACK.getKeyCode();
        return bound != Keyboard.KEY_NONE && bound >= 0 && bound == keyCode;
    }

    public static boolean isBackMouseButton(int button) {
        int bound = BACK.getKeyCode();
        return bound < 0 && button >= 0 && bound == button - MOUSE_KEY_OFFSET;
    }
}
