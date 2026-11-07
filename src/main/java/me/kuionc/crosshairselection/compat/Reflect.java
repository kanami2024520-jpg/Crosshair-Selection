package me.kuionc.crosshairselection.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 兼容层共用的反射工具。
 *
 * <p>这些部件/模组都是可选依赖，运行时可能不存在，所以统一用「拿不到就返回 null」的写法，
 * 调用方自己决定降级路径。以前每个 compat provider 都抄了一份同样的四个方法，
 * 合并到这里，避免各处的行为慢慢漂移。
 */
public final class Reflect {

    private Reflect() {
    }

    public static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Method methodOrNull(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Constructor<?> constructorOrNull(Class<?> owner, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getConstructor(params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Field fieldOrNull(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            Field field = owner.getField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
