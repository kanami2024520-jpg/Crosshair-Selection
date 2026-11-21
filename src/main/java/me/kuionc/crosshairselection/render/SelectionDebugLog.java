package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.CrosshairSelection;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;

/**
 * 选框诊断日志：只在内容发生变化时写一行，并带上距上一条日志的时间差。
 *
 * <p>由 {@code selection_box.debugTargetSelection} 控制，默认关闭。
 * 不按固定间隔采样，避免抖动周期较长时漏掉中间状态。
 */
public final class SelectionDebugLog {

    /** 单次会话的最大行数，避免日志被刷到几个 G。 */
    private static final int LIMIT = 5000;

    private static int lines;
    private static long lastMillis;

    private SelectionDebugLog() {
    }

    public static boolean enabled() {
        return SelectionBoxConfig.debugTargetSelection;
    }

    public static void log(String category, String message) {
        if (!enabled()) {
            return;
        }
        if (lines >= LIMIT) {
            if (lines == LIMIT) {
                lines++;
                // 用 mod 的 log4j logger：System.err 在 PrismLauncher + lwjgl3ify 下不会进 latest.log。
                CrosshairSelection.LOGGER.info("[CS-" + category + "] log limit reached");
            }
            return;
        }
        long now = System.currentTimeMillis();
        long delta = lastMillis == 0L ? 0L : now - lastMillis;
        lastMillis = now;
        lines++;
        CrosshairSelection.LOGGER.info("[CS-" + category + "] +" + delta + "ms " + message);
    }

    public static void reset() {
        lines = 0;
        lastMillis = 0L;
    }
}
