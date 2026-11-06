package me.kuionc.crosshairselection.config;

/**
 * 破坏进度的显示方式。
 */
public enum BreakProgressStyle {
    /** 方块面上从下往上上涨的填充条，世界空间。 */
    BAR("bar"),
    /** 准星下方的百分比数字，HUD 空间。 */
    NUMBER("number");

    private final String key;

    BreakProgressStyle(String key) {
        this.key = key;
    }

    public String getTranslationKey() {
        return "crosshairselection.break_style." + key;
    }

    public boolean showsBar() {
        return this == BAR;
    }

    public boolean showsNumber() {
        return this == NUMBER;
    }

    public BreakProgressStyle next() {
        BreakProgressStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
