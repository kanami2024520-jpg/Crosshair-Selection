package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.gui.widgets.HsvSquare;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

/**
 * Visual editor for the flowing gradient.
 *
 * <p>A hue strip with a draggable start and end handle selects the hue range, and a saturation /
 * brightness square picks the tint. The preview bar shows the resulting gradient so the numeric
 * sliders are no longer the only way to see what the colours look like.
 */
public class FlowColorScreen extends GuiScreen {

    private static final int BACK = 1;
    private static final int RESET = 2;

    private static final int STRIP_HEIGHT = 28;
    private static final int MAX_SQUARE_SIZE = 160;
    private static final int HANDLE_SIZE = 6;
    /** Half width of the centred preview panel plus a margin; everything stays outside this band. */
    private static final int PREVIEW_HALF_WIDTH = 80;
    private static final int LABEL_GAP = 12;
    private static final int LABEL_WIDTH = 150;

    private final GuiScreen parent;
    private final FlowSettings flow;
    private final boolean crosshair;
    private boolean childOpen;

    private int stripX;
    private int stripY;
    private int stripWidth;
    private int squareX;
    private int squareY;
    private int squareSize;
    private int readoutX;
    /** Left edge of the gradient bar under the strip and its width. */
    private int previewX;
    private int previewWidth;

    private enum Drag {
        NONE,
        START,
        END,
        SQUARE
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
        // The page is split into three bands: a narrow left column for the SV square, the centred
        // preview panel, and a right column for read-outs. The hue strip spans the left two bands
        // so it reads as one continuous scale without ever running under the preview.
        int margin = 16;
        // The hue strip now spans the whole page instead of sharing the right band, and the
        // gradient bar repeats it directly underneath.
        stripX = margin;
        stripWidth = Math.max(80, width - 2 * margin);
        stripY = 40;
        previewX = stripX;
        previewWidth = stripWidth;

        // Square on the left of the centred preview, read-outs on its right.
        int panelLeft = width / 2 - PREVIEW_HALF_WIDTH;
        int panelRight = width / 2 + PREVIEW_HALF_WIDTH;
        // 色带 → 渐变条 → 说明文字 → 色盘 → 读数，纵向排开，互不重叠。
        int readoutBlock = 4 * 18 + 8;
        int availableHeight = height - (stripY + STRIP_HEIGHT + 52) - readoutBlock - 48;
        squareSize = Math.min(MAX_SQUARE_SIZE,
                Math.max(64, Math.min(availableHeight, width / 4)));
        squareX = Math.max(8, panelLeft - LABEL_GAP - squareSize);
        squareY = stripY + STRIP_HEIGHT + 52;
        readoutX = squareX;
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

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        if (button != 0) {
            return;
        }
        if (mouseY >= stripY - HANDLE_SIZE && mouseY <= stripY + STRIP_HEIGHT + HANDLE_SIZE) {
            // 整条色带都可以点：按到两个把手的距离选择拖动起点/终点，点哪跳哪。
            int startX = startHandleX();
            int endX = endHandleX();
            if (Math.abs(mouseX - startX) <= Math.abs(mouseX - endX)) {
                dragging = Drag.START;
                updateStart(mouseX);
            } else {
                dragging = Drag.END;
                updateEnd(mouseX);
            }
            return;
        }
        if (mouseX >= squareX && mouseX <= squareX + squareSize
                && mouseY >= squareY && mouseY <= squareY + squareSize) {
            dragging = Drag.SQUARE;
            updateSquare(mouseX, mouseY);
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
     *
     * <p>色条只画 0..1 这一周，如果 hueStart + hueRange 超过 1，终点把手会跑到色条外面，
     * 超出部分也不会被画出来。拖动和旧配置都在这里归一化。
     */
    private void normaliseHueRange() {
        flow.hueStart = flow.hueStart - (float) Math.floor(flow.hueStart);
        float maxRange = Math.max(0.01F, 1.0F - flow.hueStart);
        flow.hueRange = Math.max(0.01F, Math.min(flow.hueRange, maxRange));
    }

    private void updateStart(int mouseX) {
        float value = clamp01((mouseX - stripX) / (float) stripWidth);
        // 终点保持不动：起点只能落在 0 .. (1 - range) 之间。
        flow.hueStart = Math.min(value, Math.max(0.0F, 1.0F - flow.hueRange));
    }

    private void updateEnd(int mouseX) {
        float value = clamp01((mouseX - stripX) / (float) stripWidth);
        // 终点必须留在色条内，否则超出的色段画不出来。
        float maxRange = Math.max(0.01F, 1.0F - flow.hueStart);
        flow.hueRange = Math.max(0.01F, Math.min(value - flow.hueStart, maxRange));
    }

    private void updateSquare(int mouseX, int mouseY) {
        flow.saturation = clamp01((mouseX - squareX) / (float) squareSize);
        flow.brightness = 1.0F - clamp01((mouseY - squareY) / (float) squareSize);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal("crosshairselection.flow_colour.title"),
                width / 2, 16, 0xFFFFFF);

        drawHueStrip();
        drawSquare();
        drawPreview();
        // The preview follows the edited target, so leaving for the colour page does not hide it.
        PreviewPanel.begin(crosshair ? PreviewPanel.Content.CROSSHAIR : PreviewPanel.Content.CUBE, flow);
        PreviewPanel.drawCentred(width, height);

        // Each read-out gets its own banded row so labels can never overlap each other or the
        // preview panel that shares this area of the screen.
        int row = 18;
        drawReadout("crosshairselection.flow_saturation", flow.saturation, 0, row);
        drawReadout("crosshairselection.flow_brightness", flow.brightness, 1, row);
        drawReadout("crosshairselection.flow_hue_start", flow.hueStart, 2, row);
        drawReadout("crosshairselection.flow_hue_range", flow.hueRange, 3, row);
        // Explains what the square's vertical axis actually changes.
        drawString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.flow_square_hint"),
                squareX, squareY - 22, 0xA0A0A0);
        drawString(fontRendererObj,
                StatCollector.translateToLocal("crosshairselection.flow_brightness_hint"),
                squareX, squareY - 12, 0xA0A0A0);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /** One label with a dark backing strip, so it stays readable over the preview backdrop. */
    /** Read-outs live under the SV square, in the left band, clear of the preview. */
    private void drawReadout(String key, float value, int index, int rowHeight) {
        int y = squareY + squareSize + 6 + index * rowHeight;
        int right = Math.min(width - 8, readoutX + LABEL_WIDTH);
        drawRect(readoutX - 3, y - 3, right, y + 12, 0x90000000);
        drawString(fontRendererObj,
                StatCollector.translateToLocalFormatted(key, String.format("%.2f", value)),
                readoutX, y, 0xE0E0E0);
    }

    private void drawHueStrip() {
        // A full hue wheel, dimmed outside the selected range so the range is obvious.
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

    private void drawSquare() {
        HsvSquare.draw(squareX, squareY, squareSize, flow.hueStart);
        HsvSquare.drawMarker(squareX, squareY, squareSize, flow.saturation, flow.brightness);
    }

    private void drawPreview() {
        int y = stripY + STRIP_HEIGHT + 6;
        int height = 16;
        for (int i = 0; i < previewWidth; i++) {
            float t = i / (float) previewWidth;
            float hue = flow.hueStart + t * flow.hueRange;
            drawRect(previewX + i, y, previewX + i + 1, y + height,
                    ColorUtil.hsbToRgb(hue, flow.saturation, flow.brightness));
        }
        drawRect(previewX, y - 1, previewX + previewWidth, y, 0x60FFFFFF);
        drawRect(previewX, y + height, previewX + previewWidth, y + height + 1, 0x60FFFFFF);

        // A moving highlight shows how the gradient will animate.
        float phase = (System.currentTimeMillis() % 4000L) / 4000.0F;
        int markerX = previewX + Math.round(phase * previewWidth);
        drawRect(markerX - 1, y - 2, markerX + 1, y + height + 2, 0xFFFFFFFF);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
