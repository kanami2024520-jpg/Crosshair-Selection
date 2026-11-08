package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import java.util.List;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Custom block selection box. Supports normal boxes and cylindrical outlines for pipes.
 */
public final class SelectionBoxRenderer {

    private static World lastWorld;
    private static boolean hasSelection;
    private static int lastX;
    private static int lastY;
    private static int lastZ;
    private static int fromX;
    private static int fromY;
    private static int fromZ;
    private static long transitionStart;

    private SelectionBoxRenderer() {
    }

    public static boolean render(EntityPlayer player, MovingObjectPosition hitResult, float partialTicks) {
        if (!SelectionBoxConfig.enabled || player == null) {
            resetSmooth();
            return false;
        }
        if (hitResult == null || hitResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            TargetLock.reset();
            resetSmooth();
            return false;
        }
        World world = player.worldObj;
        hitResult = TargetLock.stable(world, hitResult);
        int x = hitResult.blockX;
        int y = hitResult.blockY;
        int z = hitResult.blockZ;
        long now = System.currentTimeMillis();
        if (lastWorld != world) {
            resetSmooth();
            lastWorld = world;
        }
        double offsetX = 0.0D;
        double offsetY = 0.0D;
        double offsetZ = 0.0D;
        if (!hasSelection) {
            hasSelection = true;
            lastX = x;
            lastY = y;
            lastZ = z;
            transitionStart = 0L;
        } else if (x != lastX || y != lastY || z != lastZ) {
            fromX = lastX;
            fromY = lastY;
            fromZ = lastZ;
            lastX = x;
            lastY = y;
            lastZ = z;
            transitionStart = now;
        }
        if (SelectionBoxConfig.smoothTransition && transitionStart > 0L) {
            float duration = Math.max(0.05F, SelectionBoxConfig.transitionDuration) * 1000.0F;
            float t = (now - transitionStart) / duration;
            if (t >= 1.0F) {
                transitionStart = 0L;
            } else {
                double eased = 1.0D - Math.pow(1.0D - t, 3.0D);
                offsetX = (fromX - x) * (1.0D - eased);
                offsetY = (fromY - y) * (1.0D - eased);
                offsetZ = (fromZ - z) * (1.0D - eased);
            }
        }
        Block block = world.getBlock(x, y, z);
        AxisAlignedBB primary = getSelectedBox(world, block, x, y, z);
        if (primary == null) {
            return false;
        }
        List<OutlineShape> shapes = ShapeCollector.collect(world, block, x, y, z, primary, hitResult);

        double cameraX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double cameraY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double cameraZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        int colour = SelectionBoxConfig.colour;
        if (SelectionBoxConfig.flow.isActive()) {
            // The flow owns the hue, saturation and brightness; the fixed base colour only keeps
            // the alpha. Hard-coding 0.8 here made the brightness slider do nothing.
            me.kuionc.crosshairselection.config.FlowSettings flow = SelectionBoxConfig.flow;
            int alpha = ColorUtil.alpha(colour);
            int rgb = ColorUtil.hsbToRgb(System.currentTimeMillis() % 10000L / 10000.0F,
                    flow.saturation, flow.brightness);
            colour = ColorUtil.withAlpha(rgb, alpha);
        }
        int blinkAlpha;
        if (!SelectionBoxConfig.showFill) {
            blinkAlpha = 0;
        } else if (SelectionBoxConfig.blink && SelectionBoxConfig.blinkSpeed > 0.0F) {
            blinkAlpha = Math.round(SelectionBoxConfig.blinkAlpha * (float) Math.abs(
                    Math.sin(System.currentTimeMillis() / 100.0D * SelectionBoxConfig.blinkSpeed)));
        } else {
            blinkAlpha = SelectionBoxConfig.blinkAlpha;
        }
        int innerColour = ColorUtil.withAlpha(colour, blinkAlpha);
        boolean flow = SelectionBoxConfig.flow.mode != me.kuionc.crosshairselection.config.FlowSettings.Mode.OFF;
        long flowTime = System.currentTimeMillis();
        float flowSpeed = SelectionBoxConfig.rainbowSpeed;
        float thickness = outlineThickness();

        RenderBackend.beginWorldOutlines();
        RenderBackend.worldDepthMode(SelectionBoxConfig.showHidden);

        // The smooth transition offset is added to the camera-relative coordinates instead of the
        // model-view matrix, which keeps the outline pipeline matrix-free.
        double drawCameraX = cameraX - offsetX;
        double drawCameraY = cameraY - offsetY;
        double drawCameraZ = cameraZ - offsetZ;

        // The sweep progress is frame-wide, so it is resolved once before the ribbon batch opens.
        RibbonBuffer.setProgress(sweepProgress());
        // 填充先画完再画描边：否则多形状时后一个形状的填充会盖住前一个的边框，
        // 在关闭深度测试（透视模式）或光影下尤其明显。
        double[] bounds = boundsOf(shapes);
        drawFills(shapes, innerColour, drawCameraX, drawCameraY, drawCameraZ, bounds);

        boolean ribbons = RibbonBuffer.begin(colour, SelectionBoxConfig.flow,
                drawCameraX, drawCameraY, drawCameraZ, bounds);
        for (OutlineShape shape : shapes) {
            drawOutline(shape, drawCameraX, drawCameraY, drawCameraZ, colour, thickness,
                    flow, flowTime, flowSpeed);
        }
        if (ribbons) {
            RibbonBuffer.end();
        }
        RenderBackend.endWorldOutlines();

        if (SelectionBoxConfig.showBreakProgress) {
            BreakProgressRenderer.render(hitResult, drawCameraX, drawCameraY, drawCameraZ, colour);
        }

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        return true;
    }

    /** Physical-pixel outline width, including the display-size scaling. */
    public static float outlineThickness() {
        return SelectionBoxConfig.thickness * Math.max(2.5F,
                McRef.get().displayWidth / 1920.0F * 2.5F);
    }

    /**
     * Break progress drives the sweep on the selection outline. The player keeps mining past the
     * block breaking, so the visible cracking counter is used instead of the raw tool speed.
     */
    /** Manual sweep position; break progress is drawn separately by BreakProgressRenderer. */
    private static float sweepProgress() {
        return SelectionBoxConfig.flow.progress;
    }

    /** 只画填充的那一趟；几何与描边共用同一批形状。 */
    private static void drawFills(List<OutlineShape> shapes, int innerColour,
                                  double cameraX, double cameraY, double cameraZ,
                                  double[] bounds) {
        if (!SelectionBoxConfig.showFill || ColorUtil.alpha(innerColour) <= 2
                || !FillBuffer.begin(innerColour, SelectionBoxConfig.flow,
                cameraX, cameraY, cameraZ, bounds)) {
            return;
        }
        double margin = Math.max(0.0D, SelectionBoxConfig.outlineMargin);
        for (OutlineShape shape : shapes) {
            if (shape.getKind() != OutlineShape.Kind.BOX || !shape.isFill()) {
                continue;
            }
            AxisAlignedBB box = shape.getBox();
            if (margin > 0.0D) {
                box = BoxUtil.expandWithinBlock(box, margin);
            }
            FillBuffer.addBox(box);
        }
        FillBuffer.end();
    }

    /**
     * 所有形状的世界坐标包围盒，供流光/扫光把自己的进度映射到形状范围上。
     * 返回 {@code {minX,minY,minZ,maxX,maxY,maxZ}}，没有形状时返回 null。
     */
    private static double[] boundsOf(List<OutlineShape> shapes) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;
        for (OutlineShape shape : shapes) {
            if (shape.getKind() == OutlineShape.Kind.CYLINDER) {
                double[] box = cylinderBounds(shape);
                minX = Math.min(minX, box[0]);
                minY = Math.min(minY, box[1]);
                minZ = Math.min(minZ, box[2]);
                maxX = Math.max(maxX, box[3]);
                maxY = Math.max(maxY, box[4]);
                maxZ = Math.max(maxZ, box[5]);
            } else if (shape.getKind() == OutlineShape.Kind.UNION) {
                for (AxisAlignedBB box : shape.getBoxes()) {
                    minX = Math.min(minX, box.minX);
                    minY = Math.min(minY, box.minY);
                    minZ = Math.min(minZ, box.minZ);
                    maxX = Math.max(maxX, box.maxX);
                    maxY = Math.max(maxY, box.maxY);
                    maxZ = Math.max(maxZ, box.maxZ);
                }
            } else {
                AxisAlignedBB box = shape.getBox();
                minX = Math.min(minX, box.minX);
                minY = Math.min(minY, box.minY);
                minZ = Math.min(minZ, box.minZ);
                maxX = Math.max(maxX, box.maxX);
                maxY = Math.max(maxY, box.maxY);
                maxZ = Math.max(maxZ, box.maxZ);
            }
        }
        if (minX > maxX) {
            return null;
        }
        return new double[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    private static double[] cylinderBounds(OutlineShape shape) {
        double[] min = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] max = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        int axis = shape.getAxis();
        double radius = shape.getRadius();
        for (int end = 0; end < 2; end++) {
            double along = end == 0 ? shape.getAxisMin() : shape.getAxisMax();
            for (int i = 0; i < 8; i++) {
                double[] point = cylinderPoint(axis, along, shape.getCenter1(), shape.getCenter2(),
                        radius, Math.PI * 2.0D * i / 8.0D);
                for (int a = 0; a < 3; a++) {
                    min[a] = Math.min(min[a], point[a]);
                    max[a] = Math.max(max[a], point[a]);
                }
            }
        }
        return new double[]{min[0], min[1], min[2], max[0], max[1], max[2]};
    }

    private static void drawOutline(OutlineShape shape, double cameraX, double cameraY, double cameraZ,
                                    int outlineColour, float thickness,
                                    boolean flow, long flowTime, float flowSpeed) {
        double margin = Math.max(0.0D, SelectionBoxConfig.outlineMargin);
        if (shape.getKind() == OutlineShape.Kind.UNION) {
            UnionOutlineRenderer.draw(shape.getBoxes(), cameraX, cameraY, cameraZ,
                    outlineColour, thickness, margin, flow, flowTime, flowSpeed);
            return;
        }
        if (shape.getKind() == OutlineShape.Kind.BOX) {
            AxisAlignedBB box = shape.getBox();
            if (margin > 0.0D) {
                box = BoxUtil.expandWithinBlock(box, margin);
            }
            OutlineSegmentRenderer.drawBox3D(box, cameraX, cameraY, cameraZ, outlineColour, thickness);
        } else {
            drawCylinder(shape, cameraX, cameraY, cameraZ, outlineColour, thickness);
        }
    }

    private static void drawCylinder(OutlineShape shape, double cameraX, double cameraY, double cameraZ,
                                     int colour, float thickness) {
        int axis = shape.getAxis();
        double radius = shape.getRadius();
        double min = shape.getAxisMin();
        double max = shape.getAxisMax();
        double c1 = shape.getCenter1();
        double c2 = shape.getCenter2();
        int segments = 20;
        int longitudinal = 8;

        for (int end = 0; end < 2; end++) {
            double along = end == 0 ? min : max;
            for (int i = 0; i < segments; i++) {
                double[] a = cylinderPoint(axis, along, c1, c2, radius, Math.PI * 2.0D * i / segments);
                double[] b = cylinderPoint(axis, along, c1, c2, radius, Math.PI * 2.0D * (i + 1) / segments);
                OutlineSegmentRenderer.drawLine(a[0], a[1], a[2], b[0], b[1], b[2],
                        cameraX, cameraY, cameraZ, colour, thickness);
            }
        }
        for (int i = 0; i < longitudinal; i++) {
            double angle = Math.PI * 2.0D * i / longitudinal;
            double[] a = cylinderPoint(axis, min, c1, c2, radius, angle);
            double[] b = cylinderPoint(axis, max, c1, c2, radius, angle);
            OutlineSegmentRenderer.drawLine(a[0], a[1], a[2], b[0], b[1], b[2],
                    cameraX, cameraY, cameraZ, colour, thickness);
        }
    }

    /** World-space point on a pipe's rim. */
    private static double[] cylinderPoint(int axis, double along, double c1, double c2,
                                          double radius, double angle) {
        double u = Math.cos(angle) * radius;
        double v = Math.sin(angle) * radius;
        if (axis == 0) {
            return new double[]{along, c1 + u, c2 + v};
        }
        if (axis == 1) {
            return new double[]{c1 + u, along, c2 + v};
        }
        return new double[]{c1 + u, c2 + v, along};
    }

    private static void resetSmooth() {
        TargetLock.reset();
        lastWorld = null;
        hasSelection = false;
        transitionStart = 0L;
    }

    public static AxisAlignedBB getSelectedBox(World world, Block block, int x, int y, int z) {
        if (block == null) {
            return null;
        }
        block.setBlockBoundsBasedOnState(world, x, y, z);
        return block.getSelectedBoundingBoxFromPool(world, x, y, z);
    }
}
