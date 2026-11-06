package me.kuionc.crosshairselection.util;

import net.minecraft.client.Minecraft;

/**
 * 全局 {@link Minecraft} 实例缓存。
 *
 * <p>实例由 {@code MixinEntityRenderer} 通过 {@code @Accessor("mc")} 取得后写入这里。
 * 本类<b>绝不</b>调用 {@code Minecraft.getMinecraft()}：该方法的描述符在运行时与编译期不一致，
 * 调用会抛 {@code NoSuchMethodError}，而异常发生在渲染回调里只会表现为画面卡住。
 */
public final class McRef {

    private static Minecraft instance;
    private static boolean reported;

    private McRef() {
    }

    /** 由 mixin 每帧写入。 */
    public static void setInstance(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        instance = minecraft;
        if (!reported) {
            reported = true;
            System.err.println("[Crosshair-Selection] client instance captured");
        }
    }

    /** 当前实例；首帧之前为 null，调用方必须判空。 */
    public static Minecraft get() {
        return instance;
    }
}
