package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.lang.reflect.Method;
import java.util.List;
import me.kuionc.crosshairselection.util.McRef;
import static me.kuionc.crosshairselection.compat.Reflect.*;

/**
 * GT machines and pipes can carry a cover plate on a face. The block shape is still the pipe
 * or machine, so when the crosshair ray hits a cover, the selection outline should follow that
 * cover plate instead of the underlying block.
 */
public final class GtCoverShapeProvider implements ShapeProvider {

    private static final double THICKNESS = 2.0D / 16.0D;

    private static final Class<?> COVERABLE = classOrNull(
            "gregtech.api.interfaces.tileentity.ICoverable");
    private static final Method HAS_COVER = methodOrNull(COVERABLE, "hasCoverAtSide",
            ForgeDirection.class);

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (COVERABLE == null || HAS_COVER == null || target == null || block == null
                || target.sideHit < 0) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!COVERABLE.isInstance(tile)) {
            return false;
        }
        try {
            EntityPlayer player = McRef.get().thePlayer;
            Vec3 eye = player == null ? null : Vec3.createVectorHelper(
                    player.posX, player.posY + player.getEyeHeight(), player.posZ);
            Vec3 hit = target.hitVec;

            int coverSide = eye == null || hit == null ? -1
                    : selectCoverSide(tile, eye, hit, x, y, z);
            AxisAlignedBB bestBox = coverSide >= 0 ? coverBox(x, y, z, coverSide) : null;
            if (bestBox == null && hasCover(tile, target.sideHit)) {
                bestBox = coverBox(x, y, z, target.sideHit);
            }
            if (bestBox == null) {
                return false;
            }
            out.add(OutlineShape.box(bestBox, false));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int selectCoverSide(TileEntity tile, Vec3 eye, Vec3 hit,
                                       int x, int y, int z) {
        double dx = hit.xCoord - eye.xCoord;
        double dy = hit.yCoord - eye.yCoord;
        double dz = hit.zCoord - eye.zCoord;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6D) {
            return -1;
        }
        Vec3 end = Vec3.createVectorHelper(
                hit.xCoord + dx / length,
                hit.yCoord + dy / length,
                hit.zCoord + dz / length);

        int volumeSide = -1;
        double bestDistance = Double.MAX_VALUE;
        for (int side = 0; side < 6; side++) {
            if (!hasCover(tile, side)) {
                continue;
            }
            AxisAlignedBB detect = coverDetectBox(x, y, z, side);
            MovingObjectPosition intercept = detect.calculateIntercept(eye, end);
            if (intercept != null && intercept.hitVec != null) {
                double distance = intercept.hitVec.distanceTo(eye);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    volumeSide = side;
                }
            }
        }
        if (volumeSide >= 0) {
            return volumeSide;
        }

        int bestSide = -1;
        double bestT = Double.MAX_VALUE;
        for (int side = 0; side < 6; side++) {
            if (!hasCover(tile, side)) {
                continue;
            }
            double denominator;
            double t;
            double ix;
            double iy;
            double iz;
            switch (side) {
                case 0:
                    denominator = -dy;
                    if (denominator >= -1.0E-6D || Math.abs(dy) < 1.0E-6D) {
                        continue;
                    }
                    t = (y - eye.yCoord) / dy;
                    break;
                case 1:
                    denominator = dy;
                    if (denominator >= -1.0E-6D || Math.abs(dy) < 1.0E-6D) {
                        continue;
                    }
                    t = (y + 1.0D - eye.yCoord) / dy;
                    break;
                case 2:
                    denominator = -dz;
                    if (denominator >= -1.0E-6D || Math.abs(dz) < 1.0E-6D) {
                        continue;
                    }
                    t = (z - eye.zCoord) / dz;
                    break;
                case 3:
                    denominator = dz;
                    if (denominator >= -1.0E-6D || Math.abs(dz) < 1.0E-6D) {
                        continue;
                    }
                    t = (z + 1.0D - eye.zCoord) / dz;
                    break;
                case 4:
                    denominator = -dx;
                    if (denominator >= -1.0E-6D || Math.abs(dx) < 1.0E-6D) {
                        continue;
                    }
                    t = (x - eye.xCoord) / dx;
                    break;
                default:
                    denominator = dx;
                    if (denominator >= -1.0E-6D || Math.abs(dx) < 1.0E-6D) {
                        continue;
                    }
                    t = (x + 1.0D - eye.xCoord) / dx;
                    break;
            }
            if (t <= 1.0E-6D || t >= bestT) {
                continue;
            }
            ix = eye.xCoord + dx * t;
            iy = eye.yCoord + dy * t;
            iz = eye.zCoord + dz * t;
            boolean inside;
            if (side <= 1) {
                inside = inside(ix, x, x + 1.0D) && inside(iz, z, z + 1.0D);
            } else if (side <= 3) {
                inside = inside(ix, x, x + 1.0D) && inside(iy, y, y + 1.0D);
            } else {
                inside = inside(iz, z, z + 1.0D) && inside(iy, y, y + 1.0D);
            }
            if (inside) {
                bestT = t;
                bestSide = side;
            }
        }
        return bestSide;
    }

    private static boolean inside(double value, double min, double max) {
        return value >= min - 1.0E-6D && value <= max + 1.0E-6D;
    }

    public static boolean hasCover(World world, int x, int y, int z, int side) {
        if (COVERABLE == null || HAS_COVER == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return COVERABLE.isInstance(tile) && hasCover(tile, side);
    }

    public static AxisAlignedBB getCoverBox(World world, int x, int y, int z, int side) {
        return hasCover(world, x, y, z, side) ? coverBox(x, y, z, side) : null;
    }

    private static boolean hasCover(TileEntity tile, int side) {
        try {
            Object result = HAS_COVER.invoke(tile, ForgeDirection.getOrientation(side));
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static AxisAlignedBB coverBox(int x, int y, int z, int side) {
        double minX = x;
        double minY = y;
        double minZ = z;
        double maxX = x + 1.0D;
        double maxY = y + 1.0D;
        double maxZ = z + 1.0D;
        switch (side) {
            case 0:
                maxY = y + THICKNESS;
                break;
            case 1:
                minY = y + 1.0D - THICKNESS;
                break;
            case 2:
                maxZ = z + THICKNESS;
                break;
            case 3:
                minZ = z + 1.0D - THICKNESS;
                break;
            case 4:
                maxX = x + THICKNESS;
                break;
            case 5:
            default:
                minX = x + 1.0D - THICKNESS;
                break;
        }
        return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static AxisAlignedBB coverDetectBox(int x, int y, int z, int side) {
        double pad = THICKNESS + 0.02D;
        double minX = x;
        double minY = y;
        double minZ = z;
        double maxX = x + 1.0D;
        double maxY = y + 1.0D;
        double maxZ = z + 1.0D;
        switch (side) {
            case 0:
                maxY = y + pad;
                break;
            case 1:
                minY = y + 1.0D - pad;
                break;
            case 2:
                maxZ = z + pad;
                break;
            case 3:
                minZ = z + 1.0D - pad;
                break;
            case 4:
                maxX = x + pad;
                break;
            case 5:
            default:
                minX = x + 1.0D - pad;
                break;
        }
        return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

}
