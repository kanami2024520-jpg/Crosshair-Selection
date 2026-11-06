package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import me.kuionc.crosshairselection.util.McRef;

/**
 * Optional mining progress overlay on the targeted block.
 *
 * <p>Draws a translucent quad on the face under the crosshair whose height follows the block's
 * break progress, so the player can read how close a block is to breaking without looking at the
 * vanilla cracking texture. The static progress comes straight from the player controller.
 */
public final class BreakProgressRenderer {

    /** How far in front of the face the quad is placed to avoid z-fighting. */
    private static final double FACE_OFFSET = 0.502D;

    private BreakProgressRenderer() {
    }

    public static boolean render(MovingObjectPosition target, double cameraX, double cameraY, double cameraZ,
                                 int colour) {
        if (!SelectionBoxConfig.showBreakProgress || target == null
                || target.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK
                || !SelectionBoxConfig.breakProgressStyle.showsBar()) {
            return false;
        }
        float progress = progress();
        if (progress <= 0.0F) {
            return false;
        }
        // The face the crosshair is on; the fill grows upwards along it like a fill gauge.
        double[] normal = normal(target.sideHit);
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                target.blockX, target.blockY, target.blockZ,
                target.blockX + 1, target.blockY + 1, target.blockZ + 1);
        double cx = (box.minX + box.maxX) * 0.5D + normal[0] * FACE_OFFSET;
        double cy = (box.minY + box.maxY) * 0.5D + normal[1] * FACE_OFFSET;
        double cz = (box.minZ + box.maxZ) * 0.5D + normal[2] * FACE_OFFSET;
        double[] u = tangentU(target.sideHit);
        double[] v = tangentV(target.sideHit);

        double u0 = -0.5D;
        double u1 = 0.5D;
        double v0 = -0.5D;
        double v1 = -0.5D + progress;
        // The fill keeps the selection colour but brightens as the block gets closer to breaking.
        int quadColour = ColorUtil.withAlpha(colour & 0xFFFFFF,
                Math.round(60.0F + 110.0F * progress));

        RenderBackend.beginWorldOutlines();
        RenderBackend.worldDepthMode(false);
        // A plain white-textured quad: the fill carries no animation of its own.
        RenderHelper.bindWhiteTexture();
        RenderHelper.setColor(quadColour);
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawingQuads();
        vertex(tessellator, cx + u[0] * u0 + v[0] * v0, cy + u[1] * u0 + v[1] * v0,
                cz + u[2] * u0 + v[2] * v0, cameraX, cameraY, cameraZ, quadColour);
        vertex(tessellator, cx + u[0] * u1 + v[0] * v0, cy + u[1] * u1 + v[1] * v0,
                cz + u[2] * u1 + v[2] * v0, cameraX, cameraY, cameraZ, quadColour);
        vertex(tessellator, cx + u[0] * u1 + v[0] * v1, cy + u[1] * u1 + v[1] * v1,
                cz + u[2] * u1 + v[2] * v1, cameraX, cameraY, cameraZ, quadColour);
        vertex(tessellator, cx + u[0] * u0 + v[0] * v1, cy + u[1] * u0 + v[1] * v1,
                cz + u[2] * u0 + v[2] * v1, cameraX, cameraY, cameraZ, quadColour);
        tessellator.draw();
        RenderBackend.endWorldOutlines();
        return true;
    }

    private static void vertex(Tessellator tessellator, double x, double y, double z,
                               double cameraX, double cameraY, double cameraZ, int colour) {
        RenderHelper.setVertexColor(tessellator, colour);
        tessellator.addVertexWithUV(x - cameraX, y - cameraY, z - cameraZ, 0.5D, 0.5D);
    }

    /**
     * Reads {@code PlayerControllerMP.curBlockDamageMP} reflectively.
     *
     * <p>The mixin that exposes this field is rewritten at runtime, so referencing the mixin class
     * from here throws NoClassDefFoundError: the class loader cannot link a mixin-applied class
     * through the normal path. Reflection on the real field avoids that entirely.
     */
    /** Current block break progress, 0..1, exposed for the sweep source. */
    public static float breakProgress() {
        return progress();
    }

    private static float progress() {
        Minecraft minecraft = McRef.get();
        Object controller = minecraft.playerController;
        if (controller == null) {
            return 0.0F;
        }
        try {
            if (breakProgressField == null || breakProgressField.getDeclaringClass() != controller.getClass()) {
                java.lang.reflect.Field field = findField(controller.getClass(), "curBlockDamageMP",
                        "field_78770_f");
                breakProgressField = field;
            }
            if (breakProgressField == null) {
                return 0.0F;
            }
            Object value = breakProgressField.get(controller);
            return value instanceof Float ? Math.max(0.0F, Math.min(1.0F, (Float) value)) : 0.0F;
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    private static java.lang.reflect.Field breakProgressField;

    private static java.lang.reflect.Field findField(Class<?> owner, String... names) {
        for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
            for (String name : names) {
                try {
                    java.lang.reflect.Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // Try the next name or the superclass.
                }
            }
        }
        return null;
    }

    private static double[] normal(int side) {
        switch (side) {
            case 0:
                return new double[]{0.0D, -1.0D, 0.0D};
            case 1:
                return new double[]{0.0D, 1.0D, 0.0D};
            case 2:
                return new double[]{0.0D, 0.0D, -1.0D};
            case 3:
                return new double[]{0.0D, 0.0D, 1.0D};
            case 4:
                return new double[]{-1.0D, 0.0D, 0.0D};
            case 5:
            default:
                return new double[]{1.0D, 0.0D, 0.0D};
        }
    }

    private static double[] tangentU(int side) {
        switch (side) {
            case 4:
            case 5:
                return new double[]{0.0D, 0.0D, 1.0D};
            default:
                return new double[]{1.0D, 0.0D, 0.0D};
        }
    }

    private static double[] tangentV(int side) {
        switch (side) {
            case 0:
            case 1:
                return new double[]{0.0D, 0.0D, 1.0D};
            default:
                return new double[]{0.0D, 1.0D, 0.0D};
        }
    }
}
