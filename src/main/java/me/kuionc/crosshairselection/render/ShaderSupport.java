package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingHelper;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import me.kuionc.crosshairselection.config.FlowSettings;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import me.kuionc.crosshairselection.util.McRef;

/**
 * 着色器公共设施：加载、全屏四边形、流光 uniform 上传。
 *
 * <p>注意事项：
 * <ul>
 *   <li>{@link ShaderProgram#getUniformLocation} 在 uniform 被优化掉时会抛 NPE，
 *       所以所有查询都走 {@link #location} 转成 -1，调用方按需跳过。</li>
 *   <li>{@link #ensureQuad} 负责让 GTNHLib 创建它内部的全屏四边形；
 *       准星与边框在使用自己的绘制路径前都必须调用一次。</li>
 *   <li>流光参数在上传后立即生效，因此切换界面里的滑杆无需重新加载着色器。</li>
 * </ul>
 */
public final class ShaderSupport {

    private static boolean quadCreated;

    private ShaderSupport() {
    }

    /**
     * 触发 GTNHLib 创建全屏四边形。
     *
     * <p>它内部用 {@code TessellatorManager.startCapturingDirect()} 构建，
     * 因此必须先 {@code ShaderProgram.clear()}，避免捕获期间仍绑定着上一帧的程序。
     */
    static void ensureQuad() {
        if (quadCreated) {
            return;
        }
        ShaderProgram.clear();
        PostProcessingHelper.bindFullscreenVAO();
        PostProcessingHelper.unbindVAO();
        quadCreated = true;
    }

    static void drawQuad() {
        PostProcessingHelper.bindFullscreenVAO();
        PostProcessingHelper.drawFullscreenQuad();
        PostProcessingHelper.unbindVAO();
    }

    /**
     * 加载并链接一对着色器，失败时打印一次原因。
     *
     * <p>这里不吞异常：功能整体不显示却在日志里毫无痕迹，是排查成本最高的一类故障。
     */
    static ShaderProgram load(String vertex, String fragment) {
        try {
            ShaderProgram program = new ShaderProgram("crosshairselection", vertex, fragment);
            if (program.getProgram() == 0) {
                System.err.println("[Crosshair-Selection] shader program is 0 for " + vertex);
                return null;
            }
            return program;
        } catch (Throwable t) {
            System.err.println("[Crosshair-Selection] could not load " + vertex + " / " + fragment);
            t.printStackTrace();
            return null;
        }
    }

    /**
     * 上传流光参数。相位里已经包含时间推进，着色器只负责按运动曲线做映射。
     */
    static void uploadFlow(ShaderProgram program, FlowSettings flow, long now) {
        uploadFlow(program, flow, now, flow.progress, FlowFrame.worldUnit(flow));
    }

    static void uploadFlow(ShaderProgram program, FlowSettings flow, long now, float progress) {
        uploadFlow(program, flow, now, progress, FlowFrame.worldUnit(flow));
    }

    static void uploadFlow(ShaderProgram program, FlowSettings flow, long now, float progress,
                           FlowFrame frame) {
        int mode;
        switch (flow.mode) {
            case FLOW:
                mode = 1;
                break;
            case SWEEP:
                mode = 2;
                break;
            case OFF:
            default:
                mode = 0;
                break;
        }
        // Not every shader declares every flow uniform, so locations are resolved leniently.
        uniform(program, "uFlowMode", mode);
        // uFlowColour is only declared by shaders that draw the gradient; guard its location too.
        int colourLocation = location(program, "uFlowColour");
        if (colourLocation >= 0) {
            GL20.glUniform4f(colourLocation,
                    flow.hueStart, flow.hueRange, flow.saturation, flow.brightness);
        }
        uniform(program, "uFlowPhase", phase(flow, now));
        uniform(program, "uFlowMotion", flow.motion.ordinal());
        uniform(program, "uFlowDensity", Math.max(flow.bandDensity, 0.01F));
        uniform(program, "uFlowSoftness", Math.max(flow.sweepSoftness, 0.001F));
        uniform(program, "uFlowProgress", progress);
        uniform(program, "uFlowOpacity", Math.max(0.0F, Math.min(1.0F, flow.opacity)));

        // Sweep axis and direction depend on the configured travel mode.
        float[] axis = sweepAxis(flow);
        int axisLocation = location(program, "uSweepAxis");
        if (axisLocation >= 0) {
            GL20.glUniform3f(axisLocation, axis[0], axis[1], axis[2]);
        }
        uniform(program, "uSweepCentreOut", flow.direction == FlowSettings.Direction.CENTER_OUT ? 1.0F : 0.0F);

        // 流光/扫光的坐标系：方向轴、原点与投影范围。
        // 有了它们「角到角」才能真正沿选中的两个角走，扫光进度也才能和形状范围对齐。
        if (frame != null) {
            int originLocation = location(program, "uFlowOrigin");
            if (originLocation >= 0) {
                GL20.glUniform3f(originLocation, frame.originX, frame.originY, frame.originZ);
            }
            int flowAxisLocation = location(program, "uFlowAxis");
            if (flowAxisLocation >= 0) {
                GL20.glUniform3f(flowAxisLocation, frame.axisX, frame.axisY, frame.axisZ);
            }
            uniform(program, "uSweepMin", frame.min);
            uniform(program, "uSweepMax", frame.max);
            uniform(program, "uSweepRadius", frame.radius);
        }

    }

    /**
     * 流光使用的坐标系。
     *
     * <p>{@code origin}、{@code min}/{@code max} 都用与着色器相同的空间：
     * 选框/填充是世界坐标，准星是屏幕像素。
     */
    static final class FlowFrame {

        final float originX;
        final float originY;
        final float originZ;
        final float axisX;
        final float axisY;
        final float axisZ;
        final float min;
        final float max;
        final float radius;

        private FlowFrame(float originX, float originY, float originZ,
                          float axisX, float axisY, float axisZ,
                          float min, float max, float radius) {
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.axisX = axisX;
            this.axisY = axisY;
            this.axisZ = axisZ;
            this.min = min;
            this.max = max;
            this.radius = radius;
        }

        /** 没有几何信息时的单位坐标系（只有方向有意义）。 */
        static FlowFrame worldUnit(FlowSettings flow) {
            float[] axis = axisFor(flow);
            return new FlowFrame(0.0F, 0.0F, 0.0F, axis[0], axis[1], axis[2], -1.0F, 1.0F, 1.0F);
        }

        /** 世界坐标的包围盒 → 流光坐标系。 */
        static FlowFrame world(FlowSettings flow, double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ) {
            float originX = (float) ((minX + maxX) * 0.5D);
            float originY = (float) ((minY + maxY) * 0.5D);
            float originZ = (float) ((minZ + maxZ) * 0.5D);
            float radius = (float) (Math.sqrt(square(maxX - minX) + square(maxY - minY)
                    + square(maxZ - minZ)) * 0.5D);
            return box(flow, originX, originY, originZ, radius,
                    (float) (maxX - minX), (float) (maxY - minY), (float) (maxZ - minZ));
        }

        /** 屏幕坐标的坐标系：origin 是准星中心，radius 是准星的覆盖半径。 */
        static FlowFrame screen(FlowSettings flow, float centreX, float centreY, float radius) {
            boolean centreOut = flow.direction == FlowSettings.Direction.CENTER_OUT;
            float axisX = centreOut ? 0.0F : 0.70710677F;
            float axisY = centreOut ? 0.0F : 0.70710677F;
            return new FlowFrame(centreX, centreY, 0.0F, axisX, axisY, 0.0F,
                    -radius, radius, radius);
        }

        private static FlowFrame box(FlowSettings flow, float originX, float originY, float originZ,
                                     float radius, float sizeX, float sizeY, float sizeZ) {
            float[] axis = axisFor(flow);
            boolean centreOut = flow.direction == FlowSettings.Direction.CENTER_OUT;
            if (centreOut) {
                return new FlowFrame(originX, originY, originZ, 0.0F, 0.0F, 0.0F,
                        0.0F, Math.max(radius, 0.001F), Math.max(radius, 0.001F));
            }
            // 八个角在轴上的投影范围。
            float halfX = sizeX * 0.5F;
            float halfY = sizeY * 0.5F;
            float halfZ = sizeZ * 0.5F;
            float extent = Math.abs(axis[0]) * halfX + Math.abs(axis[1]) * halfY
                    + Math.abs(axis[2]) * halfZ;
            return new FlowFrame(originX, originY, originZ, axis[0], axis[1], axis[2],
                    -extent, extent, Math.max(radius, 0.001F));
        }

        /** 方向轴；REVERSE 由相位符号处理，这里始终返回正向单位向量。 */
        static float[] axisFor(FlowSettings flow) {
            if (flow.direction == FlowSettings.Direction.CORNER_TO_CORNER) {
                float[] start = cornerOffset(flow.startCorner);
                float[] end = cornerOffset(flow.endCorner);
                float dx = end[0] - start[0];
                float dy = end[1] - start[1];
                float dz = end[2] - start[2];
                float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (length > 1.0E-4F) {
                    return new float[]{dx / length, dy / length, dz / length};
                }
            }
            float inv = 0.57735027F;
            return new float[]{inv, inv, inv};
        }

        private static double square(double value) {
            return value * value;
        }
    }

    /**
     * 世界空间中的扫光方向。
     *
     * <p>角到角取起点角到终点角的方向；中心向外在着色器里另行处理；其余沿 +Y 推进。
     */
    static float[] sweepAxis(FlowSettings flow) {
        if (flow.direction == FlowSettings.Direction.CORNER_TO_CORNER) {
            float[] start = cornerOffset(flow.startCorner);
            float[] end = cornerOffset(flow.endCorner);
            float dx = end[0] - start[0];
            float dy = end[1] - start[1];
            float dz = end[2] - start[2];
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length > 1.0E-4F) {
                return new float[]{dx / length, dy / length, dz / length};
            }
        }
        return new float[]{0.0F, 1.0F, 0.0F};
    }

    /** Unit offset of a corner from the box centre; bit 0 = +X, 1 = +Y, 2 = +Z. */
    static float[] cornerOffset(int corner) {
        return new float[]{
                (corner & 1) != 0 ? 0.5F : -0.5F,
                (corner & 2) != 0 ? 0.5F : -0.5F,
                (corner & 4) != 0 ? 0.5F : -0.5F};
    }

    /** Uniform location, or -1 when the shader does not declare it. */
    private static int location(ShaderProgram program, String name) {
        try {
            return program.getUniformLocation(name);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static void uniform(ShaderProgram program, String name, float value) {
        int location = location(program, name);
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    /**
     * 0..1 的时间位置：500 表示每秒半个周期，1000 表示每秒一个周期，
     * 这样滑杆数值与旧版彩虹速度的含义保持一致。
     */
    public static float phase(FlowSettings flow, long now) {
        double value = now * 0.001D * (flow.speed / 1000.0D);
        if (flow.direction == FlowSettings.Direction.REVERSE) {
            value = -value;
        }
        value -= Math.floor(value);
        return (float) value;
    }

    static int previousProgram() {
        return GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
    }

    /** 恢复调用前的 program；previous 为 0 时用 clear() 回到固定管线。 */
    static void restoreProgram(int previous) {
        if (previous != 0) {
            GL20.glUseProgram(previous);
        } else {
            ShaderProgram.clear();
        }
    }

    static void viewport() {
        Minecraft minecraft = McRef.get();
        if (minecraft == null) {
            return;
        }
        GL11.glViewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);
    }
}
