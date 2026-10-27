package me.kuionc.crosshairselection.render;

public enum CrosshairType {

    CROSS("crosshairselection.type.cross"),
    CIRCLE("crosshairselection.type.circle"),
    SQUARE("crosshairselection.type.square"),
    DEFAULT("crosshairselection.type.default"),
    ARROW("crosshairselection.type.arrow"),
    TRIANGLE("crosshairselection.type.triangle");

    private final String translationKey;

    CrosshairType(String translationKey) {
        this.translationKey = translationKey;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public CrosshairType next() {
        CrosshairType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
