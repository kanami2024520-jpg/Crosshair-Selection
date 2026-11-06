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

    /**
     * 当前绑定键码。
     *
     * <p>不能直接调用 {@code KeyBinding.getKeyCode()}：该方法的描述符在运行时会变化，
     * 调用会抛 NoSuchMethodError。这里通过 {@code @Accessor} 读字段。
     */
    public static int keyCode() {
        if (BACK instanceof me.kuionc.crosshairselection.mixin.AccessorKeyBinding) {
            return ((me.kuionc.crosshairselection.mixin.AccessorKeyBinding) BACK)
                    .crosshairselection$getKeyCode();
        }
        return Keyboard.KEY_NONE;
    }

    public static boolean isBackKey(int keyCode) {
        int bound = keyCode();
        return bound != Keyboard.KEY_NONE && bound >= 0 && bound == keyCode;
    }

    public static boolean isBackMouseButton(int button) {
        int bound = keyCode();
        return bound < 0 && button >= 0 && bound == button - MOUSE_KEY_OFFSET;
    }
}
