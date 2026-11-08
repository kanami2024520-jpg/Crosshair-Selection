package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static me.kuionc.crosshairselection.compat.Reflect.*;

/**
 * Translocator plates are one block made from a base plate and a movable centre button.
 * getParts() exposes both cuboids, but a block can hold up to six attachments (one per face).
 * Selection must keep only the attachment under the crosshair, then union its base and button.
 */
public final class TranslocatorShapeProvider implements ShapeProvider {

    private static final Class<?> BLOCK_TRANSLOCATOR = classOrNull(
            "codechicken.translocator.BlockTranslocator");
    private static final Class<?> CUBOID = classOrNull("codechicken.lib.vec.Cuboid6");
    private static final Class<?> INDEXED_CUBOID = classOrNull(
            "codechicken.lib.raytracer.IndexedCuboid6");
    private static final Method GET_PARTS = methodOrNull(BLOCK_TRANSLOCATOR, "getParts",
            World.class, int.class, int.class, int.class);
    private static final Method TO_AABB = methodOrNull(CUBOID, "toAABB");
    private static final Field PART_DATA = fieldOrNull(INDEXED_CUBOID, "data");

    @Override
    public boolean collect(World world, int x, int y, int z, Block block, net.minecraft.util.MovingObjectPosition target,
                           List<OutlineShape> out) {
        if (BLOCK_TRANSLOCATOR == null || GET_PARTS == null || TO_AABB == null
                || !BLOCK_TRANSLOCATOR.isInstance(block)) {
            return false;
        }
        try {
            Object parts = GET_PARTS.invoke(block, world, x, y, z);
            if (!(parts instanceof Iterable)) {
                return false;
            }
            List<AxisAlignedBB> selected = new ArrayList<AxisAlignedBB>();
            List<AxisAlignedBB> all = new ArrayList<AxisAlignedBB>();
            Object hitData = HitData.raw(target);
            int subHit = hitData instanceof Number ? ((Number) hitData).intValue() : -1;
            int attachment = subHit >= 0 ? subHit % 6 : -1;
            for (Object part : (Iterable<?>) parts) {
                List<AxisAlignedBB> destination = all;
                if (PART_DATA != null && attachment >= 0) {
                    Object value = PART_DATA.get(part);
                    if (value instanceof Number) {
                        int data = ((Number) value).intValue();
                        if (data == attachment || data == attachment + 6) {
                            destination = selected;
                        }
                    }
                } else if (attachment >= 0) {
                    destination = selected;
                }
                Object aabb = TO_AABB.invoke(part);
                if (aabb instanceof AxisAlignedBB) {
                    destination.add((AxisAlignedBB) aabb);
                }
            }
            List<AxisAlignedBB> boxes = selected.isEmpty() ? all : selected;
            if (boxes.isEmpty()) {
                return false;
            }
            out.add(OutlineShape.union(boxes));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

}
