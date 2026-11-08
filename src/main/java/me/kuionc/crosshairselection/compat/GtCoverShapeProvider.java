package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.lang.reflect.Method;
import java.util.List;
import static me.kuionc.crosshairselection.compat.Reflect.*;

/**
 * GT machines and pipes can carry a cover plate on a face.
 *
 * <p>选择规则保持简单、可预测：
 * <ol>
 *   <li>命中面 {@code sideHit} 上正好有覆盖板时，直接描它；</li>
 *   <li>否则在六面里找离命中点最近、且距离在 {@link #NEAR} 以内的覆盖板
 *       （例如射线打在管道本体上、但贴着某块覆盖板）。</li>
 * </ol>
 *
 * <p>以前这里用「玩家眼睛 → 命中点」的延长线去和六块覆盖板做体积/平面求交，
 * 贴边时两个面的距离几乎相等，会逐帧在两面之间跳，表现就是选框快速左右晃。
 * 现在只依赖命中面与命中点距离，既短又稳定；贴边抖动由 {@code TargetLock}
 * 对 {@code sideHit} 的两帧确认兜底。
 */
public final class GtCoverShapeProvider implements ShapeProvider {

    private static final double THICKNESS = 2.0D / 16.0D;
    /** 命中点到覆盖板的最大距离；再远就认为没有瞄准覆盖板。 */
    private static final double NEAR = 0.06D;

    private static final Class<?> COVERABLE = classOrNull(
            "gregtech.api.interfaces.tileentity.ICoverable");
    private static final Method HAS_COVER = methodOrNull(COVERABLE, "hasCoverAtSide",
            ForgeDirection.class);

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (COVERABLE == null || HAS_COVER == null || target == null || block == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!COVERABLE.isInstance(tile)) {
            return false;
        }
        try {
            int side = target.sideHit;
            if (side >= 0 && side < 6 && hasCover(tile, side)) {
                out.add(OutlineShape.box(coverBox(x, y, z, side), false));
                return true;
            }
            Vec3 hit = target.hitVec;
            if (hit == null) {
                return false;
            }
            int bestSide = -1;
            double bestDistance = NEAR;
            for (int candidate = 0; candidate < 6; candidate++) {
                if (!hasCover(tile, candidate)) {
                    continue;
                }
                double distance = distanceToBox(coverBox(x, y, z, candidate), hit);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestSide = candidate;
                }
            }
            if (bestSide < 0) {
                return false;
            }
            out.add(OutlineShape.box(coverBox(x, y, z, bestSide), false));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
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

    private static double distanceToBox(AxisAlignedBB box, Vec3 point) {
        double dx = Math.max(0.0D, Math.max(box.minX - point.xCoord, point.xCoord - box.maxX));
        double dy = Math.max(0.0D, Math.max(box.minY - point.yCoord, point.yCoord - box.maxY));
        double dz = Math.max(0.0D, Math.max(box.minZ - point.zCoord, point.zCoord - box.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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
}
