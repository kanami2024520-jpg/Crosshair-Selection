package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.util.ColorUtil;
import net.minecraft.util.AxisAlignedBB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Draws the sharp outer edges of a union of axis-aligned boxes.
 *
 * <p>Drawing every box separately produces overlapping internal lines (the L-shaped pipe case).
 * This renderer splits the union at the exact box face coordinates and only emits edges where
 * the surface normal changes, so coplanar seams and internal box boundaries disappear. Using the
 * real coordinates instead of a fixed 1/16 voxel grid keeps the margin slider continuous.</p>
 */
public final class UnionOutlineRenderer {

    private static final double EPSILON = 1.0E-7D;

    private static final int[][] NORMALS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] TANGENTS = {
            {1, 2}, {1, 2}, {0, 2}, {0, 2}, {0, 1}, {0, 1}
    };

    private UnionOutlineRenderer() {
    }

    public static void draw(List<AxisAlignedBB> boxes, double cameraX, double cameraY, double cameraZ,
                            int colour, float thickness, double margin, boolean flow,
                            long flowTime, float flowSpeed) {
        if (boxes == null || boxes.isEmpty()) {
            return;
        }
        drawFlow(boxes, cameraX, cameraY, cameraZ, colour, thickness,
                Math.max(0.0D, margin), flow, flowTime, flowSpeed);
    }

    private static void drawFlow(List<AxisAlignedBB> boxes, double cameraX, double cameraY, double cameraZ,
                                 int colour, float thickness, double margin, boolean rainbowFlow,
                                 long flowTime, float flowSpeed) {
        RenderHelper.setColor(colour);
        RenderHelper.bindWhiteTexture();

        List<AxisAlignedBB> expanded = new ArrayList<AxisAlignedBB>(boxes.size());
        for (AxisAlignedBB box : boxes) {
            expanded.add(BoxUtil.expandWithinBlock(box, margin));
        }

        if (expanded.size() == 1) {
            AxisAlignedBB box = expanded.get(0);
            if (rainbowFlow) {
                FlowRenderer.drawBox3D(box, cameraX, cameraY, cameraZ,
                        colour, flowTime, flowSpeed, 8, thickness);
            } else {
                ThickLineRenderer.drawBox3D(box, cameraX, cameraY, cameraZ, colour, thickness);
            }
            return;
        }

        double[] xs = coordinates(expanded, 0);
        double[] ys = coordinates(expanded, 1);
        double[] zs = coordinates(expanded, 2);
        int nx = xs.length - 1;
        int ny = ys.length - 1;
        int nz = zs.length - 1;
        if (nx < 1 || ny < 1 || nz < 1) {
            return;
        }

        Set<Long> cells = new HashSet<Long>();
        List<int[]> cellList = new ArrayList<int[]>();
        for (int ix = 0; ix < nx; ix++) {
            double cx = (xs[ix] + xs[ix + 1]) * 0.5D;
            for (int iy = 0; iy < ny; iy++) {
                double cy = (ys[iy] + ys[iy + 1]) * 0.5D;
                for (int iz = 0; iz < nz; iz++) {
                    double cz = (zs[iz] + zs[iz + 1]) * 0.5D;
                    if (isInside(expanded, cx, cy, cz)) {
                        cells.add(pack(ix, iy, iz));
                        cellList.add(new int[]{ix, iy, iz});
                    }
                }
            }
        }

        int alpha = ColorUtil.alpha(colour);
        for (int[] cell : cellList) {
            int ix = cell[0];
            int iy = cell[1];
            int iz = cell[2];
            for (int face = 0; face < 6; face++) {
                int[] normal = NORMALS[face];
                int nX = normal[0];
                int nY = normal[1];
                int nZ = normal[2];
                if (cells.contains(pack(ix + nX, iy + nY, iz + nZ))) {
                    continue;
                }
                int tangentA = TANGENTS[face][0];
                int tangentB = TANGENTS[face][1];
                addFlowFaceEdges(cells, xs, ys, zs, ix, iy, iz, nX, nY, nZ,
                        tangentA, tangentB, colour, alpha, rainbowFlow, thickness,
                        flowTime, flowSpeed, cameraX, cameraY, cameraZ);
            }
        }
    }

    private static double[] coordinates(List<AxisAlignedBB> boxes, int axis) {
        TreeSet<Double> values = new TreeSet<Double>();
        for (AxisAlignedBB box : boxes) {
            if (axis == 0) {
                values.add(box.minX);
                values.add(box.maxX);
            } else if (axis == 1) {
                values.add(box.minY);
                values.add(box.maxY);
            } else {
                values.add(box.minZ);
                values.add(box.maxZ);
            }
        }
        double[] result = new double[values.size()];
        int index = 0;
        for (Double value : values) {
            result[index++] = value;
        }
        return result;
    }

    private static boolean isInside(List<AxisAlignedBB> boxes, double x, double y, double z) {
        for (AxisAlignedBB box : boxes) {
            if (x >= box.minX - EPSILON && x <= box.maxX + EPSILON
                    && y >= box.minY - EPSILON && y <= box.maxY + EPSILON
                    && z >= box.minZ - EPSILON && z <= box.maxZ + EPSILON) {
                return true;
            }
        }
        return false;
    }

    private static void addFlowFaceEdges(Set<Long> cells, double[] xs, double[] ys, double[] zs,
                                         int ix, int iy, int iz, int nX, int nY, int nZ,
                                         int tangentA, int tangentB, int colour, int alpha,
                                         boolean rainbowFlow, float thickness,
                                         long flowTime, float flowSpeed,
                                         double cameraX, double cameraY, double cameraZ) {
        addFlowEdge(cells, xs, ys, zs, ix, iy, iz, nX, nY, nZ,
                tangentA, tangentB, colour, alpha, rainbowFlow, thickness, flowTime, flowSpeed,
                cameraX, cameraY, cameraZ);
        addFlowEdge(cells, xs, ys, zs, ix, iy, iz, nX, nY, nZ,
                tangentB, tangentA, colour, alpha, rainbowFlow, thickness, flowTime, flowSpeed,
                cameraX, cameraY, cameraZ);
    }

    private static void addFlowEdge(Set<Long> cells, double[] xs, double[] ys, double[] zs,
                                    int ix, int iy, int iz, int nX, int nY, int nZ,
                                    int alongAxis, int sideAxis, int colour, int alpha,
                                    boolean rainbowFlow, float thickness,
                                    long flowTime, float flowSpeed,
                                    double cameraX, double cameraY, double cameraZ) {
        for (int side = -1; side <= 1; side += 2) {
            int sx = sideAxis == 0 ? side : 0;
            int sy = sideAxis == 1 ? side : 0;
            int sz = sideAxis == 2 ? side : 0;
            boolean coplanarNeighbour = cells.contains(pack(ix + sx, iy + sy, iz + sz))
                    && !cells.contains(pack(ix + sx + nX, iy + sy + nY, iz + sz + nZ));
            if (coplanarNeighbour) {
                continue;
            }

            double x0 = xs[ix];
            double y0 = ys[iy];
            double z0 = zs[iz];
            double x1 = xs[ix + 1];
            double y1 = ys[iy + 1];
            double z1 = zs[iz + 1];

            // The face lies on the cell boundary in the normal direction.
            if (nX > 0) {
                x0 = x1 = xs[ix + 1];
            } else if (nX < 0) {
                x0 = x1 = xs[ix];
            } else if (nY > 0) {
                y0 = y1 = ys[iy + 1];
            } else if (nY < 0) {
                y0 = y1 = ys[iy];
            } else if (nZ > 0) {
                z0 = z1 = zs[iz + 1];
            } else {
                z0 = z1 = zs[iz];
            }

            // The fixed side of the edge in the other tangent axis.
            if (side < 0) {
                if (sideAxis == 0) {
                    x0 = x1 = xs[ix];
                } else if (sideAxis == 1) {
                    y0 = y1 = ys[iy];
                } else {
                    z0 = z1 = zs[iz];
                }
            } else {
                if (sideAxis == 0) {
                    x0 = x1 = xs[ix + 1];
                } else if (sideAxis == 1) {
                    y0 = y1 = ys[iy + 1];
                } else {
                    z0 = z1 = zs[iz + 1];
                }
            }

            // The edge spans the other cell boundary.
            if (alongAxis == 0) {
                x0 = xs[ix];
                x1 = xs[ix + 1];
            } else if (alongAxis == 1) {
                y0 = ys[iy];
                y1 = ys[iy + 1];
            } else {
                z0 = zs[iz];
                z1 = zs[iz + 1];
            }

            if (rainbowFlow) {
                int c = Rainbow.colorAt((x0 + x1) * 0.5D, (y0 + y1) * 0.5D, (z0 + z1) * 0.5D,
                        flowTime, flowSpeed, alpha);
                FlowRenderer.drawSegment3D(x0, y0, z0, x1, y1, z1,
                        cameraX, cameraY, cameraZ, c, thickness);
            } else {
                ThickLineRenderer.drawLine3D(x0, y0, z0, x1, y1, z1,
                        cameraX, cameraY, cameraZ, colour, thickness);
            }
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x + 8192) & 0x3FFF) << 28
                | ((long) (y + 8192) & 0x3FFF) << 14
                | ((long) (z + 8192) & 0x3FFF);
    }
}
