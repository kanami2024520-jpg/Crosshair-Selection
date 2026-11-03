package me.kuionc.crosshairselection.compat;

import net.minecraft.util.MovingObjectPosition;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reads the part identifier carried by Forge Multipart / CodeChickenLib ray traces.
 * ExtendedMOP stores it in its {@code data} field; FMP wraps the part index in a Scala Tuple2.
 */
public final class HitData {

    private static final Class<?> EXTENDED_MOP = classOrNull(
            "codechicken.lib.raytracer.ExtendedMOP");
    private static final Field DATA = fieldOrNull(EXTENDED_MOP, "data");
    private static final Class<?> TUPLE2 = classOrNull("scala.Tuple2");
    private static final Method TUPLE_FIRST = methodOrNull(TUPLE2, "_1");

    private HitData() {
    }

    public static Object raw(MovingObjectPosition target) {
        if (target == null) {
            return null;
        }
        if (EXTENDED_MOP != null && EXTENDED_MOP.isInstance(target) && DATA != null) {
            try {
                Object data = DATA.get(target);
                if (data != null) {
                    return data;
                }
            } catch (Throwable ignored) {
                // Fall through to subHit.
            }
        }
        return target.subHit;
    }

    public static int partIndex(MovingObjectPosition target) {
        Object data = raw(target);
        if (data instanceof Number) {
            return ((Number) data).intValue();
        }
        if (data != null && TUPLE2 != null && TUPLE2.isInstance(data) && TUPLE_FIRST != null) {
            try {
                Object first = TUPLE_FIRST.invoke(data);
                if (first instanceof Number) {
                    return ((Number) first).intValue();
                }
            } catch (Throwable ignored) {
                // Not a usable tuple.
            }
        }
        return -1;
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field fieldOrNull(Class<?> owner, String name) {
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

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... args) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
