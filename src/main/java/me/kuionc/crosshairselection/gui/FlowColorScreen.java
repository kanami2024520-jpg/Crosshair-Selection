package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.gui.widgets.HsvSquare;
import me.kuionc.crosshairselection.gui.widgets.ValueBar;
import me.kuionc.crosshairselection.render.FlowPreview;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;

/**
 * Visual editor for the flowing gradient.
 *
 * <p>Left column: hue strip, live gradient preview, brightness/opacity/density bars and
 * read-outs. Right column: a large rectangular saturation/brightness square.
 */
public class FlowColorScreen extends GuiScreen {

    private static final int BACK = 1;
    private static final int RESET = 2;

    private static final int STRIP_HEIGHT = 28;
    private static final int HANDLE_SIZE = 6;
    private static final int BAR_HEIGHT = 16;
    private static final int BAR_STEP = 30;
    private static final int BAR_COUNT = 3;

    private final GuiScreen parent;
    private final FlowSettings flow;
    private final boolean crosshair;
    private boolean childOpen;

    private int stripX;
    private int stripY;
    private int stripWidth;
    private int previewX;
    private int previewWidth;
    private int pickerX;
    private int pickerY;
    private int pickerWidth;
    private int pickerHeight;
    private int barX;
    private int barWidth;
    private int barY;
    private int readoutX;
    private int readoutY;

    private enum Drag {
        NONE,
        START,
        END,
        SQUARE,
        BRIGHTNESS,
        OPACITY,
        DENSITY
    }

    private Drag dragging = Drag.NONE;

    public FlowColorScreen(GuiScreen parent, FlowSettings flow, boolean crosshair) {
        this.parent = parent;
        this.flow = flow;
        this.crosshair = crosshair;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        normaliseHueRange();

        int margin = 16;
        stripX = margin;
        stripWidth = Math.max(80, width - 2 * margin);
        stripY = 40;
        previewX = stripX;
        previewWidth = stripWidth;

        int contentTop = stripY + STRIP_HEIGHT + 52;
        // 右侧一整块长方形色盘。
        pickerWidth = Math.min(360, Math.max(160, Math.round(width * 0.42F)));
        pickerHeight = Math.min(260, Math.max(120, height - contentTop - 56));
        pickerX = width - margin - pickerWidth;
        pickerY = contentTop;

        // 左列：三条拖动条 + 读数，宽度吃掉色盘左边的全部空间。
        barX = stripX;
        barWidth = Math.max(100, pickerX - 24 - barX);
        barY = contentTop;
        readoutX = barX;
        readoutY = barY + BAR_COUNT * BAR_STEP + 8;

        buttonList.add(new GuiButton(RESET, width / 2 - 105, height - 26, 100, 20,
                StatCollector.translateToLocal("crosshairselection.flow_colour.reset")));
        buttonList.add(new GuiButton(BACK, width / 2 + 5, height - 26, 100, 20,
                StatCollector.translateToLocal("crosshairselection.back")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == RESET) {
            flow.resetToDefault();
        } else {
            mc.displayGuiScreen(parent);
        }
    }

    private int startHandleX() {
        return stripX + Math.round(flow.hueStart * stripWidth);
    }

    private int endHandleX() {
        return stripX + Math.round((flow.hueStart + flow.hueRange) * stripWidth);
    }

    private int barTop(int index) {
        return barY + index * BAR_STEP;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }
        if (mouseY >= stripY - HANDLE_SIZE && mouseY <= stripY + STRIP_HEIGHT + HANDLE_SIZE) {
            // 整条色带都可以点：按到两个把手的距离选择拖动起点/终点，点哪跳哪。
            if (Math.abs(mouseX - startHandleX()) <= Math.abs(mouseX - endHandleX())) {
                dragging = Drag.START;
                updateStart(mouseX);
            } else {
                dragging = Drag.END;
                updateEnd(mouseX);
            }
            return;
        }
        if (inside(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
            dragging = Drag.SQUARE;
            updateSquare(mouseX, mouseY);
            return;
        }
        for (int i = 0; i < BAR_COUNT; i++) {
            if (inside(mouseX, mouseY, barX, barTop(i), barWidth, BAR_HEIGHT)) {
                dragging = barDrag(i);
                updateBar(i, mouseX);
                return;
            }
        }
    }

    private static Drag barDrag(int index) {
        switch (index) {
            case 0:
                return Drag.BRIGHTNESS;
            case 1:
                return Drag.OPACITY;
            default:
                return Drag.DENSITY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, button, timeSinceLastClick);
        switch (dragging) {
            case START:
                updateStart(mouseX);
                break;
            case END:
                updateEnd(mouseX);
                break;
            case SQUARE:
                updateSquare(mouseX, mouseY);
                break;
            case BRIGHTNESS:
                updateBar(0, mouseX);
                break;
            case OPACITY:
                updateBar(1, mouseX);
                break;
            case DENSITY:
                updateBar(2, mouseX);
                break;
            default:
                break;
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
        if (button == 0) {
            dragging = Drag.NONE;
        }
    }

    /**
     * 把色相区间钳在 0..1 之内。
     */
    private void normaliseHueRange() {
        flow.hueStart = flow.hueStart - (float) Math.floor(flow.hueStart);
        float maxRange = Math.max(0.01F, 1.0F - flow.hueStart);
        flow.hueRange = Math.max(0.01F, Math.min(flow.hueRange, maxRange));
    }

    private void updateStart(int mouseX) {
        float value = clamp01((mouseX - stripX) / (float) stripWidth);
        // 终点保持不动，由起点反推范围。
        float end = flow.hueStart + flow.hueRange;
        flow.hueStart = Math.max(0.0F, Math.min(value, end - 0.01F));
        flow.hueRange = Math.max(0.01F, end - flow.hueStart);
    }

    private void updateEnd(int mouseX) {
        float value = clamp01((mouseX - stripX) / (float) stripWidth);
        float maxRange = Math.max(0.01F, 1.0F - flow.hueStart);
        flow.hueRange = Math.max(0.01F, Math.min(value - flow.hueStart, maxRange));
    }

    private void updateSquare(int mouseX, int mouseY) {
        float[] pick = HsvSquare.pick(pickerX, pickerY, pickerWidth, pickerHeight, mouseX, mouseY);
        flow.saturation = pick[0];
        flow.brightness = pick[1];
    }

    private void updateBar(int index, int mouseX) {
        float value = ValueBar.pick(barX, barWidth, mouseX);
        switch (index) {
            case 0:
                flow.brightness = value;
                break;
            case 1:
                flow.opacity = value;
                break;
            default:
                flow.bandDensity = 0.05F + value * (3.0F - 0.05F);
                break;
        }
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.flow_colour.title"),
                width / 2, 16, 0xFFFFFF);

        drawHueStrip();
        drawPreview();
        drawPicker();
        drawBars();

        drawString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.flow_square_hint"),
                pickerX, pickerY - 22, 0xA0A0A0);
        drawString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.flow_brightness_hint"),
                pickerX, pickerY - 12, 0xA0A0A0);

        int row = 18;
        drawReadout("crosshairselection.flow_saturation", flow.saturation, 0, row);
        drawReadout("crosshairselection.flow_brightness", flow.brightness, 1, row);
        drawReadout("crosshairselection.flow_hue_start", flow.hueStart, 2, row);
        drawReadout("crosshairselection.flow_hue_range", flow.hueRange, 3, row);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** 底板只包住文字，避免右侧拖出一条长阴影。 */
    private void drawReadout(String key, float value, int index, int rowHeight) {
        String text = StatCollector.translateToLocalFormatted(key, String.format("%.2f", value));
        int y = readoutY + index * rowHeight;
        int textWidth = fontRendererObj.getStringWidth(text);
        int right = Math.min(width - 8, readoutX + textWidth + 8);
        drawRect(readoutX - 3, y - 3, right, y + 12, 0x90000000);
        drawString(fontRendererObj, text, readoutX, y, 0xE0E0E0);
    }

    private void drawHueStrip() {
        for (int i = 0; i < stripWidth; i++) {
            float hue = i / (float) stripWidth;
            int colour = ColorUtil.withAlpha(ColorUtil.hsbToRgb(hue, 1.0F, 1.0F), 0xFF);
            boolean selected = hue >= flow.hueStart && hue <= flow.hueStart + flow.hueRange;
            drawRect(stripX + i, stripY, stripX + i + 1, stripY + STRIP_HEIGHT,
                    selected ? colour : (colour & 0x00FFFFFF) | 0x50000000);
        }
        drawRect(startHandleX() - 1, stripY - 4, startHandleX() + 2, stripY + STRIP_HEIGHT + 4, 0xFFFFFFFF);
        drawRect(endHandleX() - 1, stripY - 4, endHandleX() + 2, stripY + STRIP_HEIGHT + 4, 0xFFFFFFFF);
        drawRect(stripX, stripY - 1, stripX + stripWidth, stripY, 0x40FFFFFF);
        drawRect(stripX, stripY + STRIP_HEIGHT, stripX + stripWidth, stripY + STRIP_HEIGHT + 1, 0x40FFFFFF);
    }

    private void drawPreview() {
        int y = stripY + STRIP_HEIGHT + 6;
        int barHeight = 16;
        long now = System.currentTimeMillis();
        // 把预览条当成一根 1.7 格长的线，用同一套取色公式逐像素求值。
        for (int i = 0; i < previewWidth; i++) {
            double t = i / (double) Math.max(1, previewWidth - 1);
            int colour = FlowPreview.colourAt(flow, 0xFFFFFFFF, now,
                    t * 1.7D, 0.0D, 0.0D, t);
            drawRect(previewX + i, y, previewX + i + 1, y + barHeight, colour);
        }
        drawRect(previewX, y - 1, previewX + previewWidth, y, 0x60FFFFFF);
        drawRect(previewX, y + barHeight, previewX + previewWidth, y + barHeight + 1, 0x60FFFFFF);
    }

    private void drawPicker() {
        HsvSquare.draw(pickerX, pickerY, pickerWidth, pickerHeight, flow.hueStart);
        HsvSquare.drawMarker(pickerX, pickerY, pickerWidth, pickerHeight,
                flow.saturation, flow.brightness);
    }

    private void drawBars() {
        drawBar(0, "crosshairselection.flow_brightness", flow.brightness, brightnessColours());
        drawBar(1, "crosshairselection.flow_opacity", flow.opacity, opacityColours());
        float density01 = (flow.bandDensity - 0.05F) / (3.0F - 0.05F);
        drawBar(2, "crosshairselection.flow_density", clamp01(density01), densityColours());
    }

    private void drawBar(int index, String labelKey, float value, int[] colours) {
        int y = barTop(index);
        // 标签放在条的上方，避免和条本身抢宽度。
        drawString(fontRendererObj,
                StatCollector.translateToLocalFormatted(labelKey, String.format("%.2f", value)),
                barX, y - 11, 0xA0A0A0);
        if (index == 1) {
            // 透明度条：先铺棋盘格，透明端才看得出来。
            ValueBar.drawChecker(barX, y, barWidth, BAR_HEIGHT);
        }
        ValueBar.draw(barX, y, barWidth, BAR_HEIGHT, colours);
        ValueBar.drawMarker(barX, y, barWidth, BAR_HEIGHT, value);
    }

    private int[] brightnessColours() {
        int[] colours = new int[barWidth];
        for (int i = 0; i < barWidth; i++) {
            float t = i / (float) Math.max(1, barWidth - 1);
            colours[i] = 0xFF000000 | ColorUtil.hsbToRgb(flow.hueStart, flow.saturation, t);
        }
        return colours;
    }

    private int[] opacityColours() {
        // 棋盘格由 ValueBar 画在条下方之前，这里只负责渐变颜色本身。
        int rgb = ColorUtil.hsbToRgb(flow.hueStart, flow.saturation, flow.brightness);
        int[] colours = new int[barWidth];
        for (int i = 0; i < barWidth; i++) {
            int alpha = Math.round(255.0F * i / (float) Math.max(1, barWidth - 1));
            colours[i] = ColorUtil.withAlpha(rgb, alpha);
        }
        return colours;
    }

    private int[] densityColours() {
        int rgb = 0xFF000000 | ColorUtil.hsbToRgb(flow.hueStart, flow.saturation, flow.brightness);
        int[] colours = new int[barWidth];
        for (int i = 0; i < barWidth; i++) {
            float t = i / (float) Math.max(1, barWidth - 1);
            float density = 0.05F + t * (3.0F - 0.05F);
            // 靠右条纹越密，直观表示密度变大。
            double stripe = Math.abs((i * density * 0.08D) % 1.0D);
            colours[i] = stripe < 0.5D ? rgb : 0x40000000;
        }
        return colours;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
