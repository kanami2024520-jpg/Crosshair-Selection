package me.kuionc.crosshairselection.config;

/**
 * How the crosshair reacts to attacking and placing.
 */
public enum FeedbackStyle {
    /** Bow-like behaviour: the arms separate and settle back, on both attack and placement. */
    SPREAD("spread"),
    /** No reaction at all. */
    NONE("none");

    private final String key;

    FeedbackStyle(String key) {
        this.key = key;
    }

    public String getTranslationKey() {
        return "crosshairselection.feedback_style." + key;
    }

    public FeedbackStyle next() {
        FeedbackStyle[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
