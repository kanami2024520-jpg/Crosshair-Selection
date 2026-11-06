package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.DirectTessellator;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.IVertexArrayObject;
import com.gtnewhorizon.gtnhlib.client.renderer.vao.VertexBufferType;
import me.kuionc.crosshairselection.config.FlowSettings;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import me.kuionc.crosshairselection.util.McRef;

/**
 * 选框内部填充的绘制。
 *
 * <p>和描边同一套机制：顶点经 GTNHLib 的 {@link DirectTessellator} 提交，
 * 每帧新建 VAO，由自定义着色器输出颜色。以前这里走的是 {@code Tessellator}
 * （固定管线），在光影包下会被当成不透明几何写深度，于是填充把边框挡掉。
 *
 * <p>注意事项：
 * <ul>
 *   <li>只负责「盒子的半透明填充」，几何就是六个面，不需要描边的胶囊 SDF。</li>
 *   <li>颜色公式与描边共用：流光的相位、色带、扫光参数都来自
 *       {@link ShaderSupport#uploadFlow}，所以填充和边框的彩虹同步。</li>
 *   <li>盒子按<b>世界坐标</b>传入，着色器内部再减去相机，和描边保持一致。</li>
 * </ul>
 */
public final class FillBuffer {

    private static final String VERTEX = "shaders/fill.vert";
    private static final String FRAGMENT = "shaders/fill.frag";

    /** 单帧最多缓存的四边形数；一个盒子 6 个面，足够覆盖多盒目标。 */
    private static final int MAX_QUADS = 512;
    private static final int FLOATS_PER_VERTEX = 3;
    private static final int VERTICES_PER_QUAD = 4;

    private static final float[] vertices =
            new float[MAX_QUADS * VERTICES_PER_QUAD * FLOATS_PER_VERTEX];

    private static ShaderProgram program;
    private static boolean failed;

    private static int uColour;
    private static int uCamera;

    private static int quadCount;
    private static int batchColour;
    private static FlowSettings batchFlow;
    private static float cameraX;
    private static float cameraY;
    private static float cameraZ;
    /** 世界坐标的整批包围盒，用于把流光/扫光映射到形状范围内。 */
    private static double[] batchBounds;

    private FillBuffer() {
    }

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
            uColour = program.getUniformLocation("uColour");
            uCamera = program.getUniformLocation("uCamera");
            return true;
        } catch (Throwable ignored) {
            failed = true;
            program = null;
            return false;
        }
    }

    /** 开始一批填充。同一帧里可以连续调用多次，每次都会立即绘制。 */
    public static boolean begin(int colour, FlowSettings flow,
                                double camX, double camY, double camZ) {
        return begin(colour, flow, camX, camY, camZ, null);
    }

    /** @param bounds 整批填充的世界坐标包围盒 {@code {minX,minY,minZ,maxX,maxY,maxZ}} */
    public static boolean begin(int colour, FlowSettings flow,
                                double camX, double camY, double camZ, double[] bounds) {
        if (!isAvailable()) {
            return false;
        }
        quadCount = 0;
        batchColour = colour;
        batchFlow = flow;
        cameraX = (float) camX;
        cameraY = (float) camY;
        cameraZ = (float) camZ;
        batchBounds = bounds;
        return true;
    }

    /** 追加一个世界坐标的盒子。 */
    public static void addBox(AxisAlignedBB box) {
        if (quadCount >= MAX_QUADS) {
            flush();
        }
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;
        // 下、上
        addQuad(x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2);
        addQuad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1);
        // 北、南
        addQuad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1);
        addQuad(x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2);
        // 西、东
        addQuad(x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1);
        addQuad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2);
    }

    public static void end() {
        flush();
    }

    private static void addQuad(float... corners) {
        int base = quadCount * VERTICES_PER_QUAD * FLOATS_PER_VERTEX;
        System.arraycopy(corners, 0, vertices, base, corners.length);
        quadCount++;
    }

    private static ShaderSupport.FlowFrame flowFrame(FlowSettings flow) {
        if (batchBounds == null || batchBounds.length < 6) {
            return ShaderSupport.FlowFrame.worldUnit(flow);
        }
        return ShaderSupport.FlowFrame.world(flow, batchBounds[0], batchBounds[1], batchBounds[2],
                batchBounds[3], batchBounds[4], batchBounds[5]);
    }

    private static void flush() {
        if (quadCount <= 0) {
            return;
        }
        Minecraft minecraft = McRef.get();
        if (minecraft == null) {
            quadCount = 0;
            return;
        }

        int previous = ShaderSupport.previousProgram();
        IVertexArrayObject vao = null;
        try {
            DirectTessellator tessellator = new DirectTessellator(
                    quadCount * VERTICES_PER_QUAD * 32);
            tessellator.startDrawingQuads();
            for (int i = 0; i < quadCount; i++) {
                int base = i * VERTICES_PER_QUAD * FLOATS_PER_VERTEX;
                for (int v = 0; v < VERTICES_PER_QUAD; v++) {
                    int at = base + v * FLOATS_PER_VERTEX;
                    // UV 不参与填充着色，但顶点格式固定是 POSITION_TEXTURE。
                    tessellator.addVertexWithUV(
                            vertices[at] - cameraX,
                            vertices[at + 1] - cameraY,
                            vertices[at + 2] - cameraZ, 0.0D, 0.0D);
                }
            }
            vao = tessellator.uploadToVBO(VertexBufferType.MUTABLE);

            program.use();
            GL20.glUniform4f(uColour,
                    ColorUtil.red(batchColour) / 255.0F,
                    ColorUtil.green(batchColour) / 255.0F,
                    ColorUtil.blue(batchColour) / 255.0F,
                    ColorUtil.alpha(batchColour) / 255.0F);
            GL20.glUniform3f(uCamera, cameraX, cameraY, cameraZ);
            FlowSettings flow = batchFlow == null ? SelectionBoxConfig.flow : batchFlow;
            ShaderSupport.uploadFlow(program, flow, System.currentTimeMillis(), flow.progress,
                    flowFrame(flow));

            GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            // 填充绝不写深度：否则它会在地形/光影的深度缓冲里挡住之后的描边。
            GL11.glDepthMask(false);
            vao.bind();
            vao.draw();
            vao.unbind();
            GL11.glPopAttrib();
        } catch (Throwable t) {
            System.err.println("[Crosshair-Selection] fill draw failed");
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
            quadCount = 0;
        }
    }
}
