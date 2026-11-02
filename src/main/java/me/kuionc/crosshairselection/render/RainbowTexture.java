package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;

/**
 * A 1D HSV rainbow texture. Flow is done by texture coordinates instead of many tiny line
 * segments, so the GPU interpolates the hue per fragment.
 */
public final class RainbowTexture {

    private static int textureId = -1;
    private static int textureSize = -1;

    private RainbowTexture() {
    }

    public static void bind() {
        int size = resolveSize();
        if (textureId == -1 || size != textureSize) {
            create(size);
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
    }

    /**
     * 0 means Auto: pick a texture width from the current resolution. Any other value is
     * snapped to the nearest supported size so old config files cannot create exotic textures.
     */
    public static int resolveSize() {
        int configured = SelectionBoxConfig.rainbowTextureSize;
        if (configured <= 0) {
            int width = Minecraft.getMinecraft().displayWidth;
            if (width >= 1920) {
                return 512;
            }
            return width >= 1280 ? 256 : 128;
        }
        if (configured <= 128) {
            return 128;
        }
        return configured <= 256 ? 256 : 512;
    }

    private static void create(int size) {
        if (textureId != -1) {
            GL11.glDeleteTextures(textureId);
        }
        textureId = GL11.glGenTextures();
        textureSize = size;
        ByteBuffer data = BufferUtils.createByteBuffer(size * 4);
        for (int i = 0; i < size; i++) {
            int rgba = ColorUtil.hsbToRgb(i / (float) size, 0.8F, 1.0F);
            data.put((byte) (rgba >> 16 & 255));
            data.put((byte) (rgba >> 8 & 255));
            data.put((byte) (rgba & 255));
            data.put((byte) 255);
        }
        data.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, size, 1, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
    }
}
