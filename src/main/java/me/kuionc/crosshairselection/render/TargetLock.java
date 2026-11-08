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

    /** 普通情况下新目标需要稳定这么久才切换（约 3 帧）。 */
    private static final long NORMAL_CONFIRM_MILLIS = 0L;
    /** 检测到“来回横跳”后，需要稳定这么久才允许切换。 */
    private static final long FLAP_CONFIRM_MILLIS = 50L;
    /** 同一方块内换命中面需要的稳定时间；比方块切换稍长。 */
    private static final long SIDE_CONFIRM_MILLIS = 50L;
    /** 判定“来回横跳”的时间窗口：窗口内切换过至少两次。 */
    private static final long FLAP_WINDOW_MILLIS = 250L;
    /** 最近几次真正切换的时间戳。 */
    private static final long[] RECENT_SWITCHES = new long[4];
    /** 判定命中点是否落在共享面上的误差（格）。 */
    private static final double PLANE_EPSILON = 0.01D;

    private static World lockedWorld;
    private static MovingObjectPosition current;
    private static MovingObjectPosition candidate;
    private static long candidateSince;
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
        lastCallMillis = now;

        MovingObjectPosition result = resolve(hit, now);
        lastInput = hit;
        lastOutput = result;
        return result;
    }

    private static MovingObjectPosition resolve(MovingObjectPosition hit, long now) {
        boolean flapping = isFlapping(now);
        long required = flapping ? FLAP_CONFIRM_MILLIS : NORMAL_CONFIRM_MILLIS;
        String reason;
        if (current == null) {
            current = hit;
            candidate = null;
            candidateSince = 0L;
            reason = "ACQUIRE";
        } else if (sameBlock(hit, current)) {
            // 同一方块内，如果命中面在贴边处反复跳，也要求新面稳定一段时间。
            // 覆盖板选择用的正是 sideHit，这一层能挡住“同一块管道上两个面来回切”。
            reason = "SAME";
            if (hit.sideHit != current.sideHit) {
                if (candidate == null || !sameBlock(candidate, hit)
                        || candidate.sideHit != hit.sideHit) {
                    candidate = hit;
                    candidateSince = now;
                }
                long sideRequired = Math.max(required, SIDE_CONFIRM_MILLIS);
                if (now - candidateSince < sideRequired) {
                    reason = flapping ? "FLAP_HOLD_SIDE" : "KEEP_SIDE";
                } else {
                    recordSwitch(now);
                    reason = "SWITCH_SIDE";
                }
            }
            if (!reason.startsWith("KEEP") && !reason.startsWith("FLAP")) {
                current = hit;
                candidate = null;
                candidateSince = 0L;
            }
        } else if (ambiguityAtSharedFace(current, hit)) {
            // 射线正好穿过相邻方块共享的面：平局，保持当前方块。
            candidate = null;
            candidateSince = 0L;
            reason = "SHARED_FACE";
        } else {
            if (candidate == null || !sameBlock(hit, candidate)) {
                candidate = hit;
                candidateSince = now;
            }
            if (now - candidateSince >= required) {
                recordSwitch(now);
                current = hit;
                candidate = null;
                candidateSince = 0L;
                reason = "SWITCH";
            } else {
                reason = flapping ? "FLAP_HOLD" : "KEEP_CONFIRM";
            }
        }
        logDecision(hit, reason, flapping, required, now);
        return current;
    }

    /** 只在状态签名变化时写一行，避免按帧刷屏。 */
    private static String lastSignature;

    private static void logDecision(MovingObjectPosition hit, String reason, boolean flapping,
                                    long required, long now) {
        if (!SelectionDebugLog.enabled()) {
            return;
        }
        String signature = reason
                + "|raw=" + block(hit) + ",s=" + hit.sideHit
                + "|lock=" + block(current) + ",s=" + current.sideHit
                + "|cand=" + (candidate == null ? "none" : block(candidate) + ",s=" + candidate.sideHit)
                + "|age=" + (candidate == null ? -1 : (now - candidateSince))
                + "|flap=" + flapping
                + "|req=" + required;
        if (signature.equals(lastSignature)) {
            return;
        }
        lastSignature = signature;
        SelectionDebugLog.log("TGT", signature
                + " hit=" + (hit.hitVec == null ? "null" : format(hit.hitVec)));
    }

    private static String block(MovingObjectPosition hit) {
        return hit == null ? "null" : "(" + hit.blockX + "," + hit.blockY + "," + hit.blockZ + ")";
    }

    private static String format(Vec3 vec) {
        return String.format(java.util.Locale.ROOT, "(%.3f,%.3f,%.3f)",
                vec.xCoord, vec.yCoord, vec.zCoord);
    }

    /** 最近 400ms 内切换过至少两次 → 认为在交界处来回横跳。 */
    private static boolean isFlapping(long now) {
        int switches = 0;
        for (long stamp : RECENT_SWITCHES) {
            if (stamp > 0L && now - stamp <= FLAP_WINDOW_MILLIS) {
                switches++;
            }
        }
        return switches >= 2;
    }

    private static void recordSwitch(long now) {
        System.arraycopy(RECENT_SWITCHES, 1, RECENT_SWITCHES, 0, RECENT_SWITCHES.length - 1);
        RECENT_SWITCHES[RECENT_SWITCHES.length - 1] = now;
    }

    public static void reset() {
        current = null;
        candidate = null;
        candidateSince = 0L;
        lastInput = null;
        lastOutput = null;
        lastSignature = null;
        java.util.Arrays.fill(RECENT_SWITCHES, 0L);
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
