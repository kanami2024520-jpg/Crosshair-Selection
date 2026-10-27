package me.kuionc.crosshairselection.gui;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.gui.widgets.ColorButton;
import me.kuionc.crosshairselection.gui.widgets.ColorCallback;
import me.kuionc.crosshairselection.gui.widgets.OptionSlider;
import me.kuionc.crosshairselection.gui.widgets.SliderHost;
import me.kuionc.crosshairselection.render.CrosshairRenderer;
import me.kuionc.crosshairselection.render.RenderHelper;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

/**
 * Unified settings screen: selection box and crosshair.
 */
public class ConfigScreen extends GuiScreen implements SliderHost {

    private static final int TAB_SELECTION = 100;
    private static final int TAB_CROSSHAIR = 101;
    private static final int SAVE = 200;
    private static final int CANCEL = 201;
    private static final int DEFAULTS = 202;

    private static final int SELECTION_ENABLED = 1;
    private static final int SELECTION_COLOUR = 2;
    private static final int SELECTION_THICKNESS = 3;
    private static final int SELECTION_BLINK_ALPHA = 4;
    private static final int SELECTION_BLINK_SPEED = 5;
    private static final int SELECTION_RAINBOW = 6;
    private static final int SELECTION_LINK = 7;
    private static final int SELECTION_HIDDEN = 8;
    private static final int SELECTION_FILL = 9;
    private static final int SELECTION_BLINK = 18;
    private static final int SELECTION_MARGIN = 19;


    private static final int CROSSHAIR_ENABLED = 20;
    private static final int CROSSHAIR_TYPE = 21;
    private static final int CROSSHAIR_COLOUR = 22;
    private static final int CROSSHAIR_WIDTH = 23;
    private static final int CROSSHAIR_HEIGHT = 24;
    private static final int CROSSHAIR_GAP = 25;
    private static final int CROSSHAIR_THICKNESS = 26;
    private static final int CROSSHAIR_ROTATION = 27;
    private static final int CROSSHAIR_OUTLINE_THICKNESS = 38;
    private static final int CROSSHAIR_OUTLINE = 28;
    private static final int CROSSHAIR_OUTLINE_COLOUR = 29;
    private static final int CROSSHAIR_DOT = 30;
    private static final int CROSSHAIR_DOT_COLOUR = 31;
    private static final int CROSSHAIR_RAINBOW = 32;
    private static final int CROSSHAIR_RAINBOW_SPEED = 33;
    private static final int CROSSHAIR_VISIBLE = 34;
    private static final int CROSSHAIR_VISIBLE_F1 = 35;
    private static final int CROSSHAIR_VISIBLE_DEBUG = 36;
    private static final int CROSSHAIR_VISIBLE_THIRD = 37;

    private final GuiScreen parent;
    private final CrosshairRenderer previewRenderer = new CrosshairRenderer();
    private Snapshot snapshot;
    private int tab;
    private boolean saved;
    private boolean childOpen;

    public ConfigScreen(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        if (snapshot == null) {
            snapshot = new Snapshot();
        }
        buttonList.clear();

        int tabWidth = 120;
        buttonList.add(new GuiButton(TAB_SELECTION, width / 2 - tabWidth - 2, 20, tabWidth, 18,
                StatCollector.translateToLocal("crosshairselection.tab.selection")));
        buttonList.add(new GuiButton(TAB_CROSSHAIR, width / 2 + 2, 20, tabWidth, 18,
                StatCollector.translateToLocal("crosshairselection.tab.crosshair")));

        buttonList.add(new GuiButton(SAVE, width / 2 - 145, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.save")));
        buttonList.add(new GuiButton(CANCEL, width / 2 - 45, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.cancel")));
        buttonList.add(new GuiButton(DEFAULTS, width / 2 + 55, height - 26, 90, 18,
                StatCollector.translateToLocal("crosshairselection.defaults")));

        if (tab == 0) {
            initSelectionButtons();
        } else {
            initCrosshairButtons();
        }
    }

    private void initSelectionButtons() {
        int left = 4;
        int right = width - 154;
        int y = 46;
        buttonList.add(toggle(SELECTION_ENABLED, left, y, "crosshairselection.enabled", SelectionBoxConfig.enabled));
        buttonList.add(new ColorButton(SELECTION_COLOUR, left, y + 20, 150, 18, SelectionBoxConfig.colour,
                colourLabel(SelectionBoxConfig.colour)));
        buttonList.add(new OptionSlider(SELECTION_THICKNESS, left, y + 40, 150, 18, 0.1F, 7.0F,
                SelectionBoxConfig.thickness, false, "crosshairselection.thickness", this));
        buttonList.add(new OptionSlider(SELECTION_BLINK_ALPHA, left, y + 60, 150, 18, 0, 255,
                SelectionBoxConfig.blinkAlpha, true, "crosshairselection.blink_alpha", this));
        buttonList.add(new OptionSlider(SELECTION_BLINK_SPEED, left, y + 80, 150, 18, 0.0F, 1.0F,
                SelectionBoxConfig.blinkSpeed, false, "crosshairselection.blink_speed", this));
        buttonList.add(toggle(SELECTION_RAINBOW, left, y + 100, "crosshairselection.rainbow", SelectionBoxConfig.rainbow));
        buttonList.add(toggle(SELECTION_LINK, right, y, "crosshairselection.link_blocks", SelectionBoxConfig.linkBlocks));
        buttonList.add(toggle(SELECTION_HIDDEN, right, y + 20, "crosshairselection.show_hidden", SelectionBoxConfig.showHidden));
        buttonList.add(toggle(SELECTION_FILL, right, y + 40, "crosshairselection.fill", SelectionBoxConfig.showFill));
        buttonList.add(toggle(SELECTION_BLINK, right, y + 60, "crosshairselection.blink", SelectionBoxConfig.blink));
        buttonList.add(new OptionSlider(SELECTION_MARGIN, right, y + 126, 150, 18, 0.0F, 0.02F,
                SelectionBoxConfig.outlineMargin, false, 3, "crosshairselection.outline_margin", this));
    }

    private void initCrosshairButtons() {
        int left = 4;
        int right = width - 154;
        int y = 46;
        buttonList.add(toggle(CROSSHAIR_ENABLED, left, y, "crosshairselection.enabled", CrosshairConfig.enabled));
        buttonList.add(new GuiButton(CROSSHAIR_TYPE, left, y + 18, 150, 18,
                StatCollector.translateToLocalFormatted("crosshairselection.type",
                        StatCollector.translateToLocal(CrosshairConfig.type.getTranslationKey()))));
        buttonList.add(new ColorButton(CROSSHAIR_COLOUR, left, y + 36, 150, 18, CrosshairConfig.colour,
                colourLabel(CrosshairConfig.colour)));
        buttonList.add(new OptionSlider(CROSSHAIR_WIDTH, left, y + 54, 150, 18, 0.0F, 50.0F,
                CrosshairConfig.width, false, "crosshairselection.width", this));
        buttonList.add(new OptionSlider(CROSSHAIR_HEIGHT, left, y + 72, 150, 18, 0.0F, 50.0F,
                CrosshairConfig.height, false, "crosshairselection.height", this));
        buttonList.add(new OptionSlider(CROSSHAIR_GAP, left, y + 90, 150, 18, 0.0F, 50.0F,
                CrosshairConfig.gap, false, "crosshairselection.gap", this));
        buttonList.add(new OptionSlider(CROSSHAIR_THICKNESS, left, y + 108, 150, 18, 0.25F, 10.0F,
                CrosshairConfig.thickness / 4.0F, false, "crosshairselection.thickness", this));
        buttonList.add(new OptionSlider(CROSSHAIR_ROTATION, left, y + 126, 150, 18, 0, 360,
                CrosshairConfig.rotation, true, "crosshairselection.rotation", this));
        buttonList.add(new OptionSlider(CROSSHAIR_OUTLINE_THICKNESS, left, y + 144, 150, 18, 0.0F, 2.0F,
                CrosshairConfig.outlineThickness, false, "crosshairselection.outline_thickness", this));

        buttonList.add(toggle(CROSSHAIR_OUTLINE, right, y, "crosshairselection.outline", CrosshairConfig.outline));
        buttonList.add(new ColorButton(CROSSHAIR_OUTLINE_COLOUR, right, y + 18, 150, 18, CrosshairConfig.outlineColour,
                colourLabel(CrosshairConfig.outlineColour)));
        buttonList.add(toggle(CROSSHAIR_DOT, right, y + 36, "crosshairselection.dot", CrosshairConfig.dot));
        buttonList.add(new ColorButton(CROSSHAIR_DOT_COLOUR, right, y + 54, 150, 18, CrosshairConfig.dotColour,
                colourLabel(CrosshairConfig.dotColour)));
        buttonList.add(toggle(CROSSHAIR_RAINBOW, right, y + 72, "crosshairselection.rainbow", CrosshairConfig.rainbow));
        buttonList.add(new OptionSlider(CROSSHAIR_RAINBOW_SPEED, right, y + 90, 150, 18, 0, 2000,
                CrosshairConfig.rainbowSpeed, true, "crosshairselection.rainbow_speed", this));
        buttonList.add(toggle(CROSSHAIR_VISIBLE, right, y + 108, "crosshairselection.visible", CrosshairConfig.visibleDefault));
        buttonList.add(toggle(CROSSHAIR_VISIBLE_F1, right, y + 126, "crosshairselection.visible_f1", CrosshairConfig.visibleHiddenGui));
        buttonList.add(toggle(CROSSHAIR_VISIBLE_DEBUG, right, y + 144, "crosshairselection.visible_debug", CrosshairConfig.visibleDebug));
        buttonList.add(toggle(CROSSHAIR_VISIBLE_THIRD, right, y + 162, "crosshairselection.visible_third", CrosshairConfig.visibleThirdPerson));
    }

    private GuiButton toggle(int id, int x, int y, String key, boolean value) {
        return new GuiButton(id, x, y, 150, 18,
                StatCollector.translateToLocalFormatted(key, onOff(value)));
    }

    private static String onOff(boolean value) {
        return StatCollector.translateToLocal(value ? "crosshairselection.on" : "crosshairselection.off");
    }

    private static String colourLabel(int colour) {
        return StatCollector.translateToLocalFormatted("crosshairselection.colour", ColorUtil.hex(colour));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case TAB_SELECTION:
                tab = 0;
                initGui();
                break;
            case TAB_CROSSHAIR:
                tab = 1;
                initGui();
                break;
            case SAVE:
                ConfigurationManager.save(SelectionBoxConfig.class, CrosshairConfig.class);
                saved = true;
                snapshot = new Snapshot();
                mc.displayGuiScreen(parent);
                break;
            case CANCEL:
                saved = true;
                snapshot.restore();
                mc.displayGuiScreen(parent);
                break;
            case DEFAULTS:
                SelectionBoxConfig.reset();
                CrosshairConfig.reset();
                initGui();
                break;
            case SELECTION_ENABLED:
                SelectionBoxConfig.enabled = !SelectionBoxConfig.enabled;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.enabled",
                        onOff(SelectionBoxConfig.enabled));
                break;
            case SELECTION_RAINBOW:
                SelectionBoxConfig.rainbow = !SelectionBoxConfig.rainbow;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.rainbow",
                        onOff(SelectionBoxConfig.rainbow));
                break;
            case SELECTION_LINK:
                SelectionBoxConfig.linkBlocks = !SelectionBoxConfig.linkBlocks;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.link_blocks",
                        onOff(SelectionBoxConfig.linkBlocks));
                break;
            case SELECTION_HIDDEN:
                SelectionBoxConfig.showHidden = !SelectionBoxConfig.showHidden;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.show_hidden",
                        onOff(SelectionBoxConfig.showHidden));
                break;
            case SELECTION_FILL:
                SelectionBoxConfig.showFill = !SelectionBoxConfig.showFill;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.fill",
                        onOff(SelectionBoxConfig.showFill));
                break;
            case SELECTION_BLINK:
                SelectionBoxConfig.blink = !SelectionBoxConfig.blink;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.blink",
                        onOff(SelectionBoxConfig.blink));
                break;
            case SELECTION_COLOUR:
                openColourPicker("crosshairselection.colour.title", SelectionBoxConfig.colour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        SelectionBoxConfig.colour = argb;
                        refreshColourButton(SELECTION_COLOUR, argb);
                    }
                });
                break;
            case CROSSHAIR_ENABLED:
                CrosshairConfig.enabled = !CrosshairConfig.enabled;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.enabled",
                        onOff(CrosshairConfig.enabled));
                break;
            case CROSSHAIR_TYPE:
                CrosshairConfig.type = CrosshairConfig.type.next();
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.type",
                        StatCollector.translateToLocal(CrosshairConfig.type.getTranslationKey()));
                break;
            case CROSSHAIR_OUTLINE:
                CrosshairConfig.outline = !CrosshairConfig.outline;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.outline",
                        onOff(CrosshairConfig.outline));
                break;
            case CROSSHAIR_DOT:
                CrosshairConfig.dot = !CrosshairConfig.dot;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.dot",
                        onOff(CrosshairConfig.dot));
                break;
            case CROSSHAIR_RAINBOW:
                CrosshairConfig.rainbow = !CrosshairConfig.rainbow;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.rainbow",
                        onOff(CrosshairConfig.rainbow));
                break;
            case CROSSHAIR_VISIBLE:
                CrosshairConfig.visibleDefault = !CrosshairConfig.visibleDefault;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.visible",
                        onOff(CrosshairConfig.visibleDefault));
                break;
            case CROSSHAIR_VISIBLE_F1:
                CrosshairConfig.visibleHiddenGui = !CrosshairConfig.visibleHiddenGui;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.visible_f1",
                        onOff(CrosshairConfig.visibleHiddenGui));
                break;
            case CROSSHAIR_VISIBLE_DEBUG:
                CrosshairConfig.visibleDebug = !CrosshairConfig.visibleDebug;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.visible_debug",
                        onOff(CrosshairConfig.visibleDebug));
                break;
            case CROSSHAIR_VISIBLE_THIRD:
                CrosshairConfig.visibleThirdPerson = !CrosshairConfig.visibleThirdPerson;
                button.displayString = StatCollector.translateToLocalFormatted("crosshairselection.visible_third",
                        onOff(CrosshairConfig.visibleThirdPerson));
                break;
            case CROSSHAIR_COLOUR:
                openColourPicker("crosshairselection.colour.title", CrosshairConfig.colour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        CrosshairConfig.colour = argb;
                        refreshColourButton(CROSSHAIR_COLOUR, argb);
                    }
                });
                break;
            case CROSSHAIR_OUTLINE_COLOUR:
                openColourPicker("crosshairselection.colour.title", CrosshairConfig.outlineColour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        CrosshairConfig.outlineColour = argb;
                        refreshColourButton(CROSSHAIR_OUTLINE_COLOUR, argb);
                    }
                });
                break;
            case CROSSHAIR_DOT_COLOUR:
                openColourPicker("crosshairselection.colour.title", CrosshairConfig.dotColour, new ColorCallback() {
                    @Override
                    public void accept(int argb) {
                        CrosshairConfig.dotColour = argb;
                        refreshColourButton(CROSSHAIR_DOT_COLOUR, argb);
                    }
                });
                break;
            default:
                break;
        }
    }

    private void openColourPicker(String titleKey, int initial, ColorCallback callback) {
        // displayGuiScreen() calls onGuiClosed() on this screen; do not let that revert the
        // values we are about to pass to the child colour picker.
        childOpen = true;
        mc.displayGuiScreen(new ColorPickerScreen(this, titleKey, initial, callback));
    }

    private void refreshColourButton(int id, int colour) {
        for (Object object : buttonList) {
            if (object instanceof ColorButton && ((ColorButton) object).id == id) {
                ColorButton button = (ColorButton) object;
                button.setColour(colour);
                button.displayString = colourLabel(colour);
                return;
            }
        }
    }

    @Override
    public void onSliderChanged(int optionId, float value) {
        switch (optionId) {
            case SELECTION_THICKNESS:
                SelectionBoxConfig.thickness = value;
                break;
            case SELECTION_BLINK_ALPHA:
                SelectionBoxConfig.blinkAlpha = Math.round(value);
                break;
            case SELECTION_BLINK_SPEED:
                SelectionBoxConfig.blinkSpeed = value;
                break;
            case SELECTION_MARGIN:
                SelectionBoxConfig.outlineMargin = Math.round(value * 1000.0F) / 1000.0F;
                break;
            case CROSSHAIR_WIDTH:
                CrosshairConfig.width = value;
                break;
            case CROSSHAIR_HEIGHT:
                CrosshairConfig.height = value;
                break;
            case CROSSHAIR_GAP:
                CrosshairConfig.gap = value;
                break;
            case CROSSHAIR_THICKNESS:
                CrosshairConfig.thickness = Math.max(1, Math.round(value * 4.0F));
                break;
            case CROSSHAIR_ROTATION:
                CrosshairConfig.rotation = Math.round(value);
                break;
            case CROSSHAIR_RAINBOW_SPEED:
                CrosshairConfig.rainbowSpeed = Math.round(value);
                break;
            case CROSSHAIR_OUTLINE_THICKNESS:
                CrosshairConfig.outlineThickness = value;
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawGradientRect(0, 0, width, height, 0xA0101010, 0xC0101010);
        this.drawCenteredString(fontRendererObj, "Crosshair-Selection", width / 2, 6, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (tab == 0) {
            drawSelectionPreview(width / 2, 140);
        } else if (tab == 1) {
            drawCrosshairPreview(width / 2, 140);
        }
    }

    private void drawSelectionPreview(int centerX, int centerY) {
        int size = 90;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderHelper.drawCheckerboard(centerX - size / 2, centerY - size / 2, size, size, 10);
        RenderHelper.drawOutline(centerX - size / 2 - 1, centerY - size / 2 - 1,
                centerX + size / 2 + 1, centerY + size / 2 + 1, 1.0F, 0xFF808080);

        int colour = SelectionBoxConfig.colour;
        if (SelectionBoxConfig.rainbow) {
            colour = ColorUtil.withAlpha(ColorUtil.hsbToRgb(
                    System.currentTimeMillis() % 10000L / 10000.0F, 0.8F, 0.8F), ColorUtil.alpha(colour));
        }
        int innerAlpha;
        if (!SelectionBoxConfig.showFill) {
            innerAlpha = 0;
        } else if (SelectionBoxConfig.blink && SelectionBoxConfig.blinkSpeed > 0.0F) {
            innerAlpha = Math.round(SelectionBoxConfig.blinkAlpha * (float) Math.abs(Math.sin(
                    System.currentTimeMillis() / 100.0D * SelectionBoxConfig.blinkSpeed)));
        } else {
            innerAlpha = SelectionBoxConfig.blinkAlpha;
        }
        int inner = ColorUtil.withAlpha(colour, innerAlpha);

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0.0F);
        GL11.glRotatef(24.0F, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(45.0F, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(54.0F, 54.0F, 54.0F);
        drawCube(colour, inner);
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }

    private static void drawCube(int outlineColour, int innerColour) {
        if (ColorUtil.alpha(innerColour) > 2) {
            RenderHelper.setColor(innerColour);
            Tessellator tessellator = com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager.get();
            tessellator.startDrawing(GL11.GL_QUADS);
            addQuad(tessellator, -0.5, -0.5, -0.5, 0.5, -0.5, -0.5, 0.5, -0.5, 0.5, -0.5, -0.5, 0.5);
            addQuad(tessellator, -0.5, 0.5, -0.5, -0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, -0.5);
            addQuad(tessellator, -0.5, -0.5, -0.5, -0.5, 0.5, -0.5, -0.5, 0.5, 0.5, -0.5, -0.5, 0.5);
            addQuad(tessellator, 0.5, -0.5, -0.5, 0.5, -0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, -0.5);
            addQuad(tessellator, -0.5, -0.5, -0.5, 0.5, -0.5, -0.5, 0.5, 0.5, -0.5, -0.5, 0.5, -0.5);
            addQuad(tessellator, -0.5, -0.5, 0.5, -0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, -0.5, 0.5);
            tessellator.draw();
        }
        RenderHelper.setColor(outlineColour);
        GL11.glLineWidth(Math.max(1.0F, Math.min(4.0F, SelectionBoxConfig.thickness)));
        Tessellator tessellator = com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_LINES);
        addLine(tessellator, -0.5, -0.5, -0.5, 0.5, -0.5, -0.5);
        addLine(tessellator, 0.5, -0.5, -0.5, 0.5, -0.5, 0.5);
        addLine(tessellator, 0.5, -0.5, 0.5, -0.5, -0.5, 0.5);
        addLine(tessellator, -0.5, -0.5, 0.5, -0.5, -0.5, -0.5);
        addLine(tessellator, -0.5, 0.5, -0.5, 0.5, 0.5, -0.5);
        addLine(tessellator, 0.5, 0.5, -0.5, 0.5, 0.5, 0.5);
        addLine(tessellator, 0.5, 0.5, 0.5, -0.5, 0.5, 0.5);
        addLine(tessellator, -0.5, 0.5, 0.5, -0.5, 0.5, -0.5);
        addLine(tessellator, -0.5, -0.5, -0.5, -0.5, 0.5, -0.5);
        addLine(tessellator, 0.5, -0.5, -0.5, 0.5, 0.5, -0.5);
        addLine(tessellator, 0.5, -0.5, 0.5, 0.5, 0.5, 0.5);
        addLine(tessellator, -0.5, -0.5, 0.5, -0.5, 0.5, 0.5);
        tessellator.draw();
        GL11.glLineWidth(1.0F);
    }

    private void drawCrosshairPreview(int centerX, int centerY) {
        int size = 90;
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderHelper.drawCheckerboard(centerX - size / 2, centerY - size / 2, size, size, 10);
        RenderHelper.drawOutline(centerX - size / 2 - 1, centerY - size / 2 - 1,
                centerX + size / 2 + 1, centerY + size / 2 + 1, 1.0F, 0xFF808080);
        GL11.glPopAttrib();

        ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int scale = resolution.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((centerX - size / 2) * scale,
                mc.displayHeight - (centerY + size / 2) * scale, size * scale, size * scale);
        previewRenderer.draw(centerX, centerY);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static void addQuad(Tessellator tessellator,
                                double x1, double y1, double z1, double x2, double y2, double z2,
                                double x3, double y3, double z3, double x4, double y4, double z4) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
        tessellator.addVertex(x3, y3, z3);
        tessellator.addVertex(x4, y4, z4);
    }

    private static void addLine(Tessellator tessellator,
                                double x1, double y1, double z1, double x2, double y2, double z2) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
    }

    @Override
    public void onGuiClosed() {
        if (childOpen) {
            childOpen = false;
            return;
        }
        if (!saved && snapshot != null) {
            snapshot.restore();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private static final class Snapshot {

        private final boolean selectionEnabled;
        private final int selectionColour;
        private final float selectionThickness;
        private final float selectionOutlineMargin;
        private final int selectionBlinkAlpha;
        private final float selectionBlinkSpeed;
        private final boolean selectionRainbow;
        private final boolean selectionLink;
        private final boolean selectionHidden;
        private final boolean selectionFill;
        private final boolean selectionBlink;

        private final boolean crosshairEnabled;
        private final Object crosshairType;
        private final int crosshairColour;
        private final float crosshairWidth;
        private final float crosshairHeight;
        private final float crosshairGap;
        private final int crosshairThickness;
        private final int crosshairRotation;
        private final boolean crosshairOutline;
        private final int crosshairOutlineColour;
        private final float crosshairOutlineThickness;
        private final boolean crosshairDot;
        private final int crosshairDotColour;
        private final boolean crosshairRainbow;
        private final int crosshairRainbowSpeed;
        private final boolean crosshairVisible;
        private final boolean crosshairVisibleF1;
        private final boolean crosshairVisibleDebug;
        private final boolean crosshairVisibleThird;

        Snapshot() {
            selectionEnabled = SelectionBoxConfig.enabled;
            selectionColour = SelectionBoxConfig.colour;
            selectionThickness = SelectionBoxConfig.thickness;
            selectionOutlineMargin = SelectionBoxConfig.outlineMargin;
            selectionBlinkAlpha = SelectionBoxConfig.blinkAlpha;
            selectionBlinkSpeed = SelectionBoxConfig.blinkSpeed;
            selectionRainbow = SelectionBoxConfig.rainbow;
            selectionLink = SelectionBoxConfig.linkBlocks;
            selectionHidden = SelectionBoxConfig.showHidden;
            selectionFill = SelectionBoxConfig.showFill;
            selectionBlink = SelectionBoxConfig.blink;

            crosshairEnabled = CrosshairConfig.enabled;
            crosshairType = CrosshairConfig.type;
            crosshairColour = CrosshairConfig.colour;
            crosshairWidth = CrosshairConfig.width;
            crosshairHeight = CrosshairConfig.height;
            crosshairGap = CrosshairConfig.gap;
            crosshairThickness = CrosshairConfig.thickness;
            crosshairRotation = CrosshairConfig.rotation;
            crosshairOutline = CrosshairConfig.outline;
            crosshairOutlineColour = CrosshairConfig.outlineColour;
            crosshairOutlineThickness = CrosshairConfig.outlineThickness;
            crosshairDot = CrosshairConfig.dot;
            crosshairDotColour = CrosshairConfig.dotColour;
            crosshairRainbow = CrosshairConfig.rainbow;
            crosshairRainbowSpeed = CrosshairConfig.rainbowSpeed;
            crosshairVisible = CrosshairConfig.visibleDefault;
            crosshairVisibleF1 = CrosshairConfig.visibleHiddenGui;
            crosshairVisibleDebug = CrosshairConfig.visibleDebug;
            crosshairVisibleThird = CrosshairConfig.visibleThirdPerson;
        }

        void restore() {
            SelectionBoxConfig.enabled = selectionEnabled;
            SelectionBoxConfig.colour = selectionColour;
            SelectionBoxConfig.thickness = selectionThickness;
            SelectionBoxConfig.outlineMargin = selectionOutlineMargin;
            SelectionBoxConfig.blinkAlpha = selectionBlinkAlpha;
            SelectionBoxConfig.blinkSpeed = selectionBlinkSpeed;
            SelectionBoxConfig.rainbow = selectionRainbow;
            SelectionBoxConfig.linkBlocks = selectionLink;
            SelectionBoxConfig.showHidden = selectionHidden;
            SelectionBoxConfig.showFill = selectionFill;
            SelectionBoxConfig.blink = selectionBlink;

            CrosshairConfig.enabled = crosshairEnabled;
            CrosshairConfig.type = (me.kuionc.crosshairselection.render.CrosshairType) crosshairType;
            CrosshairConfig.colour = crosshairColour;
            CrosshairConfig.width = crosshairWidth;
            CrosshairConfig.height = crosshairHeight;
            CrosshairConfig.gap = crosshairGap;
            CrosshairConfig.thickness = crosshairThickness;
            CrosshairConfig.rotation = crosshairRotation;
            CrosshairConfig.outline = crosshairOutline;
            CrosshairConfig.outlineColour = crosshairOutlineColour;
            CrosshairConfig.outlineThickness = crosshairOutlineThickness;
            CrosshairConfig.dot = crosshairDot;
            CrosshairConfig.dotColour = crosshairDotColour;
            CrosshairConfig.rainbow = crosshairRainbow;
            CrosshairConfig.rainbowSpeed = crosshairRainbowSpeed;
            CrosshairConfig.visibleDefault = crosshairVisible;
            CrosshairConfig.visibleHiddenGui = crosshairVisibleF1;
            CrosshairConfig.visibleDebug = crosshairVisibleDebug;
            CrosshairConfig.visibleThirdPerson = crosshairVisibleThird;
        }
    }
}
