package me.kuionc.crosshairselection.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

import java.util.Locale;

/**
 * Minimal 1.7.10 slider button matching the rest of the config screen.
 */
public class OptionSlider extends GuiButton {

    private final int optionId;
    private final float min;
    private final float max;
    private final boolean integer;
    private final String labelKey;
    private final SliderHost host;
    private float value;
    private boolean dragging;

    public OptionSlider(int id, int x, int y, int width, int height, float min, float max,
                        float value, boolean integer, String labelKey, SliderHost host) {
        super(id, x, y, width, height, "");
        this.optionId = id;
        this.min = min;
        this.max = max;
        this.integer = integer;
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

    private void refreshLabel() {
        String formatted = integer
                ? String.valueOf(Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
        this.displayString = StatCollector.translateToLocalFormatted(labelKey, formatted);
    }

    private void updateFromMouse(int mouseX) {
        float ratio = (mouseX - (xPosition + 4)) / (float) (width - 8);
        this.value = clamp(min + (max - min) * Math.max(0.0F, Math.min(1.0F, ratio)));
        refreshLabel();
        host.onSliderChanged(optionId, value);
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        super.drawButton(minecraft, mouseX, mouseY);
        if (!visible) {
            return;
        }
        float ratio = (value - min) / (max - min);
        int knobX = xPosition + 2 + (int) (ratio * (width - 8));
        drawRect(knobX, yPosition + 1, knobX + 2, yPosition + height - 1, 0x90FFFFFF);
        int barTop = yPosition + height - 4;
        drawRect(xPosition + 2, barTop, xPosition + width - 2, barTop + 2, 0x70000000);
        drawRect(xPosition + 2, barTop, knobX, barTop + 2, 0xFF9E9E9E);
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY) {
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
