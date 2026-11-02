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
        java.util.List<OutlineShape> shapes = ShapeCollector.collect(world, block, x, y, z, primary);

        int boxColour = GtOverlayConfig.useSelectionStyle ? SelectionBoxConfig.colour : GtOverlayConfig.colour;
        double margin = Math.max(0.0D, GtOverlayConfig.margin);
        if (GtOverlayConfig.useSelectionStyle) {
            margin += Math.max(0.0D, SelectionBoxConfig.outlineMargin);
        }
        float boxThickness = GtOverlayConfig.useSelectionStyle
                ? SelectionBoxConfig.thickness * Math.max(2.5F,
                        Minecraft.getMinecraft().displayWidth / 1920.0F * 2.5F)
                : GtOverlayConfig.thickness;
        boolean flow = GtOverlayConfig.useSelectionStyle
                && SelectionBoxConfig.rainbow && SelectionBoxConfig.rainbowFlow;
        long flowTime = System.currentTimeMillis();
        float flowSpeed = SelectionBoxConfig.rainbowSpeed;

        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_POLYGON_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        RenderHelper.bindWhiteTexture();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        if (SelectionBoxConfig.antiAlias) {
            GL11.glEnable(GL13.GL_MULTISAMPLE);
        } else {
            GL11.glDisable(GL13.GL_MULTISAMPLE);
        }
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -1.0F);

        GL11.glPushMatrix();
        if (offsetX != 0.0D || offsetY != 0.0D || offsetZ != 0.0D) {
            GL11.glTranslated(offsetX, offsetY, offsetZ);
        }
        for (OutlineShape shape : shapes) {
            drawOverlayShape(shape, cameraX, cameraY, cameraZ, boxColour, boxThickness,
                    margin, flow, flowTime, flowSpeed);
        }
        if (GtOverlayConfig.showGrid) {
            if (GtOverlayConfig.gridAllFaces) {
                for (int side = 0; side < 6; side++) {
                    drawGrid(x, y, z, side, cameraX, cameraY, cameraZ, targetSides,
                            flow, flowTime, flowSpeed);
                }
            } else {
                drawGrid(x, y, z, target.sideHit, cameraX, cameraY, cameraZ, targetSides,
                        flow, flowTime, flowSpeed);
            }
        }
        GL11.glPopMatrix();

        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
        return true;
    }

    private static void drawOverlayShape(OutlineShape shape, double cameraX, double cameraY, double cameraZ,
                                          int colour, float thickness, double margin,
                                          boolean flow, long flowTime, float flowSpeed) {
        if (shape.getKind() == OutlineShape.Kind.UNION) {
            UnionOutlineRenderer.draw(shape.getBoxes(), cameraX, cameraY, cameraZ,
                    colour, thickness, margin, flow, flowTime, flowSpeed);
            return;
        }
        if (shape.getKind() != OutlineShape.Kind.BOX) {
            return;
        }
        AxisAlignedBB box = BoxUtil.expandWithinBlock(shape.getBox(), margin);
        if (flow) {
            FlowRenderer.drawBox3D(box, cameraX, cameraY, cameraZ,
                    colour, flowTime, flowSpeed, 8, thickness);
        } else {
            ThickLineRenderer.drawBox3D(box, cameraX, cameraY, cameraZ, colour, thickness);
        }
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

    private static void drawGrid(int x, int y, int z, int side, double cameraX, double cameraY, double cameraZ,
                                 java.util.Set<Integer> targetSides, boolean flow, long flowTime,
                                 float flowSpeed) {
        double[] normal = faceNormal(side);
        double[] u = faceU(side);
        double[] v = faceV(side);
        double radians = Math.toRadians(0.0F);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double ux = u[0] * cos + v[0] * sin;
        double uy = u[1] * cos + v[1] * sin;
        double uz = u[2] * cos + v[2] * sin;
        double vx = -u[0] * sin + v[0] * cos;
        double vy = -u[1] * sin + v[1] * cos;
        double vz = -u[2] * sin + v[2] * cos;

        double inset = Math.max(0.0D, Math.min(0.25D, GtOverlayConfig.gridInset));
        double half = GtGridMath.half(inset, 0.0F);
        double cx = x + 0.5D + normal[0] * 0.502D;
        double cy = y + 0.5D + normal[1] * 0.502D;
        double cz = z + 0.5D + normal[2] * 0.502D;
        double[] positions = GtGridMath.linePositions(inset, 0.0F,
                GtOverlayConfig.gridCellSize);
        int colour = gridColour();
        float thickness = gridThickness();

        for (double position : positions) {
            drawGridLine(cx + ux * position - vx * half, cy + uy * position - vy * half,
                    cz + uz * position - vz * half,
                    cx + ux * position + vx * half, cy + uy * position + vy * half,
                    cz + uz * position + vz * half,
                    cameraX, cameraY, cameraZ, colour, thickness,
                    flow, flowTime, flowSpeed);
            drawGridLine(cx - ux * half + vx * position, cy - uy * half + vy * position,
                    cz - uz * half + vz * position,
                    cx + ux * half + vx * position, cy + uy * half + vy * position,
                    cz + uz * half + vz * position,
                    cameraX, cameraY, cameraZ, colour, thickness,
                    flow, flowTime, flowSpeed);
        }

        if (GtOverlayConfig.showInputFaceX && targetSides != null && !targetSides.isEmpty()) {
            for (int col = 0; col < 3; col++) {
                for (int row = 0; row < 3; row++) {
                    int targetSide = GtGridMath.sideForCell(side, col, row);
                    if (targetSide >= 0 && targetSides.contains(targetSide)) {
                        drawCellX(cx, cy, cz, ux, uy, uz, vx, vy, vz,
                                col, row, half, cameraX, cameraY, cameraZ, colour, thickness,
                                flow, flowTime, flowSpeed);
                    }
                }
            }
        }
    }

    private static void drawCellX(double cx, double cy, double cz,
                                  double ux, double uy, double uz,
                                  double vx, double vy, double vz,
                                  int col, int row, double half,
                                  double cameraX, double cameraY, double cameraZ,
                                  int colour, float thickness, boolean flow, long flowTime,
                                  float flowSpeed) {
        if (GtOverlayConfig.xStyle == me.kuionc.crosshairselection.config.GtXStyle.FILL) {
            drawCellFill(cx, cy, cz, ux, uy, uz, vx, vy, vz, col, row,
                    cameraX, cameraY, cameraZ, colour);
            return;
        }
        if (GtOverlayConfig.xStyle == me.kuionc.crosshairselection.config.GtXStyle.FULL_X) {
            double[] bounds = GtGridMath.cellBounds(col, row, GtOverlayConfig.gridInset,
                    0.0F, GtOverlayConfig.gridCellSize);
            double u0 = bounds[0] - 0.5D;
            double u1 = bounds[1] - 0.5D;
            double v0 = bounds[2] - 0.5D;
            double v1 = bounds[3] - 0.5D;
            drawGridLine(cx + ux * u0 + vx * v0, cy + uy * u0 + vy * v0, cz + uz * u0 + vz * v0,
                    cx + ux * u1 + vx * v1, cy + uy * u1 + vy * v1, cz + uz * u1 + vz * v1,
                    cameraX, cameraY, cameraZ, colour, thickness, flow, flowTime, flowSpeed);
            drawGridLine(cx + ux * u0 + vx * v1, cy + uy * u0 + vy * v1, cz + uz * u0 + vz * v1,
                    cx + ux * u1 + vx * v0, cy + uy * u1 + vy * v0, cz + uz * u1 + vz * v0,
                    cameraX, cameraY, cameraZ, colour, thickness, flow, flowTime, flowSpeed);
            return;
        }

        double[] center = GtGridMath.cellCenter(col, row, GtOverlayConfig.gridInset,
                0.0F, GtOverlayConfig.gridCellSize);
        double pu = center[0] - 0.5D;
        double pv = center[1] - 0.5D;
        double px = cx + ux * pu + vx * pv;
        double py = cy + uy * pu + vy * pv;
        double pz = cz + uz * pu + vz * pv;
        double edge = Math.min(Math.max(0.05D, Math.min(0.45D, GtOverlayConfig.gridCellSize)), half * 0.8D);
        double size = Math.min(0.12D, Math.min(edge, half - edge) * 0.45D);

        if (GtOverlayConfig.xStyle == me.kuionc.crosshairselection.config.GtXStyle.ARROW) {
            double du = col == 0 ? -1.0D : col == 2 ? 1.0D : 0.0D;
            double dv = row == 0 ? -1.0D : row == 2 ? 1.0D : 0.0D;
            if (du == 0.0D && dv == 0.0D) {
                du = 1.0D;
            }
            double len = Math.sqrt(du * du + dv * dv);
            du /= len;
            dv /= len;
            double tipX = px + (ux * du + vx * dv) * size;
            double tipY = py + (uy * du + vy * dv) * size;
            double tipZ = pz + (uz * du + vz * dv) * size;
            double baseX = px - (ux * du + vx * dv) * size * 0.55D;
            double baseY = py - (uy * du + vy * dv) * size * 0.55D;
            double baseZ = pz - (uz * du + vz * dv) * size * 0.55D;
            double sideU = -dv * size * 0.7D;
            double sideV = du * size * 0.7D;
            drawGridLine(baseX + ux * sideU + vx * sideV, baseY + uy * sideU + vy * sideV,
                    baseZ + uz * sideU + vz * sideV, tipX, tipY, tipZ,
                    cameraX, cameraY, cameraZ, colour, thickness, flow, flowTime, flowSpeed);
            drawGridLine(baseX - ux * sideU - vx * sideV, baseY - uy * sideU - vy * sideV,
                    baseZ - uz * sideU - vz * sideV, tipX, tipY, tipZ,
                    cameraX, cameraY, cameraZ, colour, thickness, flow, flowTime, flowSpeed);
            return;
        }

        drawGridLine(px - ux * size - vx * size, py - uy * size - vy * size,
                pz - uz * size - vz * size,
                px + ux * size + vx * size, py + uy * size + vy * size,
                pz + uz * size + vz * size,
                cameraX, cameraY, cameraZ, colour, thickness * 1.15F,
                flow, flowTime, flowSpeed);
        drawGridLine(px - ux * size + vx * size, py - uy * size + vy * size,
                pz - uz * size + vz * size,
                px + ux * size - vx * size, py + uy * size - vy * size,
                pz + uz * size - vz * size,
                cameraX, cameraY, cameraZ, colour, thickness * 1.15F,
                flow, flowTime, flowSpeed);
    }

    private static void drawCellFill(double cx, double cy, double cz,
                                     double ux, double uy, double uz,
                                     double vx, double vy, double vz,
                                     int col, int row,
                                     double cameraX, double cameraY, double cameraZ,
                                     int colour) {
        double[] bounds = GtGridMath.cellBounds(col, row, GtOverlayConfig.gridInset,
                0.0F, GtOverlayConfig.gridCellSize);
        double u0 = bounds[0] - 0.5D;
        double u1 = bounds[1] - 0.5D;
        double v0 = bounds[2] - 0.5D;
        double v1 = bounds[3] - 0.5D;
        int fill = (colour & 0x00FFFFFF) | 0x66000000;
        FlowRenderer.drawQuad3D(
                cx + ux * u0 + vx * v0, cy + uy * u0 + vy * v0, cz + uz * u0 + vz * v0,
                cx + ux * u1 + vx * v0, cy + uy * u1 + vy * v0, cz + uz * u1 + vz * v0,
                cx + ux * u1 + vx * v1, cy + uy * u1 + vy * v1, cz + uz * u1 + vz * v1,
                cx + ux * u0 + vx * v1, cy + uy * u0 + vy * v1, cz + uz * u0 + vz * v1,
                cameraX, cameraY, cameraZ, fill);
    }

    private static int gridColour() {
        return GtOverlayConfig.useSelectionStyle ? SelectionBoxConfig.colour : GtOverlayConfig.gridColour;
    }

    private static float gridThickness() {
        return GtOverlayConfig.useSelectionStyle
                ? SelectionBoxConfig.thickness * Math.max(2.5F,
                        Minecraft.getMinecraft().displayWidth / 1920.0F * 2.5F)
                : GtOverlayConfig.gridThickness;
    }

    private static void drawGridLine(double x1, double y1, double z1, double x2, double y2, double z2,
                                     double cameraX, double cameraY, double cameraZ,
                                     int colour, float thickness, boolean flow, long flowTime,
                                     float flowSpeed) {
        if (flow) {
            FlowRenderer.drawLine3D(x1, y1, z1, x2, y2, z2, cameraX, cameraY, cameraZ,
                    (colour >>> 24) & 255, flowTime, flowSpeed, 1, thickness);
        } else {
            ThickLineRenderer.drawLine3D(x1, y1, z1, x2, y2, z2,
                    cameraX, cameraY, cameraZ, colour, thickness);
        }
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
