package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.gui.widgets.ColorCallback;
import me.kuionc.crosshairselection.gui.widgets.HsvSquare;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import me.kuionc.crosshairselection.gui.widgets.SliderHost;
import me.kuionc.crosshairselection.render.RenderHelper;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

/**
 * Reusable colour picker: HSV square + hue bar + alpha bar on the right, RGBA sliders on the
 * left. Everything stays in sync — dragging the square updates the sliders, editing a slider
 * moves the marker.
 */
public class ColorPickerScreen extends GuiScreen implements SliderHost {

    private static final int OK = 10;
    private static final int CANCEL = 11;

    private final GuiScreen parent;
    private final String titleKey;
    private final ColorCallback callback;
    private int colour;

    private static final int SQUARE_SIZE = 140;
    private static final int BAR_HEIGHT = 14;

    private int squareX;
    private int squareY;
    private int hueX;
    private int hueY;
    private int hueWidth;
    private int alphaX;
    private int alphaY;
    private int alphaWidth;
    private boolean draggingHue;
    private boolean draggingSquare;
    private boolean draggingAlpha;

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

        this.squareX = this.width / 2 + 20;
        this.squareY = top;
        this.hueX = this.squareX;
        this.hueY = this.squareY + SQUARE_SIZE + 10;
        this.hueWidth = SQUARE_SIZE;
        this.alphaX = this.squareX;
        this.alphaY = this.hueY + BAR_HEIGHT + 10;
        this.alphaWidth = SQUARE_SIZE;

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

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float[] hsb = ColorUtil.rgbToHsb(colour);
        HsvSquare.draw(squareX, squareY, SQUARE_SIZE, hsb[0]);
        HsvSquare.drawMarker(squareX, squareY, SQUARE_SIZE, hsb[1], hsb[2]);

        drawHueBar(hsb[0]);
        drawAlphaBar();

        GL11.glPopAttrib();

        String hex = ColorUtil.hex(this.colour);
        this.drawCenteredString(this.fontRendererObj, hex, this.squareX + SQUARE_SIZE / 2,
                this.alphaY + BAR_HEIGHT + 8, 0xFFFFFF);
        this.drawCenteredString(this.fontRendererObj,
                StatCollector.translateToLocalFormatted("crosshairselection.colour.alpha_value",
                        ColorUtil.alpha(this.colour)),
                this.squareX + SQUARE_SIZE / 2, this.alphaY + BAR_HEIGHT + 20, 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHueBar(float hue) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        for (int i = 0; i < hueWidth; i++) {
            int rgb = ColorUtil.hsbToRgb(i / (float) (hueWidth - 1), 1.0F, 1.0F);
            RenderHelper.drawRect(hueX + i, hueY, hueX + i + 1, hueY + BAR_HEIGHT, rgb);
        }
        int cursor = hueX + Math.round(hue * (hueWidth - 1));
        RenderHelper.drawRect(cursor - 1, hueY - 2, cursor + 2, hueY + BAR_HEIGHT + 2, 0xFFFFFFFF);
    }

    private void drawAlphaBar() {
        // 棋盘格垫底，透明端能看出来。
        RenderHelper.drawCheckerboard(alphaX, alphaY, alphaWidth, BAR_HEIGHT, 8);
        int opaque = colour | 0xFF000000;
        for (int i = 0; i < alphaWidth; i++) {
            int alpha = Math.round(255.0F * i / (float) (alphaWidth - 1));
            RenderHelper.drawRect(alphaX + i, alphaY, alphaX + i + 1, alphaY + BAR_HEIGHT,
                    ColorUtil.withAlpha(opaque, alpha));
        }
        int cursor = alphaX + Math.round(ColorUtil.alpha(colour) / 255.0F * (alphaWidth - 1));
        RenderHelper.drawRect(cursor - 1, alphaY - 2, cursor + 2, alphaY + BAR_HEIGHT + 2, 0xFFFFFFFF);
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
        if (mouseButton == 0) {
            if (inside(mouseX, mouseY, squareX, squareY, SQUARE_SIZE, SQUARE_SIZE)) {
                draggingSquare = true;
                updateSquare(mouseX, mouseY);
                return;
            }
            if (inside(mouseX, mouseY, hueX, hueY, hueWidth, BAR_HEIGHT)) {
                draggingHue = true;
                updateHue(mouseX);
                return;
            }
            if (inside(mouseX, mouseY, alphaX, alphaY, alphaWidth, BAR_HEIGHT)) {
                draggingAlpha = true;
                updateAlpha(mouseX);
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingSquare) {
            updateSquare(mouseX, mouseY);
        } else if (draggingHue) {
            updateHue(mouseX);
        } else if (draggingAlpha) {
            updateAlpha(mouseX);
        }
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        if (state >= 0) {
            draggingHue = false;
            draggingSquare = false;
            draggingAlpha = false;
        }
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    private void updateSquare(int mouseX, int mouseY) {
        float[] pick = HsvSquare.pick(squareX, squareY, SQUARE_SIZE, mouseX, mouseY);
        float[] hsb = ColorUtil.rgbToHsb(colour);
        int rgb = ColorUtil.hsbToRgb(hsb[0], pick[0], pick[1]);
        colour = ColorUtil.withAlpha(rgb, ColorUtil.alpha(colour));
        syncSliders();
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

    private void updateAlpha(int mouseX) {
        int alpha = Math.round(255.0F * Math.max(0.0F,
                Math.min(1.0F, (mouseX - alphaX) / (float) (alphaWidth - 1))));
        colour = ColorUtil.withAlpha(colour, alpha);
        syncSliders();
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
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
