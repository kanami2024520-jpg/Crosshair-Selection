package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.util.McRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;

import java.util.ArrayList;
import java.util.List;

/**
 * 流式布局（Flow Layout）。
 *
 * <p>控件按登记顺序从左到右排列，一行放不下就换到下一行；左右两列各自独立，互不影响。
 * 页面只需声明控件宽度，不用再手算每个 y 坐标，也就不会出现常量偏移写错导致的整段空白。
 *
 * <p>两种「不显示」要区分清楚：
 * <ul>
 *   <li><b>禁用</b>：条件不满足但仍占位，画成灰色（由 {@link SettingsScreen} 处理）；</li>
 *   <li><b>隐藏</b>：完全不参与布局，也不绘制。</li>
 * </ul>
 *
 * <p>尺寸按屏幕比例推导，保证不同分辨率与 GUI 缩放下密度一致。
 */
public final class FlowLayout {

    /** 控件高度占行距的比例，剩下的是行间空隙。 */
    private static final float HEIGHT_RATIO = 0.9F;
    /** 压缩下限：再低按钮就点不准了。 */
    private static final int MIN_ROW_HEIGHT = 14;

    private final int originX;
    private final int maxWidth;
    private int rowHeight;

    private int cursorX;
    private int cursorY;
    private int lineHeight;
    /** 每一行的起始 y，压缩时按比例重算。 */
    private final List<Integer> lineTops = new ArrayList<Integer>();
    private int startY;

    private final List<GuiButton> placed = new ArrayList<GuiButton>();

    /**
     * @param originX   列左边界
     * @param startY    第一行顶部
     * @param maxWidth  列宽，超出即换行
     * @param rowHeight 行距
     */
    public FlowLayout(int originX, int startY, int maxWidth, int rowHeight) {
        this.originX = originX;
        this.maxWidth = Math.max(60, maxWidth);
        this.rowHeight = Math.max(12, rowHeight);
        this.startY = startY;
        this.cursorY = startY;
        this.lineTops.add(startY);
    }

    /** 控件高度：行距的九成。 */
    public int controlHeight() {
        return Math.round(rowHeight * HEIGHT_RATIO);
    }

    /** 列宽。 */
    public int width() {
        return maxWidth;
    }

    /** 行距。 */
    public int rowHeight() {
        return rowHeight;
    }

    /**
     * 放置一个控件。
     *
     * @param button 控件
     * @param width  希望占用的宽度；换行后会被夹到剩余宽度
     * @return 传入的控件，便于链式调用
     */
    public GuiButton place(GuiButton button, int width) {
        return place(button, width, null);
    }

    /**
     * 放置一个控件，并指定「是否隐藏」。
     *
     * <p>隐藏的控件既不出现在界面里，也不占布局位置；禁用的控件仍会占位并显示为灰色，
     * 那由 {@link SettingsScreen} 的依赖条件负责。
     *
     * @param visible true 表示显示；false 表示整块隐藏
     */
    public GuiButton place(GuiButton button, int width, BooleanSupplier visible) {
        if (button == null) {
            return null;
        }
        if (visible != null && !visible.get()) {
            button.visible = false;
            return button;
        }
        int w = Math.min(Math.max(width, 40), maxWidth);
        if (cursorX > 0 && cursorX + w > maxWidth) {
            newLine();
        }
        if (w > maxWidth - cursorX) {
            w = maxWidth - cursorX;
        }
        button.xPosition = originX + cursorX;
        button.yPosition = cursorY;
        button.width = w;
        button.height = controlHeight();
        cursorX += w + gap();
        lineHeight = Math.max(lineHeight, controlHeight());
        placed.add(button);
        return button;
    }

    /** 占满整行的控件。 */
    public GuiButton placeWide(GuiButton button) {
        return place(button, maxWidth);
    }

    /** 结束当前行。 */
    public void newLine() {
        cursorX = 0;
        cursorY += rowHeight;
        lineHeight = 0;
        lineTops.add(cursorY);
    }

    /** 分组间隔：换行并额外留半行空白。 */
    public void group() {
        newLine();
        cursorY += Math.round(rowHeight * 0.5F);
    }

    /**
     * 把整列压缩到可用高度内。
     *
     * <p>内容比可用空间高时按比例缩行距，并同步移动已放置的控件；
     * 控件高度也跟着缩，避免两行叠在一起。缩到最小行距仍放不下时保持最小行距，
     * 由页面自行决定是否继续拆分内容。
     *
     * @param availableTop    可用区域顶部
     * @param availableBottom 可用区域底部（不含页脚按钮）
     */
    public void fitTo(int availableTop, int availableBottom) {
        int available = Math.max(1, availableBottom - availableTop);
        int rows = Math.max(1, lineTops.size());
        int needed = rows * rowHeight;
        if (needed <= available) {
            return;
        }
        int newRowHeight = Math.max(MIN_ROW_HEIGHT, available / rows);
        if (newRowHeight >= rowHeight) {
            return;
        }
        // 按行号线性重算每行顶部，再把控件搬到新位置。
        int[] newTops = new int[rows];
        for (int i = 0; i < rows; i++) {
            newTops[i] = availableTop + i * newRowHeight;
        }
        int oldRowHeight = rowHeight;
        rowHeight = newRowHeight;
        for (GuiButton button : placed) {
            // 用控件原来的 y 反查行号，再映射到新行顶。
            int index = Math.round((button.yPosition - this.startY) / (float) oldRowHeight);
            index = Math.max(0, Math.min(rows - 1, index));
            button.yPosition = newTops[index];
            button.height = controlHeight();
        }
        this.startY = availableTop;
        this.cursorY = availableTop + rows * newRowHeight;
        this.lineHeight = 0;
    }

    /** 已排到的底部 y（不含行高）。 */
    public int bottom() {
        return cursorY + lineHeight;
    }

    /** 已放置的控件。 */
    public List<GuiButton> placed() {
        return placed;
    }

    private int gap() {
        return Math.max(2, Math.round(rowHeight * 0.1F));
    }

    /** 极简的布尔供给器，避免布局类依赖 GUI 内部接口。 */
    public interface BooleanSupplier {
        boolean get();
    }

    // ------------------------------------------------------------------
    // 屏幕比例计算
    // ------------------------------------------------------------------

    /** 当前 GUI 缩放。 */
    public static int guiScale() {
        Minecraft minecraft = McRef.get();
        if (minecraft == null) {
            return 2;
        }
        ScaledResolution resolution = new ScaledResolution(minecraft,
                minecraft.displayWidth, minecraft.displayHeight);
        return Math.max(1, resolution.getScaleFactor());
    }

    /**
     * 行距：固定物理像素换算成 GUI 像素，缩放变化时视觉密度保持一致。
     */
    public static int rowHeightFor(int guiScale) {
        int scale = Math.max(1, guiScale);
        int physical = 20 * Math.max(2, scale);
        return Math.max(12, Math.round(physical / (float) scale));
    }

    /** 单列宽度：屏幕宽度的固定比例，夹在合理区间。 */
    public static int columnWidth(int screenWidth) {
        return Math.max(110, Math.min(190, Math.round(screenWidth * 0.22F)));
    }

    /** 预览面板左右需要预留的空白。 */
    public static int previewHalfWidth(int screenWidth) {
        return Math.max(74, Math.round(screenWidth * 0.09F));
    }

    /** 内容窄到只能排一列。 */
    public static boolean singleColumn(int screenWidth, int columnWidth) {
        return screenWidth < columnWidth * 2 + previewHalfWidth(screenWidth) * 2 + 24;
    }

    /** 内容区顶部：标题与页签之下。 */
    public static int contentTop(int screenHeight) {
        return Math.max(40, Math.round(screenHeight * 0.075F));
    }

    /** 页签高度。 */
    public static int tabHeight(int screenHeight) {
        return Math.max(16, Math.round(screenHeight * 0.035F));
    }

    /** 底部按钮行的 y。 */
    public static int footerTop(int screenHeight) {
        return screenHeight - Math.max(24, Math.round(screenHeight * 0.045F)) - 4;
    }
}
