package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.CrosshairSelection;
import me.kuionc.crosshairselection.compat.Ae2ShapeProvider;
import me.kuionc.crosshairselection.compat.EnderIoShapeProvider;
import me.kuionc.crosshairselection.compat.FmpShapeProvider;
import me.kuionc.crosshairselection.compat.GtCoverShapeProvider;
import me.kuionc.crosshairselection.compat.GtPipeShapeProvider;
import me.kuionc.crosshairselection.compat.ProjectRedWireShapeProvider;
import me.kuionc.crosshairselection.compat.TranslocatorShapeProvider;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the boxes/cylinders used to draw the selection outline.
 *
 * <p>顺序有意义：{@code Ae2ShapeProvider} 必须排在 {@code FmpShapeProvider} 前面。
 * AE 总线被 Forge Multipart 接管后（例如同格放了 ProjectRed 红合金丝）会变成
 * {@code appeng.fmp.CableBusPart}，而 FMP 视角下它的碰撞盒是整条总线；
 * 由 AE 自己先接手才能精确到单个部件。
 */
public final class ShapeCollector {

    private static final ShapeProvider[] PROVIDERS = {
            new GtCoverShapeProvider(),
            new GtPipeShapeProvider(),
            new TranslocatorShapeProvider(),
            new Ae2ShapeProvider(),
            new EnderIoShapeProvider(),
            new ProjectRedWireShapeProvider(),
            new FmpShapeProvider()
    };

    private static String lastShapeSignature;

    private ShapeCollector() {
    }

    /** 形状签名变化时写一行，用于区分“目标没变但形状在变”。 */
    private static void logShapes(String provider, List<OutlineShape> shapes) {
        if (!SelectionDebugLog.enabled()) {
            return;
        }
        StringBuilder signature = new StringBuilder(provider).append('|').append(shapes.size());
        for (OutlineShape shape : shapes) {
            if (shape.getKind() == OutlineShape.Kind.BOX) {
                signature.append('|').append(box(shape.getBox()));
            } else if (shape.getKind() == OutlineShape.Kind.UNION) {
                signature.append("|union").append(shape.getBoxes().size());
            } else {
                signature.append("|cyl");
            }
        }
        String value = signature.toString();
        if (value.equals(lastShapeSignature)) {
            return;
        }
        lastShapeSignature = value;
        SelectionDebugLog.log("SHAPE", value);
    }

    private static String box(net.minecraft.util.AxisAlignedBB box) {
        if (box == null) {
            return "null";
        }
        return String.format(java.util.Locale.ROOT, "(%.3f,%.3f,%.3f)-(%.3f,%.3f,%.3f)",
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public static List<OutlineShape> collect(World world, Block block, int x, int y, int z,
                                             AxisAlignedBB primary, MovingObjectPosition target) {
        List<OutlineShape> shapes = new ArrayList<OutlineShape>();
        for (ShapeProvider provider : PROVIDERS) {
            try {
                if (provider.collect(world, x, y, z, block, target, shapes) && !shapes.isEmpty()) {
                    logShapes(provider.getClass().getSimpleName(), shapes);
                    return shapes;
                }
            } catch (Throwable throwable) {
                CrosshairSelection.LOGGER.warn("Shape provider failed for " + block, throwable);
            }
            shapes.clear();
        }

        shapes.add(OutlineShape.box(primary, true));
        logShapes("primary", shapes);
        if (SelectionBoxConfig.linkBlocks) {
            for (AxisAlignedBB linked : LinkedBoxes.collect(world, block, x, y, z, primary)) {
                shapes.add(OutlineShape.box(linked, true));
            }
        }
        return shapes;
    }
}
