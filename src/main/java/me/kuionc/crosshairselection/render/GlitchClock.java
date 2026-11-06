package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.config.FlowSettings;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 「量子闪烁」（GLITCH）模式的时间轴。
 *
 * <p>与其它运动算法不同，它不能只靠 {@code now} 直接算相位：状态机里存在
 * 「速度归零（卡顿）」和「速度反转（倒流）」两种状态，必须把速度<b>积分</b>到相位上，
 * 否则归零只会让颜色跳到固定值，而不是真的停住。
 *
 * <p>状态按固定步长切换（{@link #STEP_MILLIS}），同一段时间内速度不变：
 * <ul>
 *   <li>15% 概率速度为 0（卡顿）；</li>
 *   <li>15% 概率速度为负并加速（倒流）；</li>
 *   <li>其余时间正向流动。</li>
 * </ul>
 *
 * <p>每个 {@link FlowSettings} 实例各有一份状态，所以选框和准星互不影响。
 * 设置预览与着色器共用这份相位，观感才能一致。
 */
final class GlitchClock {

    /** 状态机的离散步长（毫秒）。 */
    static final long STEP_MILLIS = 400L;

    private static final Map<FlowSettings, State> STATES =
            new WeakHashMap<FlowSettings, State>();

    private GlitchClock() {
    }

    private static final class State {
        long lastMillis;
        float phase;
    }

    /** 当前离散步长序号，供着色器做撕裂分区用。 */
    static float step(long now) {
        return (float) Math.floor(now / (double) STEP_MILLIS);
    }

    /** 积分后的相位，范围 0..1。 */
    static float phase(FlowSettings flow, long now) {
        State state;
        synchronized (STATES) {
            state = STATES.get(flow);
            if (state == null) {
                state = new State();
                state.lastMillis = now;
                STATES.put(flow, state);
            }
        }
        long elapsed = now - state.lastMillis;
        if (elapsed < 0L) {
            // 时间回退（切换世界/系统时间变化）时重新起步。
            state.phase = 0.0F;
            state.lastMillis = now;
            return 0.0F;
        }
        if (elapsed > 0L) {
            double cyclesPerSecond = Math.max(flow.speed, 0.0F) / 1000.0D;
            float speed = speedFor(now);
            state.phase = (float) fract(state.phase + speed * cyclesPerSecond * elapsed / 1000.0D);
            state.lastMillis = now;
        }
        return state.phase;
    }

    /** 当前步长的速度倍率：0 卡顿、负数倒流并加速、1 正常。 */
    static float speedFor(long now) {
        long step = now / STEP_MILLIS;
        float roll = hash(step);
        if (roll < 0.15F) {
            return 0.0F;
        }
        if (roll < 0.30F) {
            return -(1.5F + hash(step * 31L + 7L) * 1.5F);
        }
        return 1.0F;
    }

    private static double fract(double value) {
        return value - Math.floor(value);
    }

    /** 稳定、无分配的整数哈希，返回 0..1。 */
    private static float hash(long value) {
        long x = value;
        x = (x ^ (x >>> 33)) * 0xff51afd7ed558ccdL;
        x = (x ^ (x >>> 33)) * 0xc4ceb9fe1a85ec53L;
        x = x ^ (x >>> 33);
        return (float) ((x >>> 40) / (double) (1L << 24));
    }
}
