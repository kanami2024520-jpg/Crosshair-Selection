package me.kuionc.crosshairselection.render;

public enum CrosshairType {

    CROSS("crosshairselection.type.cross", true),
    CIRCLE("crosshairselection.type.circle", false),
    SQUARE("crosshairselection.type.square", false),
    DEFAULT("crosshairselection.type.default", true),
    ARROW("crosshairselection.type.arrow", false),
    TRIANGLE("crosshairselection.type.triangle", false);

    private final String translationKey;
    private final boolean selectable;

    CrosshairType(String translationKey, boolean selectable) {
        this.translationKey = translationKey;
        this.selectable = selectable;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public CrosshairType next() {
        CrosshairType[] values = values();
        for (int i = 1; i <= values.length; i++) {
            CrosshairType candidate = values[(ordinal() + i) % values.length];
            if (candidate.selectable) {
                return candidate;
            }
        }
        return CROSS;
    }

    public static CrosshairType sanitize(CrosshairType type) {
        return type != null && type.selectable ? type : CROSS;
    }
}
