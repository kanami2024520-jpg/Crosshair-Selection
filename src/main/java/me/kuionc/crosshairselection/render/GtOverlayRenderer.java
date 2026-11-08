package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Replacement for GT's wrench/cover grid. Draws the block outline and the 3x3 face grid with
 * independent style controls.
 */
public final class GtOverlayRenderer {

    private static final Class<?> GT_TILE = classOrNull("gregtech.api.interfaces.tileentity.IGregTechTileEntity");

    private static net.minecraft.world.World lastWorld;
    private static boolean hasSelection;
    private static int lastX;
    private static int lastY;
    private static int lastZ;
    private static int fromX;
    private static int fromY;
    private static int fromZ;
    private static long transitionStart;

    private static net.minecraft.world.World renderedWorld;
    private static int renderedX;
    private static int renderedY;
    private static int renderedZ;
    private static int renderedSide;

    private GtOverlayRenderer() {
    }

    public static boolean render(EntityPlayer player, MovingObjectPosition target, float partialTicks) {
        if (player == null || target == null || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return false;
        }
        World world = player.worldObj;
        int x = target.blockX;
        int y = target.blockY;
        int z = target.blockZ;
        Block block = world.getBlock(x, y, z);
        if (block == null) {
            resetSmooth();
            return false;
        }
        // 九格和外框都关掉时不接管渲染：把黑色默认九格还给 GT。
        if (!GtOverlayConfig.showGrid && !GtOverlayConfig.showBlockOutline) {
            resetSmooth();
            return false;
        }

        renderedWorld = world;
        renderedX = x;
        renderedY = y;
        renderedZ = z;
        renderedSide = target.sideHit;

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
        if (GtOverlayConfig.smoothTransition && transitionStart > 0L) {
            float duration = Math.max(0.05F, GtOverlayConfig.transitionDuration) * 1000.0F;
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

        java.util.Set<Integer> targetSides = targetSides(world, x, y, z);
        double cameraX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double cameraY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double cameraZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        AxisAlignedBB primary = SelectionBoxRenderer.getSelectedBox(world, block, x, y, z);
        if (primary == null) {
            return false;
        }
        java.util.List<OutlineShape> shapes = ShapeCollector.collect(world, block, x, y, z, primary, target);

        // "Use selection style" makes the pipe outline follow the selection box exactly, but only
        // while the 9-grid is on: with the grid disabled the overlay must not inherit the
        // selection colour (which can be any hue), so it falls back to the GT colour (black).
        boolean selectionStyle = GtOverlayConfig.useSelectionStyle && GtOverlayConfig.showGrid;
        int boxColour = selectionStyle
                ? SelectionBoxConfig.colour : GtOverlayConfig.colour;
        double margin = Math.max(0.0D, SelectionBoxConfig.outlineMargin);
        float boxThickness = selectionStyle
                ? SelectionBoxRenderer.outlineThickness() : GtOverlayConfig.thickness;

        RenderBackend.beginWorldOutlines();
        RenderBackend.worldDepthMode(false);

        // The transition offset is applied to the camera-relative coordinates, so no matrix push
        // is needed for the grid and the block outline.
        double drawCameraX = cameraX - offsetX;
        double drawCameraY = cameraY - offsetY;
        double drawCameraZ = cameraZ - offsetZ;

        // One batch per frame; every segment carries its own width, so the block outline and the
        // grid no longer have to share one thickness. The selection flow animates the pipe
        // outline only when the selection style is in use; the grid never animates.
        boolean ribbons = RibbonBuffer.begin(boxColour,
                selectionStyle ? SelectionBoxConfig.flow : null,
                drawCameraX, drawCameraY, drawCameraZ);
        if (GtOverlayConfig.showBlockOutline) {
            for (OutlineShape shape : shapes) {
                drawOverlayShape(shape, drawCameraX, drawCameraY, drawCameraZ, boxColour, boxThickness,
                        margin);
            }
        }
        if (GtOverlayConfig.showGrid) {
            if (GtOverlayConfig.gridAllFaces) {
                for (int side = 0; side < 6; side++) {
                    drawGrid(x, y, z, side, targetSides, drawCameraX, drawCameraY, drawCameraZ);
                }
            } else {
                drawGrid(x, y, z, target.sideHit, targetSides, drawCameraX, drawCameraY, drawCameraZ);
            }
        }
        if (ribbons) {
            RibbonBuffer.end();
        }
        RenderBackend.endWorldOutlines();
        return true;
    }

    private static void drawOverlayShape(OutlineShape shape, double cameraX, double cameraY, double cameraZ,
                                          int colour, float thickness, double margin) {
        if (shape.getKind() == OutlineShape.Kind.UNION) {
            for (AxisAlignedBB box : shape.getBoxes()) {
                AxisAlignedBB expanded = margin > 0.0D ? box.expand(margin, margin, margin) : box;
                OutlineSegmentRenderer.drawBox3D(expanded, cameraX, cameraY, cameraZ, colour, thickness);
            }
            return;
        }
        if (shape.getKind() != OutlineShape.Kind.BOX) {
            return;
        }
        // The extra GT margin is applied outside the block on purpose: expandWithinBlock clamps to
        // the block cell, so a full block would never show any margin at all.
        AxisAlignedBB box = margin > 0.0D ? shape.getBox().expand(margin, margin, margin) : shape.getBox();
        OutlineSegmentRenderer.drawBox3D(box, cameraX, cameraY, cameraZ, colour, thickness);
    }

    private static void resetSmooth() {
        lastWorld = null;
        hasSelection = false;
        transitionStart = 0L;
    }

    /**
     * True when the GT overlay is currently being drawn for this exact block and face. The
     * renderer is the authority for click remapping: reflection-based tool detection can fail
     * on some server environments, while a rendered overlay already proves that GT called its
     * wrench/cover grid for the held item.
     */
    public static boolean isRenderedTarget(net.minecraft.world.World world,
                                           int x, int y, int z, int side) {
        if (!GtOverlayConfig.showGrid || renderedWorld == null || renderedWorld != world) {
            return false;
        }
        if (x != renderedX || y != renderedY || z != renderedZ) {
            return false;
        }
        return GtOverlayConfig.gridAllFaces || side == renderedSide;
    }

    private static void drawGrid(int x, int y, int z, int side, java.util.Set<Integer> targetSides,
                                 double cameraX, double cameraY, double cameraZ) {
        double[] normal = faceNormal(side);
        double[] ux = faceU(side);
        double[] uy = faceV(side);
        double cx = x + 0.5D + normal[0] * 0.502D;
        double cy = y + 0.5D + normal[1] * 0.502D;
        double cz = z + 0.5D + normal[2] * 0.502D;
        double inset = Math.max(0.0D, Math.min(0.25D, GtOverlayConfig.gridInset));
        int colour = gridColour();
        float thickness = gridThickness();

        for (GtFaceGeometry.Segment line : GtFaceGeometry.gridLines(inset, GtOverlayConfig.gridCellSize)) {
            drawFaceSegment(cx, cy, cz, ux, uy, line, cameraX, cameraY, cameraZ, colour, thickness);
        }

        if (GtOverlayConfig.showInputFaceX && targetSides != null && !targetSides.isEmpty()) {
            for (int col = 0; col < 3; col++) {
                for (int row = 0; row < 3; row++) {
                    int targetSide = GtGridMath.sideForCell(side, col, row);
                    if (targetSide < 0 || !targetSides.contains(targetSide)) {
                        continue;
                    }
                    GtFaceGeometry.Cell fill = GtFaceGeometry.markerFill(col, row);
                    if (fill != null) {
                        drawFaceCell(cx, cy, cz, ux, uy, fill, cameraX, cameraY, cameraZ,
                                GtFaceGeometry.markerFillColour(colour));
                        continue;
                    }
                    for (GtFaceGeometry.Segment mark : GtFaceGeometry.markerLines(col, row)) {
                        drawFaceSegment(cx, cy, cz, ux, uy, mark, cameraX, cameraY, cameraZ,
                                colour, thickness);
                    }
                }
            }
        }
    }

    /** Maps a face-space segment to world space. u runs along ux/uy, v runs along -ux/uy. */
    private static void drawFaceSegment(double cx, double cy, double cz,
                                        double[] ux, double[] uy,
                                        GtFaceGeometry.Segment line,
                                        double cameraX, double cameraY, double cameraZ,
                                        int colour, float thickness) {
        double du1 = line.u1 - 0.5D;
        double dv1 = line.v1 - 0.5D;
        double du2 = line.u2 - 0.5D;
        double dv2 = line.v2 - 0.5D;
        double x1 = cx + ux[0] * du1 + uy[0] * dv1;
        double y1 = cy + ux[1] * du1 + uy[1] * dv1;
        double z1 = cz + ux[2] * du1 + uy[2] * dv1;
        double x2 = cx + ux[0] * du2 + uy[0] * dv2;
        double y2 = cy + ux[1] * du2 + uy[1] * dv2;
        double z2 = cz + ux[2] * du2 + uy[2] * dv2;
        OutlineSegmentRenderer.drawLine(x1, y1, z1, x2, y2, z2,
                cameraX, cameraY, cameraZ, colour, thickness * line.weight);
    }

    private static void drawFaceCell(double cx, double cy, double cz,
                                     double[] ux, double[] uy,
                                     GtFaceGeometry.Cell cell,
                                     double cameraX, double cameraY, double cameraZ,
                                     int colour) {
        double u0 = cell.u1 - 0.5D;
        double u1 = cell.u2 - 0.5D;
        double v0 = cell.v1 - 0.5D;
        double v1 = cell.v2 - 0.5D;
        FlowRenderer.drawQuad3D(
                cx + ux[0] * u0 + uy[0] * v0, cy + ux[1] * u0 + uy[1] * v0, cz + ux[2] * u0 + uy[2] * v0,
                cx + ux[0] * u1 + uy[0] * v0, cy + ux[1] * u1 + uy[1] * v0, cz + ux[2] * u1 + uy[2] * v0,
                cx + ux[0] * u1 + uy[0] * v1, cy + ux[1] * u1 + uy[1] * v1, cz + ux[2] * u1 + uy[2] * v1,
                cx + ux[0] * u0 + uy[0] * v1, cy + ux[1] * u0 + uy[1] * v1, cz + ux[2] * u0 + uy[2] * v1,
                cameraX, cameraY, cameraZ, colour);
    }

    private static int gridColour() {
        // With the selection style in use the grid inherits its colour, matching the pipe outline.
        return GtOverlayConfig.useSelectionStyle ? SelectionBoxConfig.colour : GtOverlayConfig.gridColour;
    }

    private static float gridThickness() {
        return GtOverlayConfig.gridThickness;
    }

    private static java.util.Set<Integer> targetSides(World world, int x, int y, int z) {
        java.util.Set<Integer> result = new java.util.LinkedHashSet<Integer>();
        if (world == null) {
            return result;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile == null) {
            return result;
        }
        try {
            java.lang.reflect.Field connections = tile.getClass().getField("mConnections");
            int mask = connections.getByte(tile) & 255;
            for (int side = 0; side < 6; side++) {
                if ((mask & (1 << side)) != 0) {
                    result.add(side);
                }
            }
        } catch (Throwable ignored) {
            // Not a pipe.
        }
        try {
            java.lang.reflect.Method frontMethod = tile.getClass().getMethod("getFrontFacing");
            Object front = frontMethod.invoke(tile);
            if (front instanceof net.minecraftforge.common.util.ForgeDirection) {
                result.add(((net.minecraftforge.common.util.ForgeDirection) front).ordinal());
            }
        } catch (Throwable ignored) {
            // Not a machine.
        }
        return result;
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static java.lang.reflect.Method methodOrNull(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static double[] faceNormal(int side) {
        switch (side) {
            case 0:
                return new double[]{0.0D, -1.0D, 0.0D};
            case 1:
                return new double[]{0.0D, 1.0D, 0.0D};
            case 2:
                return new double[]{0.0D, 0.0D, -1.0D};
            case 3:
                return new double[]{0.0D, 0.0D, 1.0D};
            case 4:
                return new double[]{-1.0D, 0.0D, 0.0D};
            case 5:
            default:
                return new double[]{1.0D, 0.0D, 0.0D};
        }
    }

    private static double[] faceU(int side) {
        switch (side) {
            case 2:
            case 3:
                return new double[]{1.0D, 0.0D, 0.0D};
            case 4:
            case 5:
                return new double[]{0.0D, 0.0D, 1.0D};
            default:
                return new double[]{1.0D, 0.0D, 0.0D};
        }
    }

    private static double[] faceV(int side) {
        switch (side) {
            case 0:
            case 1:
                return new double[]{0.0D, 0.0D, 1.0D};
            default:
                return new double[]{0.0D, 1.0D, 0.0D};
        }
    }
}
