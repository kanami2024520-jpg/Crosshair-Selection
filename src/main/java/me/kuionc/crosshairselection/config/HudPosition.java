package me.kuionc.crosshairselection.config;

/**
 * HUD 元素相对准星的位置。
 *
 * <p>用八个方位而不是任意坐标：偏移相对屏幕中心计算，
 * 在任意分辨率与 GUI 缩放下都一致，也不会被屏幕边缘裁掉。
 */
public enum HudPosition {
    ABOVE("above"),
    BELOW("below"),
    LEFT("left"),
    RIGHT("right"),
    ABOVE_LEFT("above_left"),
    ABOVE_RIGHT("above_right"),
    BELOW_LEFT("below_left"),
    BELOW_RIGHT("below_right");

    private final String key;

    HudPosition(String key) {
        this.key = key;
    }

    public String getTranslationKey() {
        return "crosshairselection.hud_position." + key;
    }

    public HudPosition next() {
        HudPosition[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /** 水平方向：-1 左、0 居中、1 右。 */
    public int horizontal() {
        switch (this) {
            case LEFT:
            case ABOVE_LEFT:
            case BELOW_LEFT:
                return -1;
            case RIGHT:
            case ABOVE_RIGHT:
            case BELOW_RIGHT:
                return 1;
            default:
                return 0;
        }
    }

    /** 垂直方向：-1 上、0 居中、1 下。 */
    public int vertical() {
        switch (this) {
            case ABOVE:
            case ABOVE_LEFT:
            case ABOVE_RIGHT:
                return -1;
            case BELOW:
            case BELOW_LEFT:
            case BELOW_RIGHT:
                return 1;
            default:
                return 0;
        }
    }
}
