package me.kuionc.crosshairselection.gui;

/**
 * Shared geometry for every settings page.
 *
 * <p>All pages place controls on the same columns and rows, so moving between them does not make
 * the buttons jump. The preview panel is centred and narrow enough that both columns stay clear of
 * it on normal window sizes.
 */
public final class SettingsLayout {

    /** Top of the control area, below the tab row. */
    public static final int CONTENT_TOP = 46;
    /** Distance between the tops of two consecutive rows. */
    public static final int ROW_STEP = 20;
    /** Height of a standard button or slider. */
    public static final int ROW_HEIGHT = 18;
    /** Width of one column. */
    public static final int COLUMN_WIDTH = 150;
    /** Space between the screen edge and the left column. */
    public static final int MARGIN = 4;

    private SettingsLayout() {
    }

    public static int leftColumn(int screenWidth) {
        return MARGIN;
    }

    public static int rightColumn(int screenWidth) {
        return screenWidth - COLUMN_WIDTH - MARGIN;
    }

    /**
     * True when the window is too narrow for two columns next to the centred preview; pages then
     * fall back to a single column.
     */
    public static boolean singleColumn(int screenWidth) {
        return screenWidth < COLUMN_WIDTH * 2 + 160;
    }

    /** Y of row {@code index}, starting at {@link #CONTENT_TOP}. */
    public static int row(int index) {
        return CONTENT_TOP + index * ROW_STEP;
    }

    /** Bottom row used by the save/back/defaults buttons. */
    public static int footer(int screenHeight) {
        return screenHeight - 26;
    }
}
