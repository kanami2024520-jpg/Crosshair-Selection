package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import me.kuionc.crosshairselection.util.McRef;

/**
 * 选框边框的几何收集与绘制。
 *
 * <p>每条边对应一个朝向相机的四边形，顶点在 Java 端按真实世界尺寸构造好；
 * 片段着色器再用胶囊体距离场把它裁成带圆头的抗锯齿线条。
 *
 * <p>注意事项：
 * <ul>
 *   <li>顶点必须经 GTNHLib 的 {@link DirectTessellator} 提交：自建的 VAO / 顶点属性绑定
 *       在当前渲染环境下不产生任何像素（曾经连固定在屏幕上的调试四边形都画不出来）。</li>
 *   <li>必须使用私有实例，不能用 {@code TessellatorManager.startCapturingDirect()}，
 *       那会替换全局 tessellator，与同一帧里原版粒子绘制冲突。</li>
 *   <li>VAO 每帧新建：{@code uploadToVBO} 会执行四边形→三角形优化，
 *       而 {@code updateToVBO} 只是原样覆盖，复用会得到错位的三角形。</li>
 *   <li>线宽按段携带，但着色器只有一个 uHalfWidthPixels，整批取最大值；
 *       若将来需要九格与外框不同粗细，需要改成逐边的数组。</li>
 * </ul>
 */
public final class RibbonBuffer {

    private static final String VERTEX = "shaders/selection.vert";
    private static final String FRAGMENT = "shaders/selection.frag";

    /**
     * 每段的浮点数：两个端点(6) + 法线×半宽(3) + 线宽(1)。
     * 后三项换算成「四边形半宽向量」，绘制时直接用。
     */
    private static final int FLOATS_PER_SEGMENT = 10;
    /** 每帧最大边数；必须与 selection.frag 里 uEdgeA/uEdgeB 的数组长度一致。 */
    private static final int MAX_SEGMENTS = 64;

    private static ShaderProgram program;
    private static boolean failed;

    private static int uViewport;
    private static int uHalfWidthPixels;
    private static int uColour;
    private static int uCamera;
    private static int uSweepAxisY;
    private static int uModelViewProjection;
    private static int uEdgeA;
    private static int uEdgeB;

    private static final float[] segments = new float[MAX_SEGMENTS * FLOATS_PER_SEGMENT];
    private static final float[] edgeA = new float[MAX_SEGMENTS * 3];
    private static final float[] edgeB = new float[MAX_SEGMENTS * 3];
    private static int segmentCount;

    private static int batchColour;
    private static FlowSettings batchFlow;
    private static float progress = 1.0F;
    private static float cameraX;
    private static float cameraY;
    private static float cameraZ;

    private RibbonBuffer() {
    }

    public static void setProgress(float value) {
        progress = value;
    }

    /**
     * 惰性加载着色器与 uniform 位置。任何一边缺失都打印出来，
     * 因为「静默失败」在这条链路上是历史上最消耗时间的故障模式。
     */
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
        uViewport = location(program, "uViewport");
        uHalfWidthPixels = location(program, "uHalfWidthPixels");
        uColour = location(program, "uColour");
        uCamera = location(program, "uCamera");
        uSweepAxisY = location(program, "uSweepAxisY");
        uModelViewProjection = location(program, "uModelViewProjection");
        uEdgeA = location(program, "uEdgeA");
        uEdgeB = location(program, "uEdgeB");
        if (uViewport < 0 || uHalfWidthPixels < 0) {
            System.err.println("[Crosshair-Selection] outline uniforms missing: uViewport=" + uViewport
                    + " uHalfWidthPixels=" + uHalfWidthPixels);
        }
        ShaderSupport.ensureQuad();
        return true;
    }

    /**
     * GTNHLib 的 {@code getUniformLocation} 在被优化掉的 uniform 上会抛 NPE，
     * 这里统一转成 -1，调用方据此跳过上传。
     */
    private static int location(ShaderProgram shader, String name) {
        try {
            return shader.getUniformLocation(name);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    /**
     * 开始一批边框。
     *
     * @param flow 流光参数；传 null 表示不流动（GT 网格用它保持静止）
     * @param camX 相机坐标，已包含平滑过渡的偏移
     */
    public static boolean begin(int colour, FlowSettings flow,
                                double camX, double camY, double camZ) {
        if (!isAvailable()) {
            return false;
        }
        segmentCount = 0;
        batchColour = colour;
        batchFlow = flow;
        cameraX = (float) camX;
        cameraY = (float) camY;
        cameraZ = (float) camZ;
        return true;
    }

    /**
     * 入队一条边。
     *
     * @param nx,ny,nz   朝向相机的法线，单位向量
     * @param halfWidthWorld 胶囊半径（世界单位）
     *
     * <p>端点不做外延：胶囊两端的圆头在共享角点处自然重叠，已经能把拐角补满；
     * 再外延反而会让线条探出方块顶点。
     */
    public static void addSegment(double ax, double ay, double az,
                                  double bx, double by, double bz,
                                  double nx, double ny, double nz,
                                  double halfWidthWorld,
                                  float thicknessPixels) {
        if (segmentCount >= MAX_SEGMENTS) {
            return;
        }
        int base = segmentCount * FLOATS_PER_SEGMENT;
        segments[base] = (float) ax;
        segments[base + 1] = (float) ay;
        segments[base + 2] = (float) az;
        segments[base + 3] = (float) bx;
        segments[base + 4] = (float) by;
        segments[base + 5] = (float) bz;
        // 四边形比胶囊略宽（8% + 固定余量），确保圆头在斜角和亚像素宽度下不被自身边裁掉。
        double quadHalf = halfWidthWorld + halfWidthWorld * 0.08D + 0.0004D;
        segments[base + 6] = (float) (nx * quadHalf);
        segments[base + 7] = (float) (ny * quadHalf);
        segments[base + 8] = (float) (nz * quadHalf);
        segments[base + 9] = thicknessPixels;
        segmentCount++;
    }

    /**
     * 构建并绘制本帧的全部边。
     *
     * <p>顺序很重要：先写完所有顶点再 upload，因为 VBO 容量在 upload/update 时确定；
     * 着色器 uniform 必须在本帧 program.use() 之后上传，否则会写到上一帧的程序上。
     */
    public static void end() {
        if (segmentCount <= 0) {
            return;
        }
        Minecraft minecraft = McRef.get();
        if (minecraft == null) {
            segmentCount = 0;
            return;
        }
        int width = minecraft.displayWidth;
        int height = minecraft.displayHeight;

        int previous = ShaderSupport.previousProgram();
        IVertexArrayObject vao = null;
        try {
            // 私有实例：全局捕获会替换共享 tessellator，同一帧里原版粒子绘制会抛
            // "Already tesselating!" 直接崩溃。容量按顶点计，留有充足余量。
            DirectTessellator tessellator = new DirectTessellator(
                    segmentCount * 4 * 32);
            tessellator.startDrawingQuads();
            float maxHalfWidth = 0.35F;
            for (int i = 0; i < segmentCount; i++) {
                int base = i * FLOATS_PER_SEGMENT;
                float ax = segments[base];
                float ay = segments[base + 1];
                float az = segments[base + 2];
                float bx = segments[base + 3];
                float by = segments[base + 4];
                float bz = segments[base + 5];
                float ox = segments[base + 6];
                float oy = segments[base + 7];
                float oz = segments[base + 8];
                maxHalfWidth = Math.max(maxHalfWidth, segments[base + 9] * 0.5F);
                edgeA[i * 3] = ax;
                edgeA[i * 3 + 1] = ay;
                edgeA[i * 3 + 2] = az;
                edgeB[i * 3] = bx;
                edgeB[i * 3 + 1] = by;
                edgeB[i * 3 + 2] = bz;
                // UV 只放边编号：POSITION_TEXTURE 只有两分量，装不下两个三维端点，
                // 端点随 uniform 数组一起上传，片段阶段按编号回查。
                addVertex(tessellator, ax + ox, ay + oy, az + oz, i);
                addVertex(tessellator, bx + ox, by + oy, bz + oz, i);
                addVertex(tessellator, bx - ox, by - oy, bz - oz, i);
                addVertex(tessellator, ax - ox, ay - oy, az - oz, i);
            }
            // 每帧新建 VAO：只有 uploadToVBO 会做四边形→三角形优化，
            // updateToVBO 是原样覆盖，复用会把四边形当三角形画，出现方向固定的错位方框。
            // 顶点量很小，分配开销可以忽略。
            vao = tessellator.uploadToVBO(VertexBufferType.MUTABLE);

            program.use();
            set2f(uViewport, width, height);
            FlowSettings flow = batchFlow == null ? SelectionBoxConfig.flow : batchFlow;
            set1f(uSweepAxisY, Math.abs(ShaderSupport.sweepAxis(flow)[1]));
            if (uColour >= 0) {
                GL20.glUniform4f(uColour,
                        ColorUtil.red(batchColour) / 255.0F,
                        ColorUtil.green(batchColour) / 255.0F,
                        ColorUtil.blue(batchColour) / 255.0F,
                        ColorUtil.alpha(batchColour) / 255.0F);
            }
            set3f(uCamera, cameraX, cameraY, cameraZ);
            ShaderSupport.uploadFlow(program, flow, System.currentTimeMillis(), progress);
            set1f(uHalfWidthPixels, maxHalfWidth);
            uploadMatrix();
            uploadEdges();

            GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            vao.bind();
            vao.draw();
            vao.unbind();
            GL11.glPopAttrib();
        } catch (Throwable t) {
            System.err.println("[Crosshair-Selection] outline draw failed");
            t.printStackTrace();
        } finally {
            if (vao != null) {
                try {
                    vao.delete();
                } catch (Throwable ignored) {
                    // Ignore cleanup failures.
                }
            }
            ShaderSupport.restoreProgram(previous);
            segmentCount = 0;
        }
    }

    /**
     * 把光栅化将要使用的矩阵复制一份成 MVP 上传。
     *
     * <p>片段着色器要用它重新投影端点，必须与顶点阶段看到的是同一个矩阵，
     * 否则算出的线段会和实际几何错位。<b>乘法顺序是 PROJECTION × MODELVIEW</b>，
     * 反了会让坐标飞到极远处。
     */
    private static void uploadMatrix() {
        if (uModelViewProjection < 0) {
            return;
        }
        java.nio.FloatBuffer projection = org.lwjgl.BufferUtils.createFloatBuffer(16);
        java.nio.FloatBuffer modelView = org.lwjgl.BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        float[] mvp = new float[16];
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0.0F;
                for (int k = 0; k < 4; k++) {
                    sum += projection.get(k * 4 + row) * modelView.get(col * 4 + k);
                }
                mvp[col * 4 + row] = sum;
            }
        }
        java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(16);
        buffer.put(mvp);
        buffer.flip();
        GL20.glUniformMatrix4(uModelViewProjection, false, buffer);
    }

    /** 逐边上传端点数组，长度严格等于本帧段数，避免读到上一帧的残留值。 */
    private static void uploadEdges() {
        if (uEdgeA >= 0) {
            java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(segmentCount * 3);
            buffer.put(edgeA, 0, segmentCount * 3);
            buffer.flip();
            GL20.glUniform3(uEdgeA, buffer);
        }
        if (uEdgeB >= 0) {
            java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(segmentCount * 3);
            buffer.put(edgeB, 0, segmentCount * 3);
            buffer.flip();
            GL20.glUniform3(uEdgeB, buffer);
        }
    }

    private static void addVertex(DirectTessellator tessellator,
                                  float x, float y, float z, float edgeIndex) {
        tessellator.addVertexWithUV(x, y, z, edgeIndex, 0.0D);
    }

    private static void set1f(int location, float value) {
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private static void set2f(int location, int x, int y) {
        if (location >= 0) {
            GL20.glUniform2f(location, (float) x, (float) y);
        }
    }

    private static void set3f(int location, float x, float y, float z) {
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }
}
