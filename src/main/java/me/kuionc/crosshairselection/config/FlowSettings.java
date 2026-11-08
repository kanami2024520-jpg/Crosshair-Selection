package me.kuionc.crosshairselection.config;

/**
 * Shared parameters for the flowing rainbow and the sweeping glint.
 *
 * <p>Each renderer owns an instance, so the selection box and the crosshair are configured
 * separately. The values are plain fields so the config screen can edit them live and the
 * shaders can read them every frame.
 */
public final class FlowSettings {

    /** Time curve used to advance the phase. */
    public enum Motion {
        LINEAR,
        COSINE,
        TRIANGLE,
        NOISE
    }

    /** Where the flow starts and which way it travels. */
    public enum Direction {
        FORWARD,
        REVERSE,
        CORNER_TO_CORNER,
        CENTER_OUT
    }

    /** How the colour is laid out. */
    public enum Mode {
        OFF,
        FLOW,
        SWEEP
    }

    /**
     * 新实例的流彩默认开启（选框）。
     *
     * <p>不要为了「默认关」再加字段：GTNHLib 会把任意字段都当成配置项写进 cfg。
     * 准星用 {@link #defaultOff()}。
     */
    public FlowSettings() {
    }

    /** 新实例的流彩默认关闭（准星保持纯色）。 */
    public static FlowSettings defaultOff() {
        FlowSettings settings = new FlowSettings();
        settings.mode = Mode.OFF;
        return settings;
    }

    // Colour
    // 默认是暖色（橙）到青色的一段窄色带，仍是整条彩虹编辑器的子集，
    // 在流光编辑页里表现为一条已选中的区间。
    public float hueStart = 0.26F;
    public float hueRange = 0.32F;
    public float saturation = 0.49F;
    public float brightness = 0.91F;
    /** 整体透明度倍率，乘在基色 alpha 上；1.0 表示不改变。 */
    public float opacity = 1.0F;

    // Motion
    public float speed = 500.0F;
    public Direction direction = Direction.FORWARD;
    /** Corner indices 0..7 for {@link Direction#CORNER_TO_CORNER}; bit 0 = +X, 1 = +Y, 2 = +Z. */
    public int startCorner = 0;
    public int endCorner = 7;
    public Motion motion = Motion.COSINE;
    /** Colour bands per 100 pixels (crosshair) or per block (selection box). */
    public float bandDensity = 0.35F;


    // Sweep
    public Mode mode = Mode.FLOW;
    /** Manual sweep position; the only source now that break progress has its own overlay. */
    public float progress = 1.0F;
    /** Softness of the sweep edge. */
    public float sweepSoftness = 0.35F;

    /** 恢复出厂默认值。 */
    public void reset() {
        resetToDefault();
    }

    /** 出厂默认值：暖色窄色带、余弦运动。设置页的「重置」用它。 */
    public void resetToDefault() {
        hueStart = 0.26F;
        hueRange = 0.32F;
        saturation = 0.49F;
        brightness = 0.91F;
        opacity = 1.0F;
        speed = 500.0F;
        direction = Direction.FORWARD;
        startCorner = 0;
        endCorner = 7;
        motion = Motion.COSINE;
        bandDensity = 0.35F;
        mode = Mode.FLOW;
        progress = 1.0F;
        sweepSoftness = 0.35F;
    }

    public boolean isActive() {
        return mode != Mode.OFF;
    }

    public static <T extends Enum<T>> T cycle(T[] values, T current) {
        return values[(current.ordinal() + 1) % values.length];
    }

    public FlowSettings copyFrom(FlowSettings other) {
        hueStart = other.hueStart;
        hueRange = other.hueRange;
        saturation = other.saturation;
        brightness = other.brightness;
        opacity = other.opacity;
        speed = other.speed;
        direction = other.direction;
        startCorner = other.startCorner;
        endCorner = other.endCorner;
        motion = other.motion;
        bandDensity = other.bandDensity;
        mode = other.mode;
        progress = other.progress;
        sweepSoftness = other.sweepSoftness;
        return this;
    }
}
