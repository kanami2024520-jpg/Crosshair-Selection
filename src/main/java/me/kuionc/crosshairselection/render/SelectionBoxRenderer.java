package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.util.List;

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
        if (!SelectionBoxConfig.enabled || player == null || hitResult == null
                || hitResult.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            resetSmooth();
            return false;
        }
        World world = player.worldObj;
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
        List<OutlineShape> shapes = ShapeCollector.collect(world, block, x, y, z, primary);

        double cameraX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double cameraY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double cameraZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        int colour = SelectionBoxConfig.colour;
        if (SelectionBoxConfig.rainbow) {
            int alpha = ColorUtil.alpha(colour);
            int rgb = ColorUtil.hsbToRgb(System.currentTimeMillis() % 10000L / 10000.0F, 0.8F, 0.8F);
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
        boolean flow = SelectionBoxConfig.rainbow && SelectionBoxConfig.rainbowFlow;
        long flowTime = System.currentTimeMillis();
        float flowSpeed = SelectionBoxConfig.rainbowSpeed;
        float thickness = SelectionBoxConfig.thickness * Math.max(2.5F,
                Minecraft.getMinecraft().displayWidth / 1920.0F * 2.5F);

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_POLYGON_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        RenderHelper.bindWhiteTexture();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        if (SelectionBoxConfig.antiAlias) {
            GL11.glEnable(GL13.GL_MULTISAMPLE);
        } else {
            GL11.glDisable(GL13.GL_MULTISAMPLE);
        }
        GL11.glDepthMask(false);
        if (SelectionBoxConfig.showHidden) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE);
        GL11.glPolygonOffset(-1.0F, -1.0F);

        GL11.glPushMatrix();
        if (offsetX != 0.0D || offsetY != 0.0D || offsetZ != 0.0D) {
            GL11.glTranslated(offsetX, offsetY, offsetZ);
        }
        for (OutlineShape shape : shapes) {
            drawShape(shape, cameraX, cameraY, cameraZ, colour, innerColour, thickness,
                    flow, flowTime, flowSpeed);
        }
        GL11.glPopMatrix();

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(1.0F);
        GL11.glPopAttrib();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        return true;
    }

    private static void drawShape(OutlineShape shape, double cameraX, double cameraY, double cameraZ,
                                  int outlineColour, int innerColour, float thickness,
                                  boolean flow, long flowTime, float flowSpeed) {
        double margin = Math.max(0.0D, SelectionBoxConfig.outlineMargin);
        if (shape.getKind() == OutlineShape.Kind.UNION) {
            UnionOutlineRenderer.draw(shape.getBoxes(), cameraX, cameraY, cameraZ,
                    outlineColour, thickness, margin, flow, flowTime, flowSpeed);
            return;
        }
        if (shape.getKind() == OutlineShape.Kind.BOX) {
            AxisAlignedBB box = shape.getBox().getOffsetBoundingBox(-cameraX, -cameraY, -cameraZ);
            if (margin > 0.0D) {
                box = box.expand(margin, margin, margin);
            }
            if (shape.isFill() && SelectionBoxConfig.showFill && ColorUtil.alpha(innerColour) > 2) {
                drawFilled(box, innerColour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
            }
            GL11.glLineWidth(Math.max(1.0F, thickness));
            drawOutline(box, outlineColour, thickness, flow, flowTime, flowSpeed,
                    cameraX, cameraY, cameraZ);
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

        RenderHelper.setColor(colour);
        GL11.glLineWidth(Math.max(1.0F, thickness));
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_LINES);

        for (int end = 0; end < 2; end++) {
            double along = end == 0 ? min : max;
            for (int i = 0; i < segments; i++) {
                double angle1 = Math.PI * 2.0D * i / segments;
                double angle2 = Math.PI * 2.0D * (i + 1) / segments;
                addCylinderVertex(tessellator, axis, along, c1, c2, radius, angle1, cameraX, cameraY, cameraZ);
                addCylinderVertex(tessellator, axis, along, c1, c2, radius, angle2, cameraX, cameraY, cameraZ);
            }
        }
        for (int i = 0; i < longitudinal; i++) {
            double angle = Math.PI * 2.0D * i / longitudinal;
            addCylinderVertex(tessellator, axis, min, c1, c2, radius, angle, cameraX, cameraY, cameraZ);
            addCylinderVertex(tessellator, axis, max, c1, c2, radius, angle, cameraX, cameraY, cameraZ);
        }
        tessellator.draw();
    }

    private static void addCylinderVertex(Tessellator tessellator, int axis, double along,
                                          double c1, double c2, double radius, double angle,
                                          double cameraX, double cameraY, double cameraZ) {
        double u = Math.cos(angle) * radius;
        double v = Math.sin(angle) * radius;
        double x;
        double y;
        double z;
        if (axis == 0) {
            x = along;
            y = c1 + u;
            z = c2 + v;
        } else if (axis == 1) {
            x = c1 + u;
            y = along;
            z = c2 + v;
        } else {
            x = c1 + u;
            y = c2 + v;
            z = along;
        }
        tessellator.addVertexWithUV(x - cameraX, y - cameraY, z - cameraZ, 0.5D, 0.5D);
    }

    private static void drawOutline(AxisAlignedBB box, int colour, float thickness,
                                    boolean flow, long flowTime,
                                    float flowSpeed, double cameraX, double cameraY, double cameraZ) {
        AxisAlignedBB worldBox = box.getOffsetBoundingBox(cameraX, cameraY, cameraZ);
        if (flow) {
            FlowRenderer.drawBox3D(worldBox, cameraX, cameraY, cameraZ,
                    colour, flowTime, flowSpeed, 8, thickness);
            return;
        }
        ThickLineRenderer.drawBox3D(worldBox, cameraX, cameraY, cameraZ, colour, thickness);
        if (true) {
            return;
        }
        RenderHelper.setColor(colour);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_LINES);
        line(tessellator, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        line(tessellator, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        tessellator.draw();
    }

    private static void drawFilled(AxisAlignedBB box, int colour, boolean flow, long flowTime,
                                   float flowSpeed, double cameraX, double cameraY, double cameraZ) {
        if (flow) {
            FlowRenderer.drawFilledBox3D(box.getOffsetBoundingBox(cameraX, cameraY, cameraZ),
                    cameraX, cameraY, cameraZ, colour, flowTime, flowSpeed, 6);
            return;
        }
        RenderHelper.setColor(colour);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        quad(tessellator, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ,
                box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        quad(tessellator, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ,
                box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        quad(tessellator, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        quad(tessellator, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ,
                box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        quad(tessellator, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ,
                box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        quad(tessellator, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ,
                box.maxX, box.maxY, box.maxZ, box.maxX, box.minY, box.maxZ,
                colour, flow, flowTime, flowSpeed, cameraX, cameraY, cameraZ);
        tessellator.draw();
    }

    private static void line(Tessellator tessellator, double x1, double y1, double z1,
                             double x2, double y2, double z2, int colour, boolean flow,
                             long flowTime, float flowSpeed, double cameraX, double cameraY, double cameraZ) {
        if (flow) {
            int alpha = ColorUtil.alpha(colour);
            RenderHelper.setVertexColor(tessellator,
                    Rainbow.colorAt(x1 + cameraX, y1 + cameraY, z1 + cameraZ, flowTime, flowSpeed, alpha));
        }
        tessellator.addVertexWithUV(x1, y1, z1, 0.5D, 0.5D);
        if (flow) {
            int alpha = ColorUtil.alpha(colour);
            RenderHelper.setVertexColor(tessellator,
                    Rainbow.colorAt(x2 + cameraX, y2 + cameraY, z2 + cameraZ, flowTime, flowSpeed, alpha));
        }
        tessellator.addVertexWithUV(x2, y2, z2, 0.5D, 0.5D);
    }

    private static void quad(Tessellator tessellator,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             double x3, double y3, double z3, double x4, double y4, double z4,
                             int colour, boolean flow, long flowTime, float flowSpeed,
                             double cameraX, double cameraY, double cameraZ) {
        if (flow) {
            int alpha = ColorUtil.alpha(colour);
            RenderHelper.setVertexColor(tessellator,
                    Rainbow.colorAt(x1 + cameraX, y1 + cameraY, z1 + cameraZ, flowTime, flowSpeed, alpha));
        }
        tessellator.addVertexWithUV(x1, y1, z1, 0.5D, 0.5D);
        if (flow) {
            int alpha = ColorUtil.alpha(colour);
            RenderHelper.setVertexColor(tessellator,
                    Rainbow.colorAt(x2 + cameraX, y2 + cameraY, z2 + cameraZ, flowTime, flowSpeed, alpha));
        }
        tessellator.addVertexWithUV(x2, y2, z2, 0.5D, 0.5D);
        if (flow) {
            int alpha = ColorUtil.alpha(colour);
            RenderHelper.setVertexColor(tessellator,
                    Rainbow.colorAt(x3 + cameraX, y3 + cameraY, z3 + cameraZ, flowTime, flowSpeed, alpha));
        }
        tessellator.addVertexWithUV(x3, y3, z3, 0.5D, 0.5D);
        if (flow) {
            int alpha = ColorUtil.alpha(colour);
            RenderHelper.setVertexColor(tessellator,
                    Rainbow.colorAt(x4 + cameraX, y4 + cameraY, z4 + cameraZ, flowTime, flowSpeed, alpha));
        }
        tessellator.addVertexWithUV(x4, y4, z4, 0.5D, 0.5D);
    }

    private static void resetSmooth() {
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
