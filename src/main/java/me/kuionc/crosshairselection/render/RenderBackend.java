package me.kuionc.crosshairselection.render;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import me.kuionc.crosshairselection.util.McRef;

/**
 * 本模组接触 GL 状态的唯一入口。
 *
 * <p>每个 begin 都对应一个 pushAttrib，end 对应 popAttrib：调用方只需成对使用，
 * 不必关心具体开关，也不会把状态泄漏给原版渲染器。
 *
 * <p>注意事项：
 * <ul>
 *   <li>世界空间边框<b>不做</b>矩阵变换：所有顶点都是相机相对坐标，矩阵栈保持原样。</li>
 *   <li>{@code bindWhiteTexture()} 必须保留：着色器虽然不采样纹理，
 *       但混在固定管线里的状态若不确定，自定义 VAO 的属性绑定会不稳定。</li>
 *   <li>多边形偏移把边框略微拉向相机，避免与方块表面 z-fighting。</li>
 *   <li>HUD 叠加层关闭深度测试，用于准星、预览面板、进度提示。</li>
 * </ul>
 */
public final class RenderBackend {

    private RenderBackend() {
    }

    /**
     * 世界空间边框的公共状态。
     *
     * <p>会保留并恢复的属性：ENABLE / COLOR / DEPTH / POLYGON / LINE / CURRENT / TEXTURE，
     * 少了 CURRENT 或 TEXTURE 时退出后可能把残留状态带给原版渲染。
     */
    public static void beginWorldOutlines() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                | GL11.GL_POLYGON_BIT | GL11.GL_LINE_BIT | GL11.GL_CURRENT_BIT
                | GL11.GL_TEXTURE_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        // 绑定白色纹理：让纹理单元处于已知状态，避免自定义 VAO 的属性绑定受残留状态影响。
        RenderHelper.bindWhiteTexture();
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL13.GL_MULTISAMPLE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0F, -1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** 透视模式：见“穿透显示”选项，关闭深度测试后边框会覆盖在方块之上。 */
    public static void worldDepthMode(boolean seeThrough) {
        if (seeThrough) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }
    }

    public static void endWorldOutlines() {
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
    }

    /** HUD 叠加层：无深度测试、普通 alpha 混合，用于准星、预览、进度环。 */
    public static void beginGuiOverlay() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void endGuiOverlay() {
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
    }

    /**
     * 限定绘制区域，参数是 GUI 像素。
     *
     * <p>scissor 使用的是 OpenGL 的「左下角为原点」坐标系，
     * 所以这里要把 GUI 的 y 轴翻转一次，并使用物理像素尺寸。
     */
    public static void beginScissor(int guiX, int guiY, int guiWidth, int guiHeight, int guiScale) {
        Minecraft minecraft = McRef.get();
        if (minecraft == null) {
            return;
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(guiX * guiScale,
                minecraft.displayHeight - (guiY + guiHeight) * guiScale,
                guiWidth * guiScale, guiHeight * guiScale);
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
