package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Forge Multipart: every part knows its render/collision bounds.
 */
public final class FmpShapeProvider implements ShapeProvider {

    private static final Class<?> MULTIPART_TILE = classOrNull("codechicken.multipart.TileMultipart");
    private static final Method PART_LIST = methodOrNull(MULTIPART_TILE, "jPartList");

    @Override
    public boolean collect(World world, int x, int y, int z, Block block, List<OutlineShape> out) {
        if (MULTIPART_TILE == null || PART_LIST == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!MULTIPART_TILE.isInstance(tile)) {
            return false;
        }
        try {
            List<?> parts = (List<?>) PART_LIST.invoke(tile);
            if (parts == null || parts.isEmpty()) {
                return false;
            }
            boolean added = false;
            for (Object part : parts) {
                added |= addPart(part, x, y, z, out);
            }
            return added;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean addPart(Object part, int x, int y, int z, List<OutlineShape> out) {
        AxisAlignedBB render = renderBounds(part);
        List<AxisAlignedBB> collisions = new java.util.ArrayList<AxisAlignedBB>();
        Iterable<?> collisionBoxes = collisionBoxes(part);
        if (collisionBoxes != null) {
            for (Object cuboid : collisionBoxes) {
                AxisAlignedBB box = toAabb(cuboid);
                if (box != null && !isFull(box)) {
                    collisions.add(box);
                }
            }
        }

        List<AxisAlignedBB> partBoxes = new java.util.ArrayList<AxisAlignedBB>();
        // Wires/cables return their real sub-boxes from getCollisionBoxes(), while the
        // generic getRenderBounds() default is the full block. Prefer the precise boxes.
        if (render != null && !isFull(render)) {
            partBoxes.add(offset(render, x, y, z));
        } else if (!collisions.isEmpty()) {
            for (AxisAlignedBB box : collisions) {
                partBoxes.add(offset(box, x, y, z));
            }
        } else if (render != null) {
            partBoxes.add(offset(render, x, y, z));
        } else {
            AxisAlignedBB bounds = genericBounds(part);
            if (bounds != null) {
                partBoxes.add(offset(bounds, x, y, z));
            }
        }

        if (partBoxes.isEmpty()) {
            return false;
        }
        // Union inside one part removes internal crossing lines (ProjectRed four-way wires)
        // while keeping different wire parts separate.
        out.add(OutlineShape.union(partBoxes));
        return true;
    }

    private static boolean isFull(AxisAlignedBB box) {
        return box.minX <= 0.0001D && box.minY <= 0.0001D && box.minZ <= 0.0001D
                && box.maxX >= 0.9999D && box.maxY >= 0.9999D && box.maxZ >= 0.9999D;
    }

    private static AxisAlignedBB genericBounds(Object part) {
        try {
            Method method = part.getClass().getMethod("getBounds");
            Object cuboid = method.invoke(part);
            return toAabb(cuboid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AxisAlignedBB renderBounds(Object part) {
        try {
            Method method = part.getClass().getMethod("getRenderBounds");
            Object cuboid = method.invoke(part);
            return toAabb(cuboid);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Iterable<?> collisionBoxes(Object part) {
        try {
            Method method = part.getClass().getMethod("getCollisionBoxes");
            Object value = method.invoke(part);
            return value instanceof Iterable ? (Iterable<?>) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AxisAlignedBB toAabb(Object cuboid) {
        if (cuboid == null) {
            return null;
        }
        try {
            Method method = cuboid.getClass().getMethod("toAABB");
            Object value = method.invoke(cuboid);
            return value instanceof AxisAlignedBB ? (AxisAlignedBB) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static AxisAlignedBB offset(AxisAlignedBB box, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(box.minX + x, box.minY + y, box.minZ + z,
                box.maxX + x, box.maxY + y, box.maxZ + z);
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
