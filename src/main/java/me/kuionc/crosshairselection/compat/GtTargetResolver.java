package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.SelectionBoxRenderer;
import me.kuionc.crosshairselection.render.ShapeCollector;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Refines the target for thin GT pipes, cables and cover plates.
 *
 * <p><b>只在原方块内部修正形状，绝不改变方块坐标。</b>
 *
 * <p>以前这里会在射线终点周围扫描 5x5x5 的方块，并允许把目标替换成“更远”的 GT 方块。
 * 结果是：指向 AE 接口 / 空气时，射线后面的 GT 总线会被选中，选框画在后面、点击也作用在后面。
 * 现在规则收紧为：
 *
 * <ol>
 *   <li>原命中必须是方块且是 GT 方块，否则直接返回 null（保留 AE、EnderIO 等原目标）；</li>
 *   <li>只在原命中的这个方块内，用覆盖板/管道的实际形状做求交；</li>
 *   <li>目标方块坐标永远不变，因此不可能“穿透”到后面的方块。</li>
 * </ol>
 */
public final class GtTargetResolver {

    private GtTargetResolver() {
    }

    public static MovingObjectPosition resolve(EntityPlayer player, float partialTicks,
                                               MovingObjectPosition vanilla) {
        if (player == null || player.worldObj == null) {
            return null;
        }
        // 关闭 GT 覆盖层时不做任何目标修正，完全回到原版判定。
        if (!GtOverlayConfig.enabled) {
            return null;
        }
        // 只修正“已经命中了一个 GT 方块”的情况；空气、实体、普通方块一律不替换。
        if (vanilla == null || vanilla.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return null;
        }
        World world = player.worldObj;
        int x = vanilla.blockX;
        int y = vanilla.blockY;
        int z = vanilla.blockZ;
        Block block = world.getBlock(x, y, z);
        if (!GtOverlayCompat.isGtTarget(world, x, y, z, block)) {
            return null;
        }

        Minecraft mc = McRef.get();
        Vec3 eye = Vec3.createVectorHelper(player.posX,
                player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLook(partialTicks);
        double reach = mc.playerController == null
                ? 5.0D : mc.playerController.getBlockReachDistance();
        Vec3 end = eye.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        MovingObjectPosition best = null;
        double bestDistance = Double.MAX_VALUE;

        // 覆盖板优先：它们比方块本体的碰撞盒更贴近实际瞄准位置。
        for (int side = 0; side < 6; side++) {
            AxisAlignedBB cover = GtCoverShapeProvider.getCoverBox(world, x, y, z, side);
            if (cover == null) {
                continue;
            }
            Candidate candidate = intersect(eye, end, cover, x, y, z, side, vanilla.sideHit);
            if (candidate != null && candidate.distance < bestDistance) {
                bestDistance = candidate.distance;
                best = candidate.toMop();
            }
        }

        AxisAlignedBB primary = SelectionBoxRenderer.getSelectedBox(world, block, x, y, z);
        if (primary != null) {
            List<OutlineShape> shapes = ShapeCollector.collect(world, block, x, y, z, primary, vanilla);
            for (OutlineShape shape : shapes) {
                if (shape.getKind() == OutlineShape.Kind.BOX) {
                    Candidate candidate = intersect(eye, end, shape.getBox(), x, y, z, -1, vanilla.sideHit);
                    if (candidate != null && candidate.distance < bestDistance) {
                        bestDistance = candidate.distance;
                        best = candidate.toMop();
                    }
                } else if (shape.getKind() == OutlineShape.Kind.UNION) {
                    for (AxisAlignedBB box : shape.getBoxes()) {
                        Candidate candidate = intersect(eye, end, box, x, y, z, -1, vanilla.sideHit);
                        if (candidate != null && candidate.distance < bestDistance) {
                            bestDistance = candidate.distance;
                            best = candidate.toMop();
                        }
                    }
                }
            }
        }

        // 坐标始终是原方块；没有求交结果时保留原命中。
        return best;
    }

    private static Candidate intersect(Vec3 eye, Vec3 end, AxisAlignedBB box,
                                       int x, int y, int z, int coverSide, int vanillaSide) {
        MovingObjectPosition hit = box.calculateIntercept(eye, end);
        if (hit == null || hit.hitVec == null) {
            return null;
        }
        // 覆盖板用它自己的面；其它形状沿用原版射线的面。
        // 以前用“命中点到哪个面最近”重新猜面，贴边一像素时会把侧面误判成顶面。
        int side;
        if (coverSide >= 0) {
            side = coverSide;
        } else if (vanillaSide >= 0 && vanillaSide < 6) {
            side = vanillaSide;
        } else {
            side = sideForHit(box, hit.hitVec);
        }
        return new Candidate(x, y, z, side, hit.hitVec, eye.distanceTo(hit.hitVec));
    }

    private static int sideForHit(AxisAlignedBB box, Vec3 hit) {
        double[] distances = {
                Math.abs(hit.xCoord - box.minX),
                Math.abs(hit.xCoord - box.maxX),
                Math.abs(hit.yCoord - box.minY),
                Math.abs(hit.yCoord - box.maxY),
                Math.abs(hit.zCoord - box.minZ),
                Math.abs(hit.zCoord - box.maxZ)
        };
        int[] sides = {4, 5, 0, 1, 2, 3};
        int best = 0;
        for (int i = 1; i < distances.length; i++) {
            if (distances[i] < distances[best]) {
                best = i;
            }
        }
        return sides[best];
    }

    private static final class Candidate {
        private final int x;
        private final int y;
        private final int z;
        private final int side;
        private final Vec3 hitVec;
        private final double distance;

        private Candidate(int x, int y, int z, int side, Vec3 hitVec, double distance) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.side = side;
            this.hitVec = hitVec;
            this.distance = distance;
        }

        private MovingObjectPosition toMop() {
            return new MovingObjectPosition(x, y, z, side, hitVec);
        }
    }
}
