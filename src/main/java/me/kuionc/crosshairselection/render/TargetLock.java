package me.kuionc.crosshairselection.render;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 目标方块的稳定性判断。
 *
 * <p>准星停在两根管道/线缆的交接处时，射线正好穿过两个方块共享的那个面，
 * {@code objectMouseOver} 会逐帧在两者之间跳，选框就会快速左右晃。这里用两条规则解决：
 *
 * <ol>
 *   <li><b>几何决胜</b>：新命中的方块与当前方块相邻，且命中点落在它们共享的面上时，
 *       说明射线是从交界处穿过去的，属于平局，保持当前方块不变。</li>
 *   <li><b>两帧确认</b>：其余情况新目标需要连续两帧被命中才切换；
 *       60fps 下约 33ms，肉眼几乎无感，但足以吃掉逐帧抖动。</li>
 * </ol>
 *
 * <p>同一方块内部始终采用最新的命中数据，AE / EnderIO / FMP 的部件级精度不受影响。
 * 直接保存 {@link MovingObjectPosition} 引用：1.7.10 每帧都会新建一个，
 * 拷贝成普通 MOP 会丢掉 {@code ExtendedMOP} 的 part 数据。
 */
public final class TargetLock {

    /** 新目标需要连续命中的帧数（按渲染帧计，不是按调用次数）。 */
    private static final int CONFIRM_FRAMES = 3;
    /** 判定命中点是否落在共享面上的误差（格）。 */
    private static final double PLANE_EPSILON = 0.01D;

    private static World lockedWorld;
    private static MovingObjectPosition current;
    private static MovingObjectPosition candidate;
    private static int candidateFrames;
    /** 同一帧里 GT 九格会调用多次，这里保证确认计数每帧只推进一次。 */
    private static long lastCallMillis;
    private static MovingObjectPosition lastInput;
    private static MovingObjectPosition lastOutput;

    private TargetLock() {
    }

    /** 返回本帧应该显示的目标；可能仍是之前那个方块。 */
    public static MovingObjectPosition stable(World world, MovingObjectPosition hit) {
        if (lockedWorld != world) {
            reset();
            lockedWorld = world;
        }
        if (hit == null) {
            reset();
            return null;
        }
        if (hit == lastInput) {
            // 同一帧的重复调用（GT 的 drawGrid 会进来 3 次），直接复用上一次结果。
            return lastOutput;
        }
        long now = System.currentTimeMillis();
        boolean newFrame = now != lastCallMillis;
        lastCallMillis = now;

        MovingObjectPosition result = resolve(hit, newFrame);
        lastInput = hit;
        lastOutput = result;
        return result;
    }

    private static MovingObjectPosition resolve(MovingObjectPosition hit, boolean newFrame) {
        if (current == null) {
            current = hit;
            candidate = null;
            candidateFrames = 0;
            return current;
        }
        if (sameBlock(hit, current)) {
            // 同一方块内，如果命中面在贴边处反复跳，也要求新面连续确认。
            // 覆盖板选择用的正是 sideHit，这一层能挡住“同一块管道上两个面来回切”。
            if (hit.sideHit != current.sideHit) {
                if (candidate == null || !sameBlock(candidate, hit)
                        || candidate.sideHit != hit.sideHit) {
                    candidate = hit;
                    candidateFrames = 1;
                } else if (newFrame) {
                    candidateFrames++;
                }
                if (candidateFrames < CONFIRM_FRAMES) {
                    return current;
                }
            }
            current = hit;
            candidate = null;
            candidateFrames = 0;
            return current;
        }
        if (ambiguityAtSharedFace(current, hit)) {
            // 射线正好穿过相邻方块共享的面：平局，保持当前方块。
            candidate = null;
            candidateFrames = 0;
            return current;
        }
        if (candidate == null || !sameBlock(hit, candidate)) {
            candidate = hit;
            candidateFrames = 1;
        } else if (newFrame) {
            candidateFrames++;
        }
        if (candidateFrames >= CONFIRM_FRAMES) {
            current = hit;
            candidate = null;
            candidateFrames = 0;
        }
        return current;
    }

    public static void reset() {
        current = null;
        candidate = null;
        candidateFrames = 0;
        lastInput = null;
        lastOutput = null;
    }

    /**
     * 两个方块面相邻，且命中点落在它们共享的那个面上 → 平局。
     */
    private static boolean ambiguityAtSharedFace(MovingObjectPosition current, MovingObjectPosition hit) {
        if (current == null || hit == null || hit.hitVec == null) {
            return false;
        }
        int dx = hit.blockX - current.blockX;
        int dy = hit.blockY - current.blockY;
        int dz = hit.blockZ - current.blockZ;
        if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) != 1) {
            return false;
        }
        Vec3 point = hit.hitVec;
        if (dx == 1) {
            return Math.abs(point.xCoord - (current.blockX + 1.0D)) <= PLANE_EPSILON;
        }
        if (dx == -1) {
            return Math.abs(point.xCoord - current.blockX) <= PLANE_EPSILON;
        }
        if (dy == 1) {
            return Math.abs(point.yCoord - (current.blockY + 1.0D)) <= PLANE_EPSILON;
        }
        if (dy == -1) {
            return Math.abs(point.yCoord - current.blockY) <= PLANE_EPSILON;
        }
        if (dz == 1) {
            return Math.abs(point.zCoord - (current.blockZ + 1.0D)) <= PLANE_EPSILON;
        }
        return Math.abs(point.zCoord - current.blockZ) <= PLANE_EPSILON;
    }

    private static boolean sameBlock(MovingObjectPosition hit, MovingObjectPosition reference) {
        return hit.blockX == reference.blockX
                && hit.blockY == reference.blockY
                && hit.blockZ == reference.blockZ;
    }
}
