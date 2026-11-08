package me.kuionc.crosshairselection.render;

import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * 目标方块的防抖动锁。
 *
 * <p>准星停在两个方块/线缆的交界处时，{@code objectMouseOver} 会逐帧在 A、B 之间来回跳，
 * 选框和 GT 九格就会跟着快速闪烁。这里做「翻转型滞回」：
 *
 * <ul>
 *   <li>正常切换到新目标立即生效，不增加延迟感；</li>
 *   <li>只有「跳回上一个目标」才要求它连续稳定 {@link #FLAP_LOCK_MILLIS} 毫秒，
 *       从而吃掉 A↔B 的逐帧抖动；</li>
 *   <li>同一方块内部继续使用最新的命中数据，AE/EnderIO/FMP 的部件精度不受影响。</li>
 * </ul>
 *
 * <p>直接保存 {@link MovingObjectPosition} 引用：1.7.10 每帧都会新建一个，
 * 不会原地修改；拷贝成普通 MOP 会丢掉 {@code ExtendedMOP} 的 part 数据。
 */
public final class TargetLock {

    /** 跳回上一个目标需要稳定这么久才算真的移回去。 */
    private static final long FLAP_LOCK_MILLIS = 120L;

    private static World lockedWorld;
    private static MovingObjectPosition current;
    private static int previousX;
    private static int previousY;
    private static int previousZ;
    private static boolean hasPrevious;
    private static MovingObjectPosition candidate;
    private static long candidateSince;

    private TargetLock() {
    }

    /** 返回本帧应该显示的目标；可能仍是上一帧锁定的那个方块。 */
    public static MovingObjectPosition stable(World world, MovingObjectPosition hit) {
        if (lockedWorld != world) {
            reset();
            lockedWorld = world;
        }
        if (hit == null) {
            reset();
            return null;
        }
        long now = System.currentTimeMillis();
        if (current == null) {
            current = hit;
            hasPrevious = false;
            candidate = null;
            return current;
        }
        if (sameBlock(hit, current)) {
            // 同一方块：用最新的命中点，保证部件级选择仍然精确。
            current = hit;
            candidate = null;
            return current;
        }
        if (hasPrevious && hit.blockX == previousX && hit.blockY == previousY
                && hit.blockZ == previousZ) {
            // 这是「跳回上一个目标」，需要它稳定一段时间。
            if (candidate == null || !sameBlock(hit, candidate)) {
                candidate = hit;
                candidateSince = now;
            }
            if (now - candidateSince < FLAP_LOCK_MILLIS) {
                return current;
            }
        }
        previousX = current.blockX;
        previousY = current.blockY;
        previousZ = current.blockZ;
        hasPrevious = true;
        current = hit;
        candidate = null;
        return current;
    }

    public static void reset() {
        current = null;
        candidate = null;
        hasPrevious = false;
        candidateSince = 0L;
        previousX = 0;
        previousY = 0;
        previousZ = 0;
    }

    private static boolean sameBlock(MovingObjectPosition hit, MovingObjectPosition reference) {
        return hit.blockX == reference.blockX
                && hit.blockY == reference.blockY
                && hit.blockZ == reference.blockZ;
    }
}
