package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.config.CrosshairConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Optional Tinkers' Construct bow/crossbow integration. TiC draws its own crosshair from a
 * Forge CROSSHAIRS event, so this only decides whether that event should be allowed through and
 * how much wind-up progress our own crosshair should display.
 */
public final class TicCrosshairCompat {

    private static final Class<?> IWINDUP = classOrNull("tconstruct.library.weaponry.IWindup");
    private static final Method GET_WINDUP_PROGRESS = methodOrNull(IWINDUP, "getWindupProgress",
            ItemStack.class, EntityPlayer.class);

    private TicCrosshairCompat() {
    }

    public static boolean shouldUseTicCrosshair(EntityPlayer player) {
        return CrosshairConfig.useTicBowCrosshair && isTicWindup(player);
    }

    public static boolean isTicWindup(EntityPlayer player) {
        ItemStack held = heldItem(player);
        return held != null && held.getItem() != null
                && held.getTagCompound() != null
                && IWINDUP != null && IWINDUP.isInstance(held.getItem());
    }

    public static boolean isDrawingBow(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        // TiC draws its wind-up crosshair from the held item state, not vanilla isUsingItem().
        if (isTicWindup(player)) {
            return true;
        }
        if (!player.isUsingItem()) {
            return false;
        }
        ItemStack held = heldItem(player);
        return held != null && held.getItem() instanceof ItemBow;
    }

    /**
     * @return 0 at the start of the draw, 1 when fully drawn; 0 when not drawing.
     */
    public static float progress(EntityPlayer player) {
        if (!isDrawingBow(player)) {
            return 0.0F;
        }
        ItemStack held = heldItem(player);
        if (held == null || held.getItem() == null) {
            return 0.0F;
        }

        if (IWINDUP != null && GET_WINDUP_PROGRESS != null && IWINDUP.isInstance(held.getItem())) {
            try {
                Object value = GET_WINDUP_PROGRESS.invoke(held.getItem(), held, player);
                if (value instanceof Number) {
                    return clamp(((Number) value).floatValue());
                }
            } catch (Throwable ignored) {
                // Fall through to the vanilla-bow calculation / no progress.
            }
        }

        if (held.getItem() instanceof ItemBow) {
            return clamp(player.getItemInUseDuration() / 20.0F);
        }
        return 0.0F;
    }

    private static ItemStack heldItem(EntityPlayer player) {
        return player == null ? null : player.getHeldItem();
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... parameterTypes) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
