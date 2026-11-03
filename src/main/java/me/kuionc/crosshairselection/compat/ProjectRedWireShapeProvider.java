package me.kuionc.crosshairselection.compat;

import me.kuionc.crosshairselection.render.OutlineShape;
import me.kuionc.crosshairselection.render.ShapeProvider;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * ProjectRed wires expose intentionally large selection boxes (about 8px wide) while the
 * rendered wire model is only about 2px wide. Shrink each selection box on its cross-section
 * axis so the outline follows the visible wire instead of drawing a ring around it.
 */
public final class ProjectRedWireShapeProvider implements ShapeProvider {

    private static final double WIRE_WIDTH = 0.25D;
    private static final double END_PADDING = 0.0625D;

    private static final Class<?> MULTIPART_TILE = classOrNull("codechicken.multipart.TileMultipart");
    private static final Class<?> WIRE_PART = classOrNull("mrtjp.projectred.transmission.WirePart");
    private static final Method PART_LIST = methodOrNull(MULTIPART_TILE, "jPartList");
    private static final Method SIDE = methodOrNull(WIRE_PART, "side");
    private static final Method COLLISION_BOXES = methodOrNull(WIRE_PART, "getCollisionBoxes");
    private static final Class<?> RENDER_WIRE = classOrNull("mrtjp.projectred.transmission.RenderWire$");
    private static final Object RENDER_WIRE_INSTANCE = module(RENDER_WIRE);
    private static final Method MODEL_KEY = methodOrNull(RENDER_WIRE, "modelKey", WIRE_PART);
    private static final Method GET_MODEL = methodOrNull(RENDER_WIRE, "getOrGenerateModel", int.class);
    private static final Method MODEL_BOUNDS = methodOrNull(
            classOrNull("codechicken.lib.render.CCModel"), "bounds");
    private static final Method CUBOID_TO_AABB = methodOrNull(
            classOrNull("codechicken.lib.vec.Cuboid6"), "toAABB");

    @Override
    public boolean collect(World world, int x, int y, int z, Block block,
                           MovingObjectPosition target, List<OutlineShape> out) {
        if (MULTIPART_TILE == null || WIRE_PART == null || PART_LIST == null
                || SIDE == null || COLLISION_BOXES == null) {
            return false;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        if (!MULTIPART_TILE.isInstance(tile)) {
            return false;
        }
        int partIndex = HitData.partIndex(target);
        if (partIndex < 0) {
            return false;
        }
        try {
            List<?> parts = (List<?>) PART_LIST.invoke(tile);
            if (parts == null || partIndex >= parts.size()) {
                return false;
            }
            Object part = parts.get(partIndex);
            if (!WIRE_PART.isInstance(part)) {
                return false;
            }
            int side = ((Number) SIDE.invoke(part)).intValue();
            Object boxes = COLLISION_BOXES.invoke(part);
            if (!(boxes instanceof Iterable)) {
                return false;
            }
            AxisAlignedBB modelBounds = modelBounds(part);
            List<AxisAlignedBB> collisions = new ArrayList<AxisAlignedBB>();
            for (Object cuboid : (Iterable<?>) boxes) {
                AxisAlignedBB box = toAabb(cuboid);
                if (box != null) {
                    collisions.add(box);
                }
            }
            int armCount = countArms(collisions, side);
            boolean hasArms = armCount > 0;
            boolean singleArm = armCount == 1;
            List<AxisAlignedBB> visual = new ArrayList<AxisAlignedBB>();
            for (AxisAlignedBB box : collisions) {
                visual.add(shrinkWireBox(box, side, modelBounds, hasArms, singleArm));
            }
            if (visual.isEmpty()) {
                return false;
            }
            for (int i = 0; i < visual.size(); i++) {
                AxisAlignedBB box = visual.get(i);
                visual.set(i, AxisAlignedBB.getBoundingBox(
                        box.minX + x, box.minY + y, box.minZ + z,
                        box.maxX + x, box.maxY + y, box.maxZ + z));
            }
            out.add(OutlineShape.union(visual));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int countArms(List<AxisAlignedBB> boxes, int side) {
        int normalAxis = side <= 1 ? 1 : side <= 3 ? 2 : 0;
        int tangentA;
        int tangentB;
        if (normalAxis == 0) {
            tangentA = 1;
            tangentB = 2;
        } else if (normalAxis == 1) {
            tangentA = 0;
            tangentB = 2;
        } else {
            tangentA = 0;
            tangentB = 1;
        }
        int count = 0;
        for (AxisAlignedBB box : boxes) {
            double minA = tangentA == 0 ? box.minX : tangentA == 1 ? box.minY : box.minZ;
            double maxA = tangentA == 0 ? box.maxX : tangentA == 1 ? box.maxY : box.maxZ;
            double minB = tangentB == 0 ? box.minX : tangentB == 1 ? box.minY : box.minZ;
            double maxB = tangentB == 0 ? box.maxX : tangentB == 1 ? box.maxY : box.maxZ;
            if (minA <= 1.0E-4D || maxA >= 1.0D - 1.0E-4D
                    || minB <= 1.0E-4D || maxB >= 1.0D - 1.0E-4D) {
                count++;
            }
        }
        return count;
    }

    private static AxisAlignedBB shrinkWireBox(AxisAlignedBB box, int side, AxisAlignedBB model,
                                               boolean hasArms, boolean singleArm) {
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};
        int normalAxis = side <= 1 ? 1 : side <= 3 ? 2 : 0;
        int tangentA;
        int tangentB;
        if (normalAxis == 0) {
            tangentA = 1;
            tangentB = 2;
        } else if (normalAxis == 1) {
            tangentA = 0;
            tangentB = 2;
        } else {
            tangentA = 0;
            tangentB = 1;
        }

        // An arm that reaches a block edge runs along that axis. Keep the run length,
        // extend it to the block centre, and shrink the other tangent axis to the wire width.
        // Arms then overlap the central joint instead of leaving gaps in L/T shapes.
        boolean touchAMin = min[tangentA] <= 1.0E-4D;
        boolean touchAMax = max[tangentA] >= 1.0D - 1.0E-4D;
        boolean touchBMin = min[tangentB] <= 1.0E-4D;
        boolean touchBMax = max[tangentB] >= 1.0D - 1.0E-4D;
        boolean touchA = touchAMin || touchAMax;
        boolean touchB = touchBMin || touchBMax;
        if (touchA && !touchB) {
            extendRunAxis(min, max, tangentA);
            if (singleArm) {
                extendSingleArm(min, max, tangentA, touchAMin, touchAMax, model);
            }
            shrinkCrossAxis(min, max, tangentB);
        } else if (touchB && !touchA) {
            extendRunAxis(min, max, tangentB);
            if (singleArm) {
                extendSingleArm(min, max, tangentB, touchBMin, touchBMax, model);
            }
            shrinkCrossAxis(min, max, tangentA);
        } else {
            if (hasArms) {
                // Connected wire: only a small centre joint. The actual arms extend to the
                // centre along their real connection directions; do not invent a U-axis run.
                shrinkCrossAxis(min, max, tangentA);
                shrinkCrossAxis(min, max, tangentB);
            } else {
                // Standalone wire: keep the whole face-U run so both exposed ends are covered.
                int faceV = normalAxis == 0 ? tangentA : tangentB;
                shrinkCrossAxis(min, max, faceV);
            }
        }
        return AxisAlignedBB.getBoundingBox(min[0], min[1], min[2], max[0], max[1], max[2]);
    }

    private static void extendSingleArm(double[] min, double[] max, int axis,
                                        boolean touchedMin, boolean touchedMax, AxisAlignedBB model) {
        if (touchedMax) {
            double end = model == null ? 0.25D
                    : axis == 0 ? model.minX : axis == 1 ? model.minY : model.minZ;
            if (end >= 0.5D) {
                end = 0.25D;
            }
            min[axis] = Math.min(min[axis], end);
        }
        if (touchedMin) {
            double end = model == null ? 0.75D
                    : axis == 0 ? model.maxX : axis == 1 ? model.maxY : model.maxZ;
            if (end <= 0.5D) {
                end = 0.75D;
            }
            max[axis] = Math.max(max[axis], end);
        }
    }

    private static void extendRunAxis(double[] min, double[] max, int axis) {
        if (min[axis] > 0.5D) {
            min[axis] = 0.5D;
        }
        if (max[axis] < 0.5D) {
            max[axis] = 0.5D;
        }
    }

    private static void extendToModel(double[] min, double[] max, int axis,
                                      AxisAlignedBB model, boolean touchedMin, boolean touchedMax) {
        if (model != null) {
            double modelMin = axis == 0 ? model.minX : axis == 1 ? model.minY : model.minZ;
            double modelMax = axis == 0 ? model.maxX : axis == 1 ? model.maxY : model.maxZ;
            if (touchedMin) {
                min[axis] = Math.min(min[axis], modelMin);
            }
            if (touchedMax) {
                max[axis] = Math.max(max[axis], modelMax);
            }
        }
        // A connected wire continues into the neighbouring part/block. Add a one-pixel
        // end cap even when the render-model bounds are clipped to the block, otherwise
        // the last bit of the connection edge is never inside the selection box.
        if (touchedMin) {
            min[axis] -= END_PADDING;
        }
        if (touchedMax) {
            max[axis] += END_PADDING;
        }
    }

    private static AxisAlignedBB modelBounds(Object part) {
        if (RENDER_WIRE_INSTANCE == null || MODEL_KEY == null || GET_MODEL == null
                || MODEL_BOUNDS == null || CUBOID_TO_AABB == null) {
            return null;
        }
        try {
            Object key = MODEL_KEY.invoke(RENDER_WIRE_INSTANCE, part);
            Object model = GET_MODEL.invoke(RENDER_WIRE_INSTANCE, key);
            if (model == null) {
                return null;
            }
            Object cuboid = MODEL_BOUNDS.invoke(model);
            Object value = CUBOID_TO_AABB.invoke(cuboid);
            return value instanceof AxisAlignedBB ? (AxisAlignedBB) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void shrinkCrossAxis(double[] min, double[] max, int axis) {
        double center = (min[axis] + max[axis]) * 0.5D;
        double half = WIRE_WIDTH * 0.5D;
        min[axis] = center - half;
        max[axis] = center + half;
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

    private static Class<?> classOrNull(String name) {
        try {
            return Class.forName(name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object module(Class<?> owner) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getField("MODULE$").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Method methodOrNull(Class<?> owner, String name, Class<?>... args) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
