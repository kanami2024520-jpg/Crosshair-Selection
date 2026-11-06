package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.config.FlowSettings;
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

    private static final int STRIP_HEIGHT = 22;
    private static final int SQUARE_SIZE = 92;
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
    private int labelX;
    private int readoutX;
    /** Left edge of the read-out column and the width available for it. */
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
        // The page is split into three bands: a narrow left column for the SV square, the centred
        // preview panel, and a right column for read-outs. The hue strip spans the left two bands
        // so it reads as one continuous scale without ever running under the preview.
        int panelLeft = width / 2 - PREVIEW_HALF_WIDTH;
        int panelRight = width / 2 + PREVIEW_HALF_WIDTH;
        int leftEdge = Math.max(8, panelLeft - LABEL_GAP);
        int rightEdge = width - 8;
        // The hue strip and the gradient bar share the right band so the narrow left column stays
        // free for the SV square; neither ever runs under the preview panel.
        stripX = panelRight + LABEL_GAP;
        stripWidth = Math.max(80, rightEdge - stripX);
        stripY = 40;
        // The square sits in the empty band to the left of the preview, centred there.
        squareX = Math.max(8, panelLeft - LABEL_GAP - SQUARE_SIZE);
        squareY = stripY + STRIP_HEIGHT + 50;
        labelX = stripX;
        previewX = stripX;
        readoutX = squareX;
        previewWidth = stripWidth;
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
            int startX = startHandleX();
            int endX = endHandleX();
            if (Math.abs(mouseX - startX) <= HANDLE_SIZE + 2) {
                dragging = Drag.START;
                return;
            }
            if (Math.abs(mouseX - endX) <= HANDLE_SIZE + 2) {
                dragging = Drag.END;
                return;
            }
        }
        if (mouseX >= squareX && mouseX <= squareX + SQUARE_SIZE
                && mouseY >= squareY && mouseY <= squareY + SQUARE_SIZE) {
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

    private void updateStart(int mouseX) {
        float value = clamp01((mouseX - stripX) / (float) stripWidth);
        float end = flow.hueStart + flow.hueRange;
        flow.hueStart = Math.min(value, end);
        flow.hueRange = Math.max(end - flow.hueStart, 0.01F);
    }

    private void updateEnd(int mouseX) {
        float value = clamp01((mouseX - stripX) / (float) stripWidth);
        flow.hueRange = Math.max(value - flow.hueStart, 0.01F);
    }

    private void updateSquare(int mouseX, int mouseY) {
        flow.saturation = clamp01((mouseX - squareX) / (float) SQUARE_SIZE);
        flow.brightness = 1.0F - clamp01((mouseY - squareY) / (float) SQUARE_SIZE);
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
        int y = squareY + SQUARE_SIZE + 6 + index * rowHeight;
        int right = Math.min(width / 2 - PREVIEW_HALF_WIDTH - LABEL_GAP, readoutX + LABEL_WIDTH);
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
        float hue = flow.hueStart;
        // 16x16 cells are enough to read the gradient and cheap to draw every frame.
        int cells = 16;
        int cell = SQUARE_SIZE / cells;
        for (int ix = 0; ix < cells; ix++) {
            for (int iy = 0; iy < cells; iy++) {
                float saturation = ix / (float) (cells - 1);
                float brightness = 1.0F - iy / (float) (cells - 1);
                drawRect(squareX + ix * cell, squareY + iy * cell,
                        squareX + (ix + 1) * cell, squareY + (iy + 1) * cell,
                        ColorUtil.hsbToRgb(hue, saturation, brightness));
            }
        }
        int markerX = squareX + Math.round(flow.saturation * (SQUARE_SIZE - 1));
        int markerY = squareY + Math.round((1.0F - flow.brightness) * (SQUARE_SIZE - 1));
        drawRect(markerX - 3, markerY - 3, markerX + 4, markerY + 4, 0xFF000000);
        drawRect(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFFFFFFFF);
    }

    private void drawPreview() {
        int y = stripY + STRIP_HEIGHT + 18;
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
