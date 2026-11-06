package me.kuionc.crosshairselection.config;

/**
 * 拉弓时的提示方式。
 */
public enum BowChargeIndicator {
    /** 不显示额外提示；准星本身的收缩已经表达进度。 */
    NONE("none"),
    /** 准星旁的百分比数字。 */
    NUMBER("number"),
    /** 准星旁的向上箭头，随蓄力变大变亮。 */
    ARROW("arrow");

    private final String key;

    BowChargeIndicator(String key) {
        this.key = key;
    }

    public String getTranslationKey() {
        return "crosshairselection.bow_indicator." + key;
    }

    public BowChargeIndicator next() {
        BowChargeIndicator[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
