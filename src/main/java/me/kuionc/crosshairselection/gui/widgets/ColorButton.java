package me.kuionc.crosshairselection.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Button that shows a colour swatch next to its label.
 */
public class ColorButton extends GuiButton {

    private int colour;

    public ColorButton(int id, int x, int y, int width, int height, int colour, String displayString) {
        super(id, x, y, width, height, displayString);
        this.colour = colour;
    }

    public int getColour() {
        return colour;
    }

    public void setColour(int colour) {
        this.colour = colour;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
        super.drawButton(minecraft, mouseX, mouseY);
        if (!visible) {
            return;
        }
        int size = 10;
        int swatchX = xPosition + width - size - 3;
        int swatchY = yPosition + (height - size) / 2;
        drawRect(swatchX - 1, swatchY - 1, swatchX + size + 1, swatchY + size + 1, 0xFF000000);
        drawRect(swatchX, swatchY, swatchX + size, swatchY + size, colour);
    }
}
