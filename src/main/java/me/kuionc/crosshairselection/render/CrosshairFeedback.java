package me.kuionc.crosshairselection.render;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Drives the crosshair reaction as a single continuous value.
 *
 * <p>{@link #level} runs from 0 (resting) to 1 (fully expanded) and is moved towards a target at
 * the configured speeds instead of restarting a one-shot animation per click. Holding the button
 * keeps the target at 1, so repeated clicks continue along the current path rather than snapping
 * back to the start; on release the target drops to 0 and the value eases home.
 */
public final class CrosshairFeedback {

    /** Repeated events inside this window are treated as one (client and server both fire). */
    private static final long DEDUPE_MILLIS = 10L;
    /**
     * 点击后目标值维持为 1 的时长。
     *
     * <p>只决定「什么时候开始回落」，具体节奏由扩张/归位速度控制，
     * 因此连点不会重置动画，只是不断刷新这段维持期。
     */
    private static final long HOLD_MILLIS = 140L;
    /**
     * 追赶系数。决定「点击弹出」与「回落」的干脆程度：
     * 太小会拖沓，太大则一碰就到位、看不出动画。
     */
    private static final float CHASE_FACTOR = 14.0F;
    /** Upper bound of the expansion, in GUI pixels, so the crosshair can never fly off screen. */
    public static final float MAX_EXPAND_GUI_PIXELS = 6.0F;

    private static long attackAt;
    private static long placeAt;
    private static Object world;

    private static float level;
    private static long lastUpdate;
    private static boolean previousSwing;
    private static boolean previousLeftDown;

    public CrosshairFeedback() {
    }

    public static void notifyAttack() {
        attackAt = stamp(attackAt);
    }

    public static void notifyPlace() {
        placeAt = stamp(placeAt);
    }

    private static long stamp(long previous) {
        long now = System.currentTimeMillis();
        return now - previous >= DEDUPE_MILLIS || now < previous ? now : previous;
    }

    /** Forge fires this for both the client and, in single player, the integrated server. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (isLocalPlayer(event.entityPlayer)) {
            notifyPlace();
        }
    }

    /**
     * 推进放在渲染帧而不是客户端 tick：tick 每秒只有 20 次，连续点击时状态更新会明显滞后，
     * 看起来像动画卡住。
     */
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = McRef.get();
        if (minecraft == null) {
            return;
        }
        EntityPlayer player = minecraft.thePlayer;
        if (player == null || minecraft.theWorld == null) {
            level = 0.0F;
            return;
        }
        if (world != minecraft.theWorld) {
            world = minecraft.theWorld;
            attackAt = 0L;
            placeAt = 0L;
            level = 0.0F;
        }
        // 点击检测用鼠标按键的上升沿：
        // clickBlock 只在点到方块时触发，点空气/点生物完全没有反馈。
        // 这里直接看左键状态，任何一次按下都算一次点击。
        boolean leftDown = Mouse.isButtonDown(0);
        if (leftDown && !previousLeftDown && minecraft.currentScreen == null) {
            notifyAttack();
        }
        previousLeftDown = leftDown;

        // 挥击状态作为补充：某些攻击不经过按键（脚本、连发），仍然给出反馈。
        if (player.isSwingInProgress && !previousSwing) {
            notifyAttack();
        }
        previousSwing = player.isSwingInProgress;

        update(minecraft);
    }

    /**
     * 每帧推进一次状态。
     *
     * <p>核心是「目标值 + 追赶」：点击只把目标抬到 1，之后不论连点多少次，
     * 当前值都只是朝目标平滑逼近，不会重置、不会跳变，因此动画永远跟手。
     * 松手后目标回落到 0，同样以设定速度收回。
     */
    private static void update(Minecraft minecraft) {
        long now = System.currentTimeMillis();
        float delta = lastUpdate == 0L ? 0.016F : Math.max(0.0F, (now - lastUpdate) / 1000.0F);
        lastUpdate = now;

        long lastClick = Math.max(attackAt, placeAt);
        // 点击后的一小段时间内目标保持为 1，超过就回落；不再有「播完整段动画」的概念。
        boolean active = lastClick > 0L && now - lastClick < HOLD_MILLIS;

        // 这里只管点击反馈。拉弓的张开由准星自己按蓄力进度计算，
        // 不经过 level；否则满弓时 level 停在 1，会给准星留一圈额外的间隙。
        float target = active ? 1.0F : 0.0F;

        float expand = Math.max(0.1F, CrosshairConfig.expandSpeed);
        float release = Math.max(0.1F, CrosshairConfig.returnSpeed);
        // 目标变大用扩张速度，变小用归位速度。
        float speed = target > level ? expand : release;
        float k = (float) (1.0D - Math.exp(-speed * CHASE_FACTOR * delta));
        level += (target - level) * Math.min(1.0F, k);
        level = Math.max(0.0F, Math.min(1.0F, level));
    }

    /** Current expansion, 0 at rest and 1 fully expanded. Continuous across clicks. */
    public static float level() {
        return level;
    }

    /**
     * Bow-style separation. Kept as a function of the shared level so holding a button and
     * repeated clicks behave the same as the other styles.
     */
    public static float spread() {
        return level;
    }

    /** Wave style shares the continuous level as well. */
    public static float wave() {
        return level;
    }

    /** 0..1 attack kick, kept for the punch style's outer ring. */
    public static float punch() {
        return level;
    }

    /** 0..1 placement pulse, kept for the punch style's ripple. */
    public static float placePulse() {
        return level;
    }

    private static boolean isLocalPlayer(EntityPlayer player) {
        Minecraft minecraft = McRef.get();
        return minecraft != null && player != null && player == minecraft.thePlayer;
    }

    /** Explicit trigger, used by the settings screens so the feedback can be previewed. */
    public static void triggerAttack() {
        notifyAttack();
    }

    public static void triggerPlace() {
        notifyPlace();
    }
}
