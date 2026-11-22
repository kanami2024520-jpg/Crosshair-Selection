package me.kuionc.crosshairselection.config;

/**
 * 选框切换时的过渡方式。
 */
public enum TransitionMode {
    /** 形状相同则滑动；形状不同则旧形状原位淡出、新形状淡入。 */
    CROSSFADE("crossfade"),
    /** 永远从旧位置滑动到新位置（旧行为）。 */
    SLIDE("slide");

    private final String key;

    TransitionMode(String key) {
        this.key = key;
    }

    public String getTranslationKey() {
        return "crosshairselection.transition_mode." + key;
    }

    public TransitionMode next() {
        TransitionMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
