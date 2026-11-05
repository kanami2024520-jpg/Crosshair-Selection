package me.kuionc.crosshairselection.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection helpers for BetterQuesting's GUI canvases.
 *
 * <p>Every BetterQuesting class is looked up lazily through a class name, never touched at
 * startup, so a missing BetterQuesting cannot break mod loading.
 *
 * <p>{@code GuiScreenCanvas} stores the previous page in {@code previousScreen} and
 * {@code GuiContainerCanvas} only has {@code parent}. Returning uses whatever the screen actually
 * has, instead of BetterQuesting's own check that only looks at {@code parent}.
 */
public final class BetterQuestingCompat {

    private static final String SCREEN_CANVAS_NAME = "betterquesting.api2.client.gui.GuiScreenCanvas";
    private static final String CONTAINER_CANVAS_NAME = "betterquesting.api2.client.gui.GuiContainerCanvas";

    private static Class<?> screenCanvas;
    private static Class<?> containerCanvas;
    private static boolean resolved;

    private static Method screenKeyTyped;
    private static Method containerKeyTyped;
    private static Method screenClosePopup;
    private static Method containerClosePopup;

    private static Field screenPopup;
    private static Field containerPopup;
    private static Field screenParent;
    private static Field containerParent;
    private static Field screenPrevious;

    private BetterQuestingCompat() {
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        screenCanvas = classOrNull(SCREEN_CANVAS_NAME);
        containerCanvas = classOrNull(CONTAINER_CANVAS_NAME);

        screenKeyTyped = methodOrNull(screenCanvas, "onKeyTyped", char.class, int.class);
        containerKeyTyped = methodOrNull(containerCanvas, "onKeyTyped", char.class, int.class);
        screenClosePopup = methodOrNull(screenCanvas, "closePopup");
        containerClosePopup = methodOrNull(containerCanvas, "closePopup");

        screenPopup = fieldOrNull(screenCanvas, "popup");
        containerPopup = fieldOrNull(containerCanvas, "popup");
        screenParent = fieldOrNull(screenCanvas, "parent");
        containerParent = fieldOrNull(containerCanvas, "parent");
        screenPrevious = fieldOrNull(screenCanvas, "previousScreen");
    }

    public static boolean isScreenCanvas(GuiScreen screen) {
        resolve();
        return screen != null && screenCanvas != null && screenCanvas.isInstance(screen);
    }

    public static boolean isContainerCanvas(GuiScreen screen) {
        resolve();
        return screen != null && containerCanvas != null && containerCanvas.isInstance(screen);
    }

    public static boolean isCanvas(GuiScreen screen) {
        return isScreenCanvas(screen) || isContainerCanvas(screen);
    }

    /** Let BetterQuesting handle the key first so text fields and dialogs keep priority. */
    public static boolean handlesKey(GuiScreen screen, char typedChar, int keyCode) {
        resolve();
        Method method = isScreenCanvas(screen) ? screenKeyTyped
                : isContainerCanvas(screen) ? containerKeyTyped
                : null;
        if (method == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(method.invoke(screen, typedChar, keyCode));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** @return true when the screen is a BetterQuesting canvas that still wants the back key. */
    public static boolean handleBackKey(GuiScreen screen, char typedChar, int keyCode) {
        if (!isCanvas(screen)) {
            return false;
        }
        // Let BetterQuesting see the key first (text fields, popups, its own back handling).
        return !handlesKey(screen, typedChar, keyCode);
    }

    /** @return true when the screen is a BetterQuesting canvas and has been handled. */
    public static boolean goBack(GuiScreen screen) {
        resolve();
        if (!isCanvas(screen)) {
            return false;
        }
        try {
            Field popup = isScreenCanvas(screen) ? screenPopup : containerPopup;
            Method closePopup = isScreenCanvas(screen) ? screenClosePopup : containerClosePopup;
            if (popup != null && popup.get(screen) != null && closePopup != null) {
                closePopup.invoke(screen);
                return true;
            }

            Minecraft minecraft = Minecraft.getMinecraft();
            GuiScreen target = null;
            if (isScreenCanvas(screen) && screenPrevious != null) {
                target = (GuiScreen) screenPrevious.get(screen);
            }
            if (target == null) {
                Field parent = isScreenCanvas(screen) ? screenParent : containerParent;
                if (parent != null) {
                    target = (GuiScreen) parent.get(screen);
                }
            }
            minecraft.displayGuiScreen(target);
            if (target == null) {
                minecraft.setIngameFocus();
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field fieldOrNull(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
