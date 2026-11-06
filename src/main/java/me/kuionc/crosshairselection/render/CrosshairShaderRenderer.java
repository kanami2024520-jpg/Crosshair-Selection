package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import me.kuionc.crosshairselection.config.CrosshairConfig;
import me.kuionc.crosshairselection.config.FeedbackStyle;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import me.kuionc.crosshairselection.util.McRef;

/**
 * 基于片段着色器的准星。
 *
 * <p>准星由 SDF 在物理像素空间求值，任意旋转角与亚像素线宽都有精确覆盖率，
 * 几何只是一个覆盖全屏的四边形。
 *
 * <p>注意事项：
 * <ul>
 *   <li>本类<b>只做渲染</b>：不读取玩家状态，也不判断是否在拉弓。
 *       所有动态状态由 {@link CrosshairRenderer} 算成 0..1 后显式传入，
 *       这样 HUD 与设置预览用同一个出口，却各自决定是否叠加实时状态。</li>
 *   <li>所有距离都以物理像素为单位，GUI 缩放已在 {@link CrosshairRenderer} 乘好。</li>
 *   <li>反馈样式共用一个 0..1 的连续状态（{@link CrosshairFeedback#level()}），
 *       点击只改变目标而不是重置时间轴。</li>
 * </ul>
 */
public final class CrosshairShaderRenderer {

    private static final String VERTEX = "shaders/crosshair.vert";
    private static final String FRAGMENT = "shaders/crosshair.frag";

    /** 拉弓时间隙的最大增加值（GUI 像素）；只改间隙，不动臂长。 */
    public static final float BOW_MAX_OPEN_GUI_PIXELS = 10.0F;

    private static ShaderProgram program;
    private static boolean failed;

    private static int uCentre;
    private static int uHalf;
    private static int uGap;
    private static int uArmX;
    private static int uArmY;
    private static int uOutline;
    private static int uRotation;
    private static int uColour;
    private static int uOutlineColour;
    private static int uDotColour;
    private static int uDot;
    private static int uScreen;
    private static int uPunch;
    private static int uPlacePulse;
    private static int uGapBoost;
    private static int uWaveScale;
    private static int uBowOpen;

    private CrosshairShaderRenderer() {
    }

    /** @return false when the shader is unavailable and the caller should fall back. */
    public static boolean isAvailable() {
        if (program != null) {
            return true;
        }
        if (failed) {
            return false;
        }
        program = ShaderSupport.load(VERTEX, FRAGMENT);
        if (program == null) {
            failed = true;
            return false;
        }
        try {
            // 全部用 lenient：着色器里暂时用不到的 uniform 会被驱动优化掉，
            // getUniformLocation 对它们会抛异常。以前 uScreen 是硬查的，
            // 一次改写把它变成了未使用 uniform，整个准星就再也画不出来了。
            uCentre = lenient("uCentre");
            uHalf = lenient("uHalf");
            uGap = lenient("uGap");
            uArmX = lenient("uArmX");
            uArmY = lenient("uArmY");
            uOutline = lenient("uOutline");
            uRotation = lenient("uRotation");
            uColour = lenient("uColour");
            uOutlineColour = lenient("uOutlineColour");
            uDotColour = lenient("uDotColour");
            uDot = lenient("uDot");
            uScreen = lenient("uScreen");
            uPunch = lenient("uPunch");
            uPlacePulse = lenient("uPlacePulse");
            uGapBoost = lenient("uGapBoost");
            uWaveScale = lenient("uWaveScale");
            uBowOpen = lenient("uBowOpen");
            return true;
        } catch (Throwable ignored) {
            failed = true;
            program = null;
            return false;
        }
    }

    private static int lenient(String name) {
        try {
            return program.getUniformLocation(name);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /**
     * 纯渲染入口：形状全部由参数决定，不读取任何玩家或反馈状态。
     *
     * @param guiScale       GUI 缩放，用于把「GUI 像素」的反馈量换算成物理像素
     * @param centreX        准星中心 X（物理像素）
     * @param centreY        准星中心 Y（物理像素，从屏幕底部量起）
     * @param half           半个臂厚（物理像素）
     * @param gap            玩家设置的间隙（物理像素）
     * @param bowOpen01      拉弓张开程度，0 = 闭合（按设置的间隙），1 = 最大张开
     * @param feedbackLevel  点击反馈状态，0 = 静止
     */
    public static void draw(int guiScale, float centreX, float centreY, float half, float gap,
                            float armX, float armY, float outline, float rotationDegrees,
                            int colour, int outlineColour, int dotColour, boolean dot,
                            boolean flow, float flowSpeed, float progress,
                            float bowOpen01, float feedbackLevel) {
        if (!isAvailable()) {
            return;
        }
        Minecraft minecraft = McRef.get();
        if (minecraft == null) {
            return;
        }
        int width = minecraft.displayWidth;
        int height = minecraft.displayHeight;
        if (width <= 0 || height <= 0) {
            return;
        }

        int previous = ShaderSupport.previousProgram();
        ShaderSupport.ensureQuad();

        program.use();
        GL20.glUniform2f(uCentre, centreX, height - centreY);
        GL20.glUniform1f(uHalf, half);
        GL20.glUniform1f(uGap, gap);
        GL20.glUniform1f(uArmX, armX);
        GL20.glUniform1f(uArmY, armY);
        GL20.glUniform1f(uOutline, outline);
        GL20.glUniform1f(uRotation, (float) Math.toRadians(rotationDegrees));
        setColour(uColour, colour);
        setColour(uOutlineColour, outlineColour);
        setColour(uDotColour, dotColour);
        GL20.glUniform1f(uDot, dot ? 1.0F : 0.0F);
        GL20.glUniform1f(uScreen, height);

        float strength = CrosshairConfig.feedbackStrength;
        boolean spreadStyle = CrosshairConfig.feedbackStyle == FeedbackStyle.SPREAD;
        boolean reacting = CrosshairConfig.feedbackStyle != FeedbackStyle.NONE;
        float level = reacting
                ? clamp01(feedbackLevel) * Math.min(1.0F, strength)
                : 0.0F;

        // The punch style was removed; the legacy uniforms stay at rest so old shaders still link.
        if (uPunch >= 0) {
            GL20.glUniform1f(uPunch, 0.0F);
        }
        if (uPlacePulse >= 0) {
            GL20.glUniform1f(uPlacePulse, 0.0F);
        }
        // Separation is bounded by MAX_EXPAND so the crosshair never leaves its neighbourhood.
        if (uGapBoost >= 0) {
            float separation = spreadStyle ? level * CrosshairFeedback.MAX_EXPAND_GUI_PIXELS : 0.0F;
            GL20.glUniform1f(uGapBoost, separation * guiScale);
        }
        // 拉弓只改间隙：0 = 完全按设置的间隙，1 = 最大张开。每帧都上传，避免上一帧的值残留。
        if (uBowOpen >= 0) {
            GL20.glUniform1f(uBowOpen, clamp01(bowOpen01) * BOW_MAX_OPEN_GUI_PIXELS * guiScale);
        }
        // 反馈样式（弓式/波纹）仍用 uWaveScale 做整体缩放，与拉弓互不影响。
        if (uWaveScale >= 0) {
            GL20.glUniform1f(uWaveScale, 1.0F);
        }
        ShaderSupport.uploadFlow(program, CrosshairConfig.flow, System.currentTimeMillis(), progress);

        RenderBackend.beginGuiOverlay();
        // 不再使用 scissor：顶点铺满全屏，范围由片段着色器的 discard 决定，
        // 任何旋转、放大或反馈位移都不会被裁剪。
        ShaderSupport.drawQuad();
        RenderBackend.endGuiOverlay();
        ShaderSupport.restoreProgram(previous);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static void setColour(int location, int argb) {
        GL20.glUniform4f(location,
                ColorUtil.red(argb) / 255.0F,
                ColorUtil.green(argb) / 255.0F,
                ColorUtil.blue(argb) / 255.0F,
                ColorUtil.alpha(argb) / 255.0F);
    }

    /** Whether the current framebuffer can resolve multisampling at all. */
    public static boolean framebufferHasMsaa() {
        try {
            return GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_SAMPLE_BUFFERS) > 0
                    && GL11.glGetInteger(org.lwjgl.opengl.GL13.GL_SAMPLES) > 1;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
