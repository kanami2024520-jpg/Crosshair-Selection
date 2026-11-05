package me.kuionc.crosshairselection.compat;

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

/**
 * Corrects Minecraft's block target for thin GT pipes, cables and cover plates. The vanilla
 * ray trace often reports the full-block bounds of an adjacent pipe, which makes selection and
 * wrench clicks land on the wrong block.
 */
public final class GtTargetResolver {

    private GtTargetResolver() {
    }

    public static MovingObjectPosition resolve(EntityPlayer player, float partialTicks,
                                               MovingObjectPosition vanilla) {
        if (player == null || player.worldObj == null) {
            return null;
        }
        World world = player.worldObj;
        boolean vanillaGt = vanilla != null
                && vanilla.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                && GtOverlayCompat.isGtTarget(world, vanilla.blockX, vanilla.blockY, vanilla.blockZ,
                world.getBlock(vanilla.blockX, vanilla.blockY, vanilla.blockZ));
        double vanillaDistance = vanilla != null && vanilla.hitVec != null
                ? eyeDistance(player, vanilla.hitVec) : Double.MAX_VALUE;
        if (vanilla != null) {
            if (vanilla.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                return null;
            }
            // Never replace a valid non-GT block target (for example an EnderIO conduit face)
            // with a GT machine behind it.
            if (vanilla.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !vanillaGt) {
                return null;
            }
        }
        Minecraft mc = Minecraft.getMinecraft();
        Vec3 eye = Vec3.createVectorHelper(player.posX,
                player.posY + player.getEyeHeight(), player.posZ);
        Vec3 look = player.getLook(partialTicks);
        double reach = mc.playerController == null
                ? 5.0D : mc.playerController.getBlockReachDistance();
        Vec3 end = eye.addVector(look.xCoord * reach, look.yCoord * reach, look.zCoord * reach);

        int baseX = floor(end.xCoord);
        int baseY = floor(end.yCoord);
        int baseZ = floor(end.zCoord);

        MovingObjectPosition best = null;
        double bestDistance = Double.MAX_VALUE;
        int radius = 2;
        for (int x = baseX - radius; x <= baseX + radius; x++) {
            for (int y = baseY - radius; y <= baseY + radius; y++) {
                for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                    Block block = world.getBlock(x, y, z);
                    if (block == null || !GtOverlayCompat.isGtTarget(world, x, y, z, block)) {
                        continue;
                    }
                    for (int side = 0; side < 6; side++) {
                        AxisAlignedBB cover = GtCoverShapeProvider.getCoverBox(world, x, y, z, side);
                        if (cover != null) {
                            Candidate candidate = intersect(eye, end, cover, x, y, z, side);
                            if (candidate != null && candidate.distance < bestDistance) {
                                bestDistance = candidate.distance;
                                best = candidate.toMop();
                            }
                        }
                    }

                    AxisAlignedBB primary = SelectionBoxRenderer.getSelectedBox(world, block, x, y, z);
                    if (primary != null) {
                        MovingObjectPosition synthetic = new MovingObjectPosition(x, y, z, -1,
                                Vec3.createVectorHelper(x + 0.5D, y + 0.5D, z + 0.5D));
                        List<OutlineShape> shapes = ShapeCollector.collect(
                                world, block, x, y, z, primary, synthetic);
                        for (OutlineShape shape : shapes) {
                            if (shape.getKind() == OutlineShape.Kind.BOX) {
                                Candidate candidate = intersect(eye, end, shape.getBox(), x, y, z, -1);
                                if (candidate != null && candidate.distance < bestDistance) {
                                    bestDistance = candidate.distance;
                                    best = candidate.toMop();
                                }
                            } else if (shape.getKind() == OutlineShape.Kind.UNION) {
                                for (AxisAlignedBB box : shape.getBoxes()) {
                                    Candidate candidate = intersect(eye, end, box, x, y, z, -1);
                                    if (candidate != null && candidate.distance < bestDistance) {
                                        bestDistance = candidate.distance;
                                        best = candidate.toMop();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (best == null) {
            return null;
        }
        if (!vanillaGt && vanillaDistance + 0.05D < bestDistance) {
            return null;
        }
        // A vanilla GT target may be the full-block bounds of an adjacent pipe; allow a
        // small correction, but do not jump to a much farther GT block.
        if (vanillaGt && vanillaDistance + 0.75D < bestDistance) {
            return null;
        }
        return best;
    }

    private static double eyeDistance(EntityPlayer player, Vec3 hit) {
        double dx = player.posX - hit.xCoord;
        double dy = player.posY + player.getEyeHeight() - hit.yCoord;
        double dz = player.posZ - hit.zCoord;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Candidate intersect(Vec3 eye, Vec3 end, AxisAlignedBB box,
                                       int x, int y, int z, int coverSide) {
        MovingObjectPosition hit = box.calculateIntercept(eye, end);
        if (hit == null || hit.hitVec == null) {
            return null;
        }
        int side = coverSide >= 0 ? coverSide : sideForHit(box, hit.hitVec);
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

    private static int floor(double value) {
        int result = (int) value;
        return value < result ? result - 1 : result;
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
