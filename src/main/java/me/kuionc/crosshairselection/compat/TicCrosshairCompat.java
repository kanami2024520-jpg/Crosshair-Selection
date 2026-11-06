package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.config.CrosshairConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

/**
 * 可选的原版弓与匠魂弓/弩整合。
 *
 * <p>两件事：
 * <ul>
 *   <li>匠魂会从 Forge 的 CROSSHAIRS 事件自己画准星，这里决定是否让路；</li>
 *   <li>提供统一的蓄力进度，供准星形状与蓄力提示使用。</li>
 * </ul>
 */
public final class TicCrosshairCompat {

    private static final Class<?> IWINDUP = classOrNull("tconstruct.library.weaponry.IWindup");
    /** Wind-up accessors in the order they are tried; TiC changed the signature across versions. */
    private static final Method[] WINDUP_PROGRESS = {
            methodOrNull(IWINDUP, "getWindupProgress", ItemStack.class, EntityPlayer.class),
            methodOrNull(IWINDUP, "getWindupProgress", EntityPlayer.class, ItemStack.class),
            methodOrNull(IWINDUP, "getWindupProgress", ItemStack.class),
            methodOrNull(IWINDUP, "getWindupProgress", EntityPlayer.class),
            methodOrNull(IWINDUP, "getWindupProgress")
    };
    private static final Method MIN_WINDUP = methodOrNull(IWINDUP, "getMinWindupProgress", ItemStack.class);

    private TicCrosshairCompat() {
    }

    /**
     * True when TiC's own winding crosshair should be shown instead of this mod's crosshair.
     *
     * <p>By default the custom crosshair stays on, because the bow charge is rendered as the
     * crosshair spreading apart; TiC's crosshair is only used when explicitly asked for.
     */
    public static boolean shouldUseTicCrosshair(EntityPlayer player) {
        return !CrosshairConfig.ticKeepCustomCrosshair && isTicWindup(player);
    }

    public static boolean isTicWindup(EntityPlayer player) {
        ItemStack held = heldItem(player);
        return held != null && held.getItem() != null
                && held.getTagCompound() != null
                && IWINDUP != null && IWINDUP.isInstance(held.getItem());
    }

    /**
     * 是否手持弓类武器（原版弓或匠魂蓄力武器）。
     *
     * <p>匠魂武器在蓄力时不进入原版的 {@code isUsingItem()}，所以对匠魂只要求「拿着」。
     * 是否真的在蓄力由 {@link #progress} 决定，未拉时它返回 0。
     */
    public static boolean isDrawingBow(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        if (isTicWindup(player)) {
            return true;
        }
        ItemStack held = heldItem(player);
        return player.isUsingItem() && held != null && held.getItem() instanceof ItemBow;
    }

    /**
     * 蓄力进度，归一到 0..1。
     *
     * <p>1 表示<b>已经拉到可以射击</b>，不只是「进度条走完了」：原版弓按使用满 20 tick，
     * 匠魂按 {@code getMinWindupProgress} 归一化。取不到门槛时按 1.0 处理。
     *
     * @return 0 表示没在蓄力
     */
    public static float progress(EntityPlayer player) {
        if (!isDrawingBow(player)) {
            return 0.0F;
        }
        ItemStack held = heldItem(player);
        if (held == null || held.getItem() == null) {
            return 0.0F;
        }
        if (IWINDUP != null && IWINDUP.isInstance(held.getItem())) {
            float raw = windup(held, player);
            float min = minWindup(held);
            return clamp(raw / (min > 0.0F ? min : 1.0F));
        }
        if (held.getItem() instanceof ItemBow) {
            return clamp(player.getItemInUseDuration() / 20.0F);
        }
        return 0.0F;
    }

    /** 匠魂原始蓄力值；所有已知签名都失败时退回按使用时长估算。 */
    private static float windup(ItemStack held, EntityPlayer player) {
        for (Method method : WINDUP_PROGRESS) {
            if (method == null) {
                continue;
            }
            Object[] args = argsFor(method, held, player);
            if (args == null) {
                continue;
            }
            try {
                Object value = method.invoke(held.getItem(), args);
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                }
            } catch (Throwable ignored) {
                // 换下一个签名。
            }
        }
        return player.getItemInUseDuration() / 20.0F;
    }

    /** 匠魂满蓄力门槛；取不到时返回 0，调用方按 1.0 处理。 */
    private static float minWindup(ItemStack held) {
        if (MIN_WINDUP == null) {
            return 0.0F;
        }
        try {
            Object value = MIN_WINDUP.invoke(held.getItem(), held);
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
        } catch (Throwable ignored) {
            // 取不到就按 1.0 处理。
        }
        return 0.0F;
    }

    /** Builds the argument list for one wind-up signature, or null when it does not fit. */
    private static Object[] argsFor(Method method, ItemStack held, EntityPlayer player) {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (types[i] == ItemStack.class) {
                args[i] = held;
            } else if (EntityPlayer.class.isAssignableFrom(types[i])
                    || types[i].isAssignableFrom(EntityPlayer.class)) {
                args[i] = player;
            } else {
                return null;
            }
        }
        return args;
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
