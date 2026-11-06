package me.kuionc.crosshairselection.render;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.kuionc.crosshairselection.compat.TicCrosshairCompat;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import me.kuionc.crosshairselection.util.McRef;

/**
 * 准星的<b>唯一出口</b>：可见性判断、状态采集与几何参数都在这里完成。
 *
 * <p>两个入口都走同一个私有实现 {@link #draw(int, int, boolean)}：
 * <ul>
 *   <li>{@link #draw(int, int)}：HUD，叠加实时状态（拉弓张开、点击反馈）。</li>
 *   <li>{@link #drawPreview(int, int)}：设置预览，动态状态固定为静止，
 *       只反映设置本身。之前预览和 HUD 共用一份实时状态，
 *       手上拿着弓时预览会一直带一圈间隙，与「间隙设为 0」对不上。</li>
 * </ul>
 *
 * <p>注意事项：
 * <ul>
 *   <li>拉弓状态只影响准星的形状，<b>绝不参与「是否绘制」的判断</b>，
 *       否则弓的蓄力状态切换瞬间会产生闪烁或整帧消失。</li>
 *   <li>{@code DEFAULT} 类型仍然走原版图标绘制，与弓无关。</li>
 *   <li>可见性开关（F1 / F3 / 第三人称）在此统一判断，Mixin 侧只负责隐藏原版准星。</li>
 * </ul>
 */
public class CrosshairRenderer {

    private static final ResourceLocation VANILLA_ICONS = new ResourceLocation("textures/gui/icons.png");

    /**
     * 每次都从静态单例取，而不是缓存到字段。
     *
     * <p>渲染器可能在代理构造阶段就被创建，此时 {@code McRef.get()} 还不能调用；
     * 延迟到真正绘制时取值，避免 NoSuchMethodError / &lt;clinit&gt; 顺序问题。
     */
    private static Minecraft minecraft() {
        return McRef.get();
    }

    private int guiScale = 1;
    private float originX;
    private float originY;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = minecraft();
        if (minecraft == null || minecraft.theWorld == null || !minecraft.inGameHasFocus) {
            return;
        }
        if (!shouldRender()) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(minecraft,
                minecraft.displayWidth, minecraft.displayHeight);
        if (minecraft.gameSettings.hideGUI) {
            setupHiddenGuiOverlay(resolution);
        }
        draw(resolution.getScaledWidth() / 2, resolution.getScaledHeight() / 2);
    }

    public static boolean shouldRender() {
        if (!CrosshairConfig.enabled || !CrosshairConfig.visibleDefault) {
            return false;
        }
        Minecraft minecraft = minecraft();
        if (minecraft == null) {
            return false;
        }
        if (minecraft.gameSettings.thirdPersonView > 0 && !CrosshairConfig.visibleThirdPerson) {
            return false;
        }
        if (minecraft.gameSettings.hideGUI && !CrosshairConfig.visibleHiddenGui) {
            return false;
        }
        // The bow state never decides whether the crosshair is drawn; it only shapes it. Gating on
        // it made the crosshair vanish for a frame while the draw state settled.
        if (TicCrosshairCompat.shouldUseTicCrosshair(minecraft.thePlayer)) {
            return false;
        }
        return !minecraft.gameSettings.showDebugInfo || CrosshairConfig.visibleDebug;
    }

    /**
     * 在给定 GUI 坐标绘制准星，HUD 使用。实时状态（拉弓张开、点击反馈）会叠加到形状上。
     */
    public void draw(int drawX, int drawY) {
        draw(drawX, drawY, true);
    }

    /**
     * 在给定 GUI 坐标绘制准星预览，设置界面使用。
     *
     * <p>动态状态固定为静止，所以预览严格等于设置本身：间隙设成 0 就是 0，
     * 不会因为手上拿着弓或刚点过左键而出现额外的间隙。
     */
    public void drawPreview(int drawX, int drawY) {
        draw(drawX, drawY, false);
    }

    /**
     * 唯一实现。{@code live} 决定是否采集实时状态；几何、颜色与缩放换算只有这一份。
     */
    private void draw(int drawX, int drawY, boolean live) {
        Minecraft minecraft = minecraft();
        if (minecraft == null) {
            return;
        }
        ScaledResolution resolution = new ScaledResolution(minecraft,
                minecraft.displayWidth, minecraft.displayHeight);
        this.guiScale = Math.max(1, resolution.getScaleFactor());
        // 着色器用全屏四边形，中心必须按物理像素传。
        this.originX = drawX;
        this.originY = drawY;
        if (CrosshairConfig.type == CrosshairType.DEFAULT) {
            drawVanillaLayer(drawX, drawY);
            return;
        }
        drawWithShader(live ? collectLiveState() : LiveState.REST);
    }

    /** 形状上叠加的动态状态；预览用静态常量的 {@link #REST}。 */
    private static final class LiveState {
        static final LiveState REST = new LiveState(0.0F, 0.0F);

        final float bowOpen01;
        final float feedbackLevel;

        LiveState(float bowOpen01, float feedbackLevel) {
            this.bowOpen01 = bowOpen01;
            this.feedbackLevel = feedbackLevel;
        }
    }

    /**
     * 采集实时状态：拉弓张开程度与点击反馈。
     *
     * <p>张开程度 = 1 - 蓄力进度。{@link TicCrosshairCompat#progress} 在拉满时恰好返回 1，
     * 所以拉满时张开量严格为 0，间隙回到设置值，不会残留一圈空隙。
     */
    private static LiveState collectLiveState() {
        float feedback = CrosshairFeedback.level();
        if (!CrosshairConfig.bowChargeFeedback) {
            return new LiveState(0.0F, feedback);
        }
        Minecraft minecraft = minecraft();
        EntityPlayer player = minecraft == null ? null : minecraft.thePlayer;
        if (!TicCrosshairCompat.isDrawingBow(player)) {
            return new LiveState(0.0F, feedback);
        }
        return new LiveState(1.0F - TicCrosshairCompat.progress(player), feedback);
    }

    /**
     * Draws the crosshair with the fragment shader, which evaluates exact coverage per physical
     * pixel. GTNH always provides GTNHLib and a GL 4.6 compatible context, so no fallback path
     * exists.
     */
    private void drawWithShader(LiveState state) {
        float scale = this.guiScale;
        float halfWidth = Math.max(CrosshairConfig.thickness / 4.0F, 0.05F) * scale / 2.0F;
        CrosshairShaderRenderer.draw(
                this.guiScale, this.originX * scale, this.originY * scale, halfWidth,
                CrosshairConfig.gap * scale,
                CrosshairConfig.width * scale, CrosshairConfig.height * scale,
                CrosshairConfig.outline && CrosshairConfig.outlineThickness > 0.01F
                        ? CrosshairConfig.outlineThickness * scale : 0.0F,
                CrosshairConfig.rotation,
                CrosshairConfig.colour,
                CrosshairConfig.outlineColour,
                CrosshairConfig.dot ? CrosshairConfig.dotColour : 0,
                CrosshairConfig.dot,
                CrosshairConfig.flow.mode != CrosshairConfig.flow.mode.OFF,
                CrosshairConfig.flow.speed,
                CrosshairConfig.flow.progress,
                state.bowOpen01,
                state.feedbackLevel);
    }

    private void drawVanillaLayer(int drawX, int drawY) {
        RenderBackend.beginGuiOverlay();
        drawVanilla(drawX, drawY);
        RenderBackend.endGuiOverlay();
    }

    private void drawVanilla(int drawX, int drawY) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_ONE_MINUS_DST_COLOR, GL11.GL_ONE_MINUS_SRC_COLOR);
        minecraft().getTextureManager().bindTexture(VANILLA_ICONS);
        minecraft().ingameGUI.drawTexturedModalRect(drawX - 7, drawY - 7, 0, 0, 16, 16);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    private static void setupHiddenGuiOverlay(ScaledResolution resolution) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, resolution.getScaledWidth_double(),
                resolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
        GL11.glTranslatef(0.0F, 0.0F, -2000.0F);
    }
}
