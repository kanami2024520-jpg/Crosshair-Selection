package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.gui.widgets.ColorCallback;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import me.kuionc.crosshairselection.gui.widgets.SliderHost;
import me.kuionc.crosshairselection.render.RenderHelper;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

/**
 * Reusable RGBA colour picker: RGB/A sliders, hue bar, checkerboard preview and hex readout.
 */
public class ColorPickerScreen extends GuiScreen implements SliderHost {

    private static final int OK = 10;
    private static final int CANCEL = 11;

    private final GuiScreen parent;
    private final String titleKey;
    private final ColorCallback callback;
    private int colour;

    private final int hueWidth = 140;
    private final int hueHeight = 14;
    private int hueX;
    private int hueY;
    private int swatchX;
    private int swatchY;
    private boolean draggingHue;

    public ColorPickerScreen(GuiScreen parent, String titleKey, int initialColour, ColorCallback callback) {
        this.parent = parent;
        this.titleKey = titleKey;
        this.callback = callback;
        this.colour = initialColour;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int left = this.width / 2 - 200;
        int top = 54;
        this.buttonList.add(new OptionSlider(1, left, top, 170, 18, 0, 255,
                ColorUtil.red(colour), true, "crosshairselection.colour.red", this));
        this.buttonList.add(new OptionSlider(2, left, top + 24, 170, 18, 0, 255,
                ColorUtil.green(colour), true, "crosshairselection.colour.green", this));
        this.buttonList.add(new OptionSlider(3, left, top + 48, 170, 18, 0, 255,
                ColorUtil.blue(colour), true, "crosshairselection.colour.blue", this));
        this.buttonList.add(new OptionSlider(4, left, top + 72, 170, 18, 0, 255,
                ColorUtil.alpha(colour), true, "crosshairselection.colour.alpha", this));

        this.swatchX = this.width / 2 + 10;
        this.swatchY = 54;
        this.hueX = this.swatchX;
        this.hueY = this.swatchY + 84;

        this.buttonList.add(new GuiButton(OK, this.width / 2 - 105, this.height - 30, 100, 20,
                StatCollector.translateToLocal("crosshairselection.colour.ok")));
        this.buttonList.add(new GuiButton(CANCEL, this.width / 2 + 5, this.height - 30, 100, 20,
                StatCollector.translateToLocal("crosshairselection.colour.cancel")));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, StatCollector.translateToLocal(titleKey),
                this.width / 2, 20, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderHelper.drawCheckerboard(this.swatchX, this.swatchY, 140, 64, 8);
        RenderHelper.drawRect(this.swatchX, this.swatchY, this.swatchX + 140, this.swatchY + 64, this.colour);
        RenderHelper.drawOutline(this.swatchX - 1, this.swatchY - 1, this.swatchX + 141, this.swatchY + 65,
                1.0F, 0xFF808080);

        for (int i = 0; i < this.hueWidth; i++) {
            int rgb = ColorUtil.hsbToRgb(i / (float) (this.hueWidth - 1), 1.0F, 1.0F);
            RenderHelper.drawRect(this.hueX + i, this.hueY, this.hueX + i + 1, this.hueY + this.hueHeight, rgb);
        }
        float[] hsb = ColorUtil.rgbToHsb(this.colour);
        int cursor = this.hueX + (int) (hsb[0] * (this.hueWidth - 1));
        RenderHelper.drawRect(cursor - 1, this.hueY - 2, cursor + 2, this.hueY + this.hueHeight + 2, 0xFFFFFFFF);

        GL11.glPopAttrib();

        String hex = ColorUtil.hex(this.colour);
        this.drawCenteredString(this.fontRendererObj, hex, this.swatchX + 70,
                this.hueY + this.hueHeight + 6, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj,
                StatCollector.translateToLocalFormatted("crosshairselection.colour.alpha_value",
                        ColorUtil.alpha(this.colour)),
                this.swatchX + 70, this.hueY + this.hueHeight + 18, 0xAAAAAA);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == OK) {
            callback.accept(colour);
            this.mc.displayGuiScreen(parent);
        } else if (button.id == CANCEL) {
            this.mc.displayGuiScreen(parent);
        }
    }

    @Override
    public void onSliderChanged(int optionId, float value) {
        int channel = Math.round(value);
        switch (optionId) {
            case 1:
                colour = ColorUtil.pack(channel, ColorUtil.green(colour), ColorUtil.blue(colour), ColorUtil.alpha(colour));
                break;
            case 2:
                colour = ColorUtil.pack(ColorUtil.red(colour), channel, ColorUtil.blue(colour), ColorUtil.alpha(colour));
                break;
            case 3:
                colour = ColorUtil.pack(ColorUtil.red(colour), ColorUtil.green(colour), channel, ColorUtil.alpha(colour));
                break;
            case 4:
                colour = ColorUtil.pack(ColorUtil.red(colour), ColorUtil.green(colour), ColorUtil.blue(colour), channel);
                break;
            default:
                break;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && mouseX >= hueX && mouseX < hueX + hueWidth
                && mouseY >= hueY && mouseY < hueY + hueHeight) {
            draggingHue = true;
            updateHue(mouseX);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingHue) {
            updateHue(mouseX);
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (state >= 0) {
            draggingHue = false;
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    private void updateHue(int mouseX) {
        float hue = Math.max(0.0F, Math.min(1.0F, (mouseX - hueX) / (float) (hueWidth - 1)));
        float[] hsb = ColorUtil.rgbToHsb(colour);
        float saturation = hsb[1] < 0.01F ? 0.8F : hsb[1];
        float brightness = hsb[2] < 0.01F ? 0.8F : hsb[2];
        int rgb = ColorUtil.hsbToRgb(hue, saturation, brightness);
        colour = ColorUtil.withAlpha(rgb, ColorUtil.alpha(colour));
        syncSliders();
    }

    private void syncSliders() {
        for (Object object : this.buttonList) {
            if (!(object instanceof OptionSlider)) {
                continue;
            }
            OptionSlider slider = (OptionSlider) object;
            switch (slider.getOptionId()) {
                case 1:
                    slider.setValue(ColorUtil.red(colour));
                    break;
                case 2:
                    slider.setValue(ColorUtil.green(colour));
                    break;
                case 3:
                    slider.setValue(ColorUtil.blue(colour));
                    break;
                case 4:
                    slider.setValue(ColorUtil.alpha(colour));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            this.mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
