package me.kuionc.crosshairselection.config;

public enum GtXStyle {
    CROSS("crosshairselection.gt_x_style.cross"),
    ARROW("crosshairselection.gt_x_style.arrow"),
    FILL("crosshairselection.gt_x_style.fill"),
    FULL_X("crosshairselection.gt_x_style.full_x");

    private final String translationKey;

    GtXStyle(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public GtXStyle next() {
        GtXStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
