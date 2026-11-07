package me.kuionc.crosshairselection.gui;

import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.render.CrosshairRenderer;
import me.kuionc.crosshairselection.render.FlowPreview;
import me.kuionc.crosshairselection.render.GtFaceGeometry;
import me.kuionc.crosshairselection.render.SelectionBoxRenderer;
import me.kuionc.crosshairselection.render.RenderBackend;
import me.kuionc.crosshairselection.render.RenderHelper;
import me.kuionc.crosshairselection.config.GtXStyle;
import net.minecraft.client.gui.ScaledResolution;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Shared preview panel used by every settings page.
 *
 * <p>The panel is a single persistent component: entering the colour sub-page keeps it visible in
 * the same place, because it is driven by {@link #begin} rather than being owned by one screen.
 * It renders a single face so the outline, the GT grid and the crosshair share one look.
 */
public final class PreviewPanel {

    public enum Content {
        CUBE,
        FACE,
        CROSSHAIR
    }

    private static final int BACKGROUND = 0xC0101010;
    private static final int BORDER = 0x60FFFFFF;
    private static final int PANEL_WIDTH = 140;
    private static final int PANEL_HEIGHT = 100;

    /** 预览用的准星渲染器：每帧复用，不再每次新建。 */
    private static final CrosshairRenderer CROSSHAIR_PREVIEW = new CrosshairRenderer();

    private static Content content = Content.CUBE;
    private static FlowSettings flow;

    private PreviewPanel() {
    }

    /** Sets what the panel shows; call from the screen that owns the settings. */
    public static void begin(Content target, FlowSettings settings) {
        content = target;
        flow = settings;
    }

    public static int width() {
        return PANEL_WIDTH;
    }

    public static int height() {
        return PANEL_HEIGHT;
    }

    /** Centred position of the panel, used by every page so it never moves between them. */
    public static int centreLeft(int screenWidth) {
        return screenWidth / 2 - PANEL_WIDTH / 2;
    }

    public static int centreTop(int screenHeight) {
        return screenHeight / 2 - PANEL_HEIGHT / 2;
    }

    public static void drawCentred(int screenWidth, int screenHeight) {
        draw(centreLeft(screenWidth), centreTop(screenHeight));
    }

    /**
     * @param left left edge in GUI pixels
     * @param top  top edge in GUI pixels
     */
    public static void draw(int left, int top) {
        RenderBackend.beginGuiOverlay();
        RenderHelper.drawRect(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, BACKGROUND);
        // Checkerboard backdrop so thin outlines stay readable; the scissor keeps the pattern from
        // bleeding past the panel and the previous integer division left a dark fringe.
        RenderBackend.beginScissor(left + 1, top + 1, PANEL_WIDTH - 2, PANEL_HEIGHT - 2, 1);
        RenderHelper.drawCheckerboardClipped(left, top, PANEL_WIDTH, PANEL_HEIGHT, 8);
        RenderBackend.endScissor();
        RenderHelper.drawOutline(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 1.0F, BORDER);

        int cx = left + PANEL_WIDTH / 2;
        int cy = top + PANEL_HEIGHT / 2;
        switch (content) {
            case CROSSHAIR:
                // 准星现在是全屏四边形 + 片元 discard，不需要也不应该再套 scissor。
                // drawPreview() 把动态状态固定为静止，预览因此严格等于设置本身；
                // 坐标由它内部乘上 GUI 缩放换算成物理像素，这里传面板中心（GUI 坐标）。
                CROSSHAIR_PREVIEW.drawPreview(cx, cy);
                break;
            case FACE:
                // GT preview draws the block outline plus the grid and input marker, so margins
                // and thickness changes are visible.
                drawFace(cx, cy, 30, flow, true);
                break;
            case CUBE:
            default:
                drawCube(cx, cy, 34, flow);
                break;
        }
        RenderBackend.endGuiOverlay();
    }

    /**
     * Isometric cube: twelve edges coloured with the same formula the world shader uses.
     *
     * <p>The corners are mapped to world units of a real block, so the band density and the sweep
     * direction look the same here as around a block in game.
     */
    private static void drawCube(int cx, int cy, int radius, FlowSettings flow) {
        float[][] corners = {
                {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
                {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
        };
        float[][] p = new float[corners.length][2];
        for (int i = 0; i < corners.length; i++) {
            p[i] = project(corners[i], cx, cy, radius);
        }
        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {3, 0},
                {4, 5}, {5, 6}, {6, 7}, {7, 4},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        long now = System.currentTimeMillis();
        int base = SelectionBoxConfig.colour;
        for (int[] edge : edges) {
            drawFlowSegment(p[edge[0]][0], p[edge[0]][1], toWorld(corners[edge[0]]),
                    p[edge[1]][0], p[edge[1]][1], toWorld(corners[edge[1]]),
                    flow, base, now, 1.5F);
        }
    }

    /**
     * Splits a previewed segment into small pieces and colours each one at its centre, so the band
     * direction and the sweep mask match the shader instead of averaging into a single colour.
     */
    private static void drawFlowSegment(float x1, float y1, double[] world1,
                                        float x2, float y2, double[] world2,
                                        FlowSettings flow, int base, long now, float width) {
        int pieces = flow != null && flow.mode != FlowSettings.Mode.OFF ? 10 : 1;
        for (int i = 0; i < pieces; i++) {
            float t0 = i / (float) pieces;
            float t1 = (i + 1) / (float) pieces;
            float mid = (t0 + t1) * 0.5F;
            double wx = world1[0] + (world2[0] - world1[0]) * mid;
            double wy = world1[1] + (world2[1] - world1[1]) * mid;
            double wz = world1[2] + (world2[2] - world1[2]) * mid;
            int colour = FlowPreview.colourAt(flow, base, now, wx, wy, wz, mid);
            drawSegment(lerp(x1, x2, t0), lerp(y1, y2, t0), lerp(x1, x2, t1), lerp(y1, y2, t1),
                    colour, width);
        }
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Preview corners are -1..1; one block is two units, so world = corner * 0.5. */
    private static double[] toWorld(float[] corner) {
        return new double[]{corner[0] * 0.5D, corner[1] * 0.5D, corner[2] * 0.5D};
    }

    private static float[] project(float[] v, int cx, int cy, int radius) {
        // 30 / 22 keeps every projected edge separate, so all twelve stay visible.
        double ry = Math.toRadians(30.0D);
        double rx = Math.toRadians(22.0D);
        double x = v[0] * Math.cos(ry) + v[2] * Math.sin(ry);
        double z = -v[0] * Math.sin(ry) + v[2] * Math.cos(ry);
        double y = v[1] * Math.cos(rx) - z * Math.sin(rx);
        return new float[]{cx + (float) x * radius, cy + (float) y * radius};
    }

    private static void drawSegment(float x1, float y1, float x2, float y2, int colour, float width) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001F) {
            return;
        }
        float nx = -dy / length * width * 0.5F;
        float ny = dx / length * width * 0.5F;
        // Screen-space quad through the tessellator; the world shaders are not available here.
        RenderHelper.drawLineSegment(x1, y1, x2, y2, nx, ny, colour);
    }

    /**
     * GT face preview built from {@link GtFaceGeometry}, the same geometry the world overlay uses,
     * so cell size, inset and every marker style stay in sync by construction.
     */
    private static void drawFace(int cx, int cy, int half, FlowSettings flow, boolean grid) {
        long now = System.currentTimeMillis();
        int base = SelectionBoxConfig.colour;
        int flowColour = FlowPreview.solidColour(flow, base, now);
        // The pipe outline always follows the selection box, exactly like GtOverlayRenderer.
        // Mirrors GtOverlayRenderer: the grid follows the selection colour while the style is used.
        int outlineColour = GtOverlayConfig.useSelectionStyle ? flowColour : GtOverlayConfig.colour;
        int gridColour = GtOverlayConfig.useSelectionStyle ? flowColour : GtOverlayConfig.gridColour;
        float scale = guiScale();

        if (GtOverlayConfig.showBlockOutline) {
            // Thickness is configured in physical pixels; the panel draws in GUI pixels.
            int thickness = Math.max(1, Math.round((GtOverlayConfig.useSelectionStyle
                    ? SelectionBoxRenderer.outlineThickness() : GtOverlayConfig.thickness) / scale));
            // The GT page no longer has its own margin; the selection margin is the only one.
            float margin = SelectionBoxConfig.outlineMargin;
            int outer = half + Math.round(margin * 60.0F);
            RenderHelper.drawRect(cx - outer - thickness, cy - outer - thickness,
                    cx + outer + thickness, cy - outer, outlineColour);
            RenderHelper.drawRect(cx - outer - thickness, cy + outer,
                    cx + outer + thickness, cy + outer + thickness, outlineColour);
            RenderHelper.drawRect(cx - outer - thickness, cy - outer,
                    cx - outer, cy + outer, outlineColour);
            RenderHelper.drawRect(cx + outer, cy - outer,
                    cx + outer + thickness, cy + outer, outlineColour);
        }

        if (!grid || !GtOverlayConfig.showGrid) {
            return;
        }

        float lineWidth = Math.max(0.5F, GtOverlayConfig.gridThickness / scale);
        double inset = Math.max(0.0D, Math.min(0.25D, GtOverlayConfig.gridInset));
        for (GtFaceGeometry.Segment line : GtFaceGeometry.gridLines(inset, GtOverlayConfig.gridCellSize)) {
            drawFaceSegment(cx, cy, half, line, lineWidth, gridColour);
        }

        if (!GtOverlayConfig.showInputFaceX) {
            return;
        }
        if (GtOverlayConfig.xStyle == GtXStyle.ARROW) {
            // Arrows point outwards from every edge cell, so showing them explains the style; the
            // centre cell gets the dot the world overlay draws.
            for (int col = 0; col < 3; col++) {
                for (int row = 0; row < 3; row++) {
                    if (col == 1 && row == 1) {
                        continue;
                    }
                    if (Math.abs(col - 1) + Math.abs(row - 1) != 1) {
                        continue;
                    }
                    for (GtFaceGeometry.Segment mark : GtFaceGeometry.markerLines(col, row, inset,
                            GtOverlayConfig.gridCellSize, GtOverlayConfig.xStyle)) {
                        drawFaceSegment(cx, cy, half, mark, lineWidth, gridColour);
                    }
                }
            }
        }
        // The preview targets the face the crosshair points at, which is the centre cell's side.
        int col = 1;
        int row = 1;
        GtFaceGeometry.Cell fill = GtFaceGeometry.markerFill(col, row);
        if (fill != null) {
            int u0 = faceX(cx, half, fill.u1);
            int u1 = faceX(cx, half, fill.u2);
            int v0 = faceY(cy, half, fill.v1);
            int v1 = faceY(cy, half, fill.v2);
            RenderHelper.drawRect(Math.min(u0, u1), Math.min(v0, v1),
                    Math.max(u0, u1), Math.max(v0, v1), GtFaceGeometry.markerFillColour(gridColour));
        } else {
            for (GtFaceGeometry.Segment mark : GtFaceGeometry.markerLines(col, row, inset,
                    GtOverlayConfig.gridCellSize, GtOverlayConfig.xStyle)) {
                drawFaceSegment(cx, cy, half, mark, lineWidth, gridColour);
            }
        }
    }

    /** GUI scale, so physical-pixel settings map to the same visual size as in game. */
    private static float guiScale() {
        return Math.max(1, new ScaledResolution(McRef.get(),
                McRef.get().displayWidth, McRef.get().displayHeight)
                .getScaleFactor());
    }

    /** Face u (0..1) to preview pixels; v runs downwards on screen, matching the world face. */
    private static int faceX(int cx, int half, double u) {
        return Math.round(cx + (float) ((u - 0.5D) * 2.0D * half));
    }

    private static int faceY(int cy, int half, double v) {
        return Math.round(cy + (float) ((v - 0.5D) * 2.0D * half));
    }

    private static void drawFaceSegment(int cx, int cy, int half, GtFaceGeometry.Segment line,
                                        float width, int colour) {
        float x1 = faceX(cx, half, line.u1);
        float y1 = faceY(cy, half, line.v1);
        float x2 = faceX(cx, half, line.u2);
        float y2 = faceY(cy, half, line.v2);
        float w = Math.max(1.0F, width * line.weight);
        float dx = x2 - x1;
        float dy = y2 - y1;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 0.001F) {
            return;
        }
        float nx = -dy / length * w * 0.5F;
        float ny = dx / length * w * 0.5F;
        RenderHelper.drawLineSegment(x1, y1, x2, y2, nx, ny, colour);
    }


}
