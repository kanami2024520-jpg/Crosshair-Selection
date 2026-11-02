package me.kuionc.crosshairselection.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import java.util.Locale;

/**
 * Minimal 1.7.10 slider button matching the rest of the config screen. The two ends are small
 * decrement/increment buttons; dragging the middle still changes the value continuously.
 */
public class OptionSlider extends GuiButton {

    private static final int SIDE_BUTTON_WIDTH = 12;

    private final int optionId;
    private final float min;
    private final float max;
    private final boolean integer;
    private final int decimals;
    private final float step;
    private final String labelKey;
    private final SliderHost host;
    private float value;
    private boolean dragging;

    public OptionSlider(int id, int x, int y, int width, int height, float min, float max,
                        float value, boolean integer, String labelKey, SliderHost host) {
        this(id, x, y, width, height, min, max, value, integer, integer ? 0 : 1, labelKey, host);
    }

    public OptionSlider(int id, int x, int y, int width, int height, float min, float max,
                        float value, boolean integer, int decimals, String labelKey, SliderHost host) {
        this(id, x, y, width, height, min, max, value, integer, decimals,
                integer ? 1.0F : (float) Math.pow(10.0D, -Math.max(0, decimals)), labelKey, host);
    }

    public OptionSlider(int id, int x, int y, int width, int height, float min, float max,
                        float value, boolean integer, int decimals, float step,
                        String labelKey, SliderHost host) {
        super(id, x, y, width, height, "");
        this.optionId = id;
        this.min = min;
        this.max = max;
        this.integer = integer;
        this.decimals = Math.max(0, decimals);
        this.step = step > 0.0F ? step : 1.0F;
        this.labelKey = labelKey;
        this.host = host;
        this.value = clamp(value);
        refreshLabel();
    }

    private float clamp(float input) {
        return Math.max(min, Math.min(max, input));
    }

    public void setValue(float value) {
        this.value = clamp(value);
        refreshLabel();
    }

    public float getValue() {
        return value;
    }

    public int getOptionId() {
        return optionId;
    }

    private int trackLeft() {
        return xPosition + SIDE_BUTTON_WIDTH;
    }

    private int trackRight() {
        return xPosition + width - SIDE_BUTTON_WIDTH;
    }

    private void refreshLabel() {
        String formatted = integer
                ? String.valueOf(Math.round(value))
                : String.format(Locale.ROOT, "%." + decimals + "f", value);
        this.displayString = StatCollector.translateToLocalFormatted(labelKey, formatted);
    }

    private void adjust(int direction) {
        float next = value + direction * step;
        if (integer) {
            next = Math.round(next);
        } else {
            float scale = 1.0F / step;
            next = Math.round(next * scale) / scale;
        }
        next = clamp(next);
        setValue(next);
        host.onSliderChanged(optionId, next);
    }

    private void updateFromMouse(int mouseX) {
        float left = trackLeft() + 2.0F;
        float right = trackRight() - 2.0F;
        float ratio = (mouseX - left) / (right - left);
        setValue(min + (max - min) * Math.max(0.0F, Math.min(1.0F, ratio)));
        host.onSliderChanged(optionId, value);
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        super.drawButton(minecraft, mouseX, mouseY);
        if (!visible) {
            return;
        }

        int left = trackLeft();
        int right = trackRight();
        drawRect(xPosition, yPosition, left, yPosition + height, 0x50000000);
        drawRect(right, yPosition, xPosition + width, yPosition + height, 0x50000000);
        drawCenteredString(minecraft.fontRenderer, "-", xPosition + SIDE_BUTTON_WIDTH / 2,
                yPosition + 5, 0xFFFFFFFF);
        drawCenteredString(minecraft.fontRenderer, "+", right + SIDE_BUTTON_WIDTH / 2,
                yPosition + 5, 0xFFFFFFFF);

        float ratio = (value - min) / (max - min);
        int knobX = left + 2 + (int) (ratio * (right - left - 4));
        drawRect(knobX, yPosition + 1, knobX + 2, yPosition + height - 1, 0x90FFFFFF);
        int barTop = yPosition + height - 4;
        drawRect(left + 2, barTop, right - 2, barTop + 2, 0x70000000);
        drawRect(left + 2, barTop, knobX, barTop + 2, 0xFF9E9E9E);
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY) {
        if (!visible || !enabled) {
            return false;
        }
        if (mouseY >= yPosition && mouseY < yPosition + height) {
            if (mouseX >= xPosition && mouseX < xPosition + SIDE_BUTTON_WIDTH) {
                adjust(-1);
                return true;
            }
            if (mouseX >= xPosition + width - SIDE_BUTTON_WIDTH && mouseX < xPosition + width) {
                adjust(1);
                return true;
            }
        }
        if (super.mousePressed(minecraft, mouseX, mouseY)) {
            this.dragging = true;
            updateFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        this.dragging = false;
    }

    @Override
    protected void mouseDragged(Minecraft minecraft, int mouseX, int mouseY) {
        if (dragging) {
            updateFromMouse(mouseX);
        }
    }
}
