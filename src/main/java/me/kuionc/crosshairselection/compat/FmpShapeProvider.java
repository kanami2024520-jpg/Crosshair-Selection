package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static me.kuionc.crosshairselection.compat.Reflect.*;

/**
 * Forge Multipart: every part knows its render/collision/occlusion bounds. The hit data selects
 * the part under the crosshair; occlusion boxes calibrate the slightly larger collision boxes
 * back to the visible wire body.
 *
 * <p><b>注意：</b>如果命中的部件是 AE 的 {@code appeng.fmp.CableBusPart}，不要在这里处理。
 * 它的 {@code getCollisionBoxes()} 返回整条总线（线缆 + 终端 + 伪装板）的并集，
 * 在这里画会变成「两个 AE 部件一起被选上」。这类方块由 {@link Ae2ShapeProvider} 先接手
 * （它在 {@code ShapeCollector} 里的顺序更靠前），用 AE 自己的 {@code selectPart} 精确到一个部件。
 * 这条路径正是被 ProjectRed 红色合金丝触发的：它的存在会把 AE 总线转换成 FMP 部件。
 */
public final class FmpShapeProvider implements ShapeProvider {

    private static final Class<?> MULTIPART_TILE = classOrNull("codechicken.multipart.TileMultipart");
    private static final Method PART_LIST = methodOrNull(MULTIPART_TILE, "jPartList");

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (MULTIPART_TILE == null || PART_LIST == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!MULTIPART_TILE.isInstance(tile)) {
            return false;
        }
        try {
            List<?> parts = (List<?>) PART_LIST.invoke(tile);
            if (parts == null || parts.isEmpty()) {
                return false;
            }
            int partIndex = HitData.partIndex(target);
            Iterable<?> selectedParts = parts;
            if (partIndex >= 0 && partIndex < parts.size()) {
                selectedParts = Collections.singletonList(parts.get(partIndex));
            }

            List<AxisAlignedBB> allBoxes = new ArrayList<AxisAlignedBB>();
            boolean added = false;
            for (Object part : selectedParts) {
                added |= collectPart(part, x, y, z, allBoxes);
            }
            if (!added || allBoxes.isEmpty()) {
                return false;
            }
            out.add(OutlineShape.union(allBoxes));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean collectPart(Object part, int x, int y, int z, List<AxisAlignedBB> out) {
        AxisAlignedBB render = renderBounds(part);
        List<AxisAlignedBB> collisions = new ArrayList<AxisAlignedBB>();
        Iterable<?> collisionBoxes = collisionBoxes(part);
        if (collisionBoxes != null) {
            for (Object cuboid : collisionBoxes) {
                AxisAlignedBB box = toAabb(cuboid);
                if (box != null && !isFull(box)) {
                    collisions.add(box);
                }
            }
        }

        List<AxisAlignedBB> partBoxes = new ArrayList<AxisAlignedBB>();
        // Wires/cables return their real sub-boxes from getCollisionBoxes(), while the
        // generic getRenderBounds() default is the full block. Prefer the precise boxes.
        if (render != null && !isFull(render)) {
            partBoxes.add(render);
        } else if (!collisions.isEmpty()) {
            partBoxes.addAll(collisions);
        } else if (render != null) {
            partBoxes.add(render);
        } else {
            AxisAlignedBB bounds = genericBounds(part);
            if (bounds != null) {
                partBoxes.add(bounds);
            }
        }

        if (partBoxes.isEmpty()) {
            return false;
        }

        // ProjectRed's collision boxes are intentionally a little larger than the wire
        // model for easier clicking. Calibrate them against the occlusion box so the
        // outline hugs the visible wire instead of drawing a ring around it.
        partBoxes = calibrate(partBoxes, occlusionBoxes(part));
        for (AxisAlignedBB box : partBoxes) {
            out.add(offset(box, x, y, z));
        }
        return true;
    }

    private static List<AxisAlignedBB> calibrate(List<AxisAlignedBB> boxes,
                                                  List<AxisAlignedBB> occlusion) {
        if (boxes.isEmpty() || occlusion.isEmpty()) {
            return boxes;
        }
        AxisAlignedBB occ = occlusion.get(0);
        AxisAlignedBB best = null;
        double bestVolume = 0.0D;
        for (AxisAlignedBB box : boxes) {
            double overlapX = Math.min(box.maxX, occ.maxX) - Math.max(box.minX, occ.minX);
            double overlapY = Math.min(box.maxY, occ.maxY) - Math.max(box.minY, occ.minY);
            double overlapZ = Math.min(box.maxZ, occ.maxZ) - Math.max(box.minZ, occ.minZ);
            if (overlapX <= 0.0D || overlapY <= 0.0D || overlapZ <= 0.0D) {
                continue;
            }
            double volume = overlapX * overlapY * overlapZ;
            if (volume > bestVolume) {
                bestVolume = volume;
                best = box;
            }
        }
        if (best == null) {
            return boxes;
        }

        double insetMinX = Math.max(0.0D, occ.minX - best.minX);
        double insetMinY = Math.max(0.0D, occ.minY - best.minY);
        double insetMinZ = Math.max(0.0D, occ.minZ - best.minZ);
        double insetMaxX = Math.max(0.0D, best.maxX - occ.maxX);
        double insetMaxY = Math.max(0.0D, best.maxY - occ.maxY);
        double insetMaxZ = Math.max(0.0D, best.maxZ - occ.maxZ);
        if (insetMinX + insetMinY + insetMinZ + insetMaxX + insetMaxY + insetMaxZ < 1.0E-5D) {
            return boxes;
        }

        List<AxisAlignedBB> result = new ArrayList<AxisAlignedBB>(boxes.size());
        for (AxisAlignedBB box : boxes) {
            double minX = box.minX + insetMinX;
            double minY = box.minY + insetMinY;
            double minZ = box.minZ + insetMinZ;
            double maxX = box.maxX - insetMaxX;
            double maxY = box.maxY - insetMaxY;
            double maxZ = box.maxZ - insetMaxZ;
            if (minX < maxX && minY < maxY && minZ < maxZ) {
                result.add(AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
            }
        }
        return result.isEmpty() ? boxes : result;
    }

    private static boolean isFull(AxisAlignedBB box) {
        return box.minX <= 0.0001D && box.minY <= 0.0001D && box.minZ <= 0.0001D
                && box.maxX >= 0.9999D && box.maxY >= 0.9999D && box.maxZ >= 0.9999D;
    }

    private static AxisAlignedBB genericBounds(Object part) {
        try {
            Method method = part.getClass().getMethod("getBounds");
            Object cuboid = method.invoke(part);
            return toAabb(cuboid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AxisAlignedBB renderBounds(Object part) {
        try {
            Method method = part.getClass().getMethod("getRenderBounds");
            Object cuboid = method.invoke(part);
            return toAabb(cuboid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Iterable<?> collisionBoxes(Object part) {
        try {
            Method method = part.getClass().getMethod("getCollisionBoxes");
            Object value = method.invoke(part);
            return value instanceof Iterable ? (Iterable<?>) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<AxisAlignedBB> occlusionBoxes(Object part) {
        List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
        try {
            Method method = part.getClass().getMethod("getOcclusionBoxes");
            Object value = method.invoke(part);
            if (value instanceof Iterable) {
                for (Object cuboid : (Iterable<?>) value) {
                    AxisAlignedBB box = toAabb(cuboid);
                    if (box != null && !isFull(box)) {
                        boxes.add(box);
                    }
                }
            } else if (value instanceof Object[]) {
                for (Object cuboid : (Object[]) value) {
                    AxisAlignedBB box = toAabb(cuboid);
                    if (box != null && !isFull(box)) {
                        boxes.add(box);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Occlusion boxes are optional.
        }
        return boxes;
    }

    private static AxisAlignedBB toAabb(Object cuboid) {
        if (cuboid == null) {
            return null;
        }
        try {
            Method method = cuboid.getClass().getMethod("toAABB");
            Object value = method.invoke(cuboid);
            return value instanceof AxisAlignedBB ? (AxisAlignedBB) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AxisAlignedBB offset(AxisAlignedBB box, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(box.minX + x, box.minY + y, box.minZ + z,
                box.maxX + x, box.maxY + y, box.maxZ + z);
    }

}
