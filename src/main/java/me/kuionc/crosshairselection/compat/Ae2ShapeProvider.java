package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Applied Energistics cable buses. selectPart(hitVec) identifies the exact cable or facade
 * under the crosshair; the provider asks that part for its boxes and draws only those.
 */
public final class Ae2ShapeProvider implements ShapeProvider {

    private static final Class<?> CABLE_TILE = classOrNull("appeng.tile.networking.TileCableBus");
    private static final Class<?> SELECTED_PART = classOrNull("appeng.api.parts.SelectedPart");
    private static final Class<?> IPART = classOrNull("appeng.api.parts.IPart");
    private static final Class<?> IFACADE_PART = classOrNull("appeng.api.parts.IFacadePart");
    private static final Class<?> COLLISION_HELPER = classOrNull("appeng.api.parts.IPartCollisionHelper");
    private static final Class<?> BUS_HELPER = classOrNull("appeng.parts.BusCollisionHelper");

    private static final Method GET_BOXES = methodOrNull(CABLE_TILE, "getSelectedBoundingBoxesFromPool",
            World.class, int.class, int.class, int.class, Entity.class, boolean.class);
    private static final Method SELECT_PART = methodOrNull(CABLE_TILE, "selectPart", Vec3.class);
    private static final Method PART_BOXES = methodOrNull(IPART, "getBoxes", COLLISION_HELPER);
    private static final Method FACADE_BOXES = methodOrNull(IFACADE_PART, "getBoxes",
            COLLISION_HELPER, Entity.class);
    private static final Constructor<?> HELPER_CONSTRUCTOR = constructorOrNull(BUS_HELPER,
            List.class, ForgeDirection.class, Entity.class, boolean.class);
    private static final Field PART_FIELD = fieldOrNull(SELECTED_PART, "part");
    private static final Field FACADE_FIELD = fieldOrNull(SELECTED_PART, "facade");
    private static final Field SIDE_FIELD = fieldOrNull(SELECTED_PART, "side");

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (CABLE_TILE == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!CABLE_TILE.isInstance(tile)) {
            return false;
        }

        if (target != null && target.hitVec != null && SELECT_PART != null
                && SELECTED_PART != null && HELPER_CONSTRUCTOR != null) {
            try {
                Vec3 localHit = Vec3.createVectorHelper(
                        target.hitVec.xCoord - x,
                        target.hitVec.yCoord - y,
                        target.hitVec.zCoord - z);
                Object selected = SELECT_PART.invoke(tile, localHit);
                if (selected != null) {
                    List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
                    Object part = PART_FIELD != null ? PART_FIELD.get(selected) : null;
                    Object facade = FACADE_FIELD != null ? FACADE_FIELD.get(selected) : null;
                    Object side = SIDE_FIELD != null ? SIDE_FIELD.get(selected) : null;
                    Object helper = HELPER_CONSTRUCTOR.newInstance(boxes,
                            side instanceof ForgeDirection ? side : ForgeDirection.UNKNOWN, null, true);
                    if (part != null && PART_BOXES != null) {
                        PART_BOXES.invoke(part, helper);
                    } else if (facade != null && FACADE_BOXES != null) {
                        FACADE_BOXES.invoke(facade, helper, (Object) null);
                    }
                    if (addOffsetBoxes(boxes, x, y, z, out)) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
                // Fall back to the complete bus shape below.
            }
        }

        return collectAllBoxes(tile, world, x, y, z, out);
    }

    private static boolean collectAllBoxes(TileEntity tile, World world, int x, int y, int z,
                                           List<OutlineShape> out) {
        if (GET_BOXES == null) {
            return false;
        }
        try {
            Object value = GET_BOXES.invoke(tile, world, x, y, z, null, false);
            return value instanceof Iterable && addOffsetBoxes((Iterable<?>) value, x, y, z, out);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean addOffsetBoxes(Iterable<?> boxes, int x, int y, int z,
                                          List<OutlineShape> out) {
        boolean added = false;
        for (Object object : boxes) {
            if (!(object instanceof AxisAlignedBB)) {
                continue;
            }
            AxisAlignedBB box = (AxisAlignedBB) object;
            if (box.maxX - box.minX <= 0.0001D || box.maxY - box.minY <= 0.0001D
                    || box.maxZ - box.minZ <= 0.0001D) {
                continue;
            }
            out.add(OutlineShape.box(AxisAlignedBB.getBoundingBox(
                    box.minX + x, box.minY + y, box.minZ + z,
                    box.maxX + x, box.maxY + y, box.maxZ + z), false));
            added = true;
        }
        return added;
    }

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Constructor<?> constructorOrNull(Class<?> owner, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getConstructor(params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field fieldOrNull(Class<?> owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            Field field = owner.getField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
