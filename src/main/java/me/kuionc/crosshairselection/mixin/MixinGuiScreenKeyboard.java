package me.kuionc.crosshairselection.mixin;

import me.kuionc.crosshairselection.compat.BetterQuestingCompat;
import me.kuionc.crosshairselection.input.BetterQuestingKeybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Replaces BetterQuesting's hardcoded Backspace return key on its own GUI canvases.
 *
 * <p>Every GUI key press passes through {@code GuiScreen.handleKeyboardInput}, whatever the screen
 * does with {@code allowUserInput}, and the binding is read from the raw event because vanilla
 * stops polling key bindings while a screen is open.
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreenKeyboard {

    @Inject(method = "handleKeyboardInput", at = @At("HEAD"), cancellable = true)
    private void crosshairselection$handleKeyboard(CallbackInfo ci) {
        if (!Keyboard.getEventKeyState()) {
            return;
        }
        int keyCode = Keyboard.getEventKey();
        GuiScreen screen = McRef.get().currentScreen;
        if (!BetterQuestingCompat.isCanvas(screen)) {
            return;
        }
        boolean backKey = BetterQuestingKeybinds.isBackKey(keyCode);
        boolean vanillaBack = keyCode == Keyboard.KEY_BACK;
        if (!backKey && !vanillaBack) {
            return;
        }
        // Let BetterQuesting see the key first (text fields, popups, its own back handling).
        boolean handled = BetterQuestingCompat.handlesKey(screen, Keyboard.getEventCharacter(), keyCode);
        if (backKey) {
            // Take the press over so BetterQuesting's hardcoded Backspace cannot return twice.
            ci.cancel();
            if (!handled) {
                BetterQuestingCompat.goBack(screen);
            }
        } else if (!handled) {
            // Backspace is no longer the back key: swallow it instead of firing BQ's hardcoded path.
            ci.cancel();
        }
    }
}
