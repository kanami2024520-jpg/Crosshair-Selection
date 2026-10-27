package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.CrosshairSelection;
import me.kuionc.crosshairselection.compat.Ae2ShapeProvider;
import me.kuionc.crosshairselection.compat.EnderIoShapeProvider;
import me.kuionc.crosshairselection.compat.FmpShapeProvider;
import me.kuionc.crosshairselection.compat.GtPipeShapeProvider;
import me.kuionc.crosshairselection.config.SelectionBoxConfig;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses the boxes/cylinders used to draw the selection outline.
 */
public final class ShapeCollector {

    private static final ShapeProvider[] PROVIDERS = {
            new GtPipeShapeProvider(),
            new Ae2ShapeProvider(),
            new EnderIoShapeProvider(),
            new FmpShapeProvider()
    };

    private ShapeCollector() {
    }

    public static List<OutlineShape> collect(World world, Block block, int x, int y, int z,
                                             AxisAlignedBB primary) {
        List<OutlineShape> shapes = new ArrayList<OutlineShape>();
        for (ShapeProvider provider : PROVIDERS) {
            try {
                if (provider.collect(world, x, y, z, block, shapes) && !shapes.isEmpty()) {
                    return shapes;
                }
            } catch (Throwable throwable) {
                CrosshairSelection.LOGGER.warn("Shape provider failed for " + block, throwable);
            }
            shapes.clear();
        }

        shapes.add(OutlineShape.box(primary, true));
        if (SelectionBoxConfig.linkBlocks) {
            for (AxisAlignedBB linked : LinkedBoxes.collect(world, block, x, y, z, primary)) {
                shapes.add(OutlineShape.box(linked, true));
            }
        }
        return shapes;
    }
}
