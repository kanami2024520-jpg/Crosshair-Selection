package me.kuionc.crosshairselection.render;

import com.gtnewhorizon.gtnhlib.client.renderer.TessellatorManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import org.lwjgl.opengl.GL11;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws the sharp outer edges of a union of axis-aligned boxes.
 *
 * <p>Drawing every box separately produces overlapping internal lines (the L-shaped pipe case).
 * This renderer voxelises the union and only emits edges where the surface normal changes, so
 * coplanar seams and internal box boundaries disappear.</p>
 */
public final class UnionOutlineRenderer {

    private static final double CELL = 1.0D / 16.0D;
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
                            int colour, float thickness, double margin) {
        if (boxes == null || boxes.isEmpty()) {
            return;
        }
        margin = Math.max(0.0D, margin);
        RenderHelper.setColor(colour);
        GL11.glLineWidth(Math.max(1.0F, thickness));
        Tessellator tessellator = TessellatorManager.get();
        tessellator.startDrawing(GL11.GL_LINES);

        if (boxes.size() == 1) {
            addBoxEdges(tessellator, boxes.get(0).expand(margin, margin, margin),
                    cameraX, cameraY, cameraZ);
            tessellator.draw();
            return;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        for (AxisAlignedBB box : boxes) {
            minX = Math.min(minX, box.minX - margin);
            minY = Math.min(minY, box.minY - margin);
            minZ = Math.min(minZ, box.minZ - margin);
        }
        int originX = (int) Math.floor(minX / CELL);
        int originY = (int) Math.floor(minY / CELL);
        int originZ = (int) Math.floor(minZ / CELL);

        Set<Long> cells = new HashSet<Long>();
        List<int[]> cellList = new java.util.ArrayList<int[]>();
        for (AxisAlignedBB box : boxes) {
            int ix0 = (int) Math.floor((box.minX - margin) / CELL);
            int iy0 = (int) Math.floor((box.minY - margin) / CELL);
            int iz0 = (int) Math.floor((box.minZ - margin) / CELL);
            int ix1 = (int) Math.floor((box.maxX + margin - EPSILON) / CELL);
            int iy1 = (int) Math.floor((box.maxY + margin - EPSILON) / CELL);
            int iz1 = (int) Math.floor((box.maxZ + margin - EPSILON) / CELL);
            for (int ix = ix0; ix <= ix1; ix++) {
                for (int iy = iy0; iy <= iy1; iy++) {
                    for (int iz = iz0; iz <= iz1; iz++) {
                        int rx = ix - originX;
                        int ry = iy - originY;
                        int rz = iz - originZ;
                        if (cells.add(pack(rx, ry, rz))) {
                            cellList.add(new int[]{rx, ry, rz});
                        }
                    }
                }
            }
        }

        for (int[] cell : cellList) {
            int rx = cell[0];
            int ry = cell[1];
            int rz = cell[2];
            int ix = originX + rx;
            int iy = originY + ry;
            int iz = originZ + rz;
            for (int face = 0; face < 6; face++) {
                int[] normal = NORMALS[face];
                int nx = normal[0];
                int ny = normal[1];
                int nz = normal[2];
                if (cells.contains(pack(rx + nx, ry + ny, rz + nz))) {
                    continue;
                }
                int tangentA = TANGENTS[face][0];
                int tangentB = TANGENTS[face][1];
                addFaceEdges(tessellator, cells, rx, ry, rz, ix, iy, iz,
                        nx, ny, nz, tangentA, tangentB, cameraX, cameraY, cameraZ);
            }
        }

        tessellator.draw();
    }

    private static void addFaceEdges(Tessellator tessellator, Set<Long> cells,
                                     int rx, int ry, int rz, int ix, int iy, int iz,
                                     int nx, int ny, int nz, int tangentA, int tangentB,
                                     double cameraX, double cameraY, double cameraZ) {
        // Edge runs along tangentA, the side is chosen by tangentB (and vice versa).
        addEdges(tessellator, cells, rx, ry, rz, ix, iy, iz, nx, ny, nz,
                tangentA, tangentB, cameraX, cameraY, cameraZ);
        addEdges(tessellator, cells, rx, ry, rz, ix, iy, iz, nx, ny, nz,
                tangentB, tangentA, cameraX, cameraY, cameraZ);
    }

    private static void addEdges(Tessellator tessellator, Set<Long> cells,
                                 int rx, int ry, int rz, int ix, int iy, int iz,
                                 int nx, int ny, int nz, int alongAxis, int sideAxis,
                                 double cameraX, double cameraY, double cameraZ) {
        for (int side = -1; side <= 1; side += 2) {
            int sx = sideAxis == 0 ? side : 0;
            int sy = sideAxis == 1 ? side : 0;
            int sz = sideAxis == 2 ? side : 0;
            boolean coplanarNeighbour = cells.contains(pack(rx + sx, ry + sy, rz + sz))
                    && !cells.contains(pack(rx + sx + nx, ry + sy + ny, rz + sz + nz));
            if (coplanarNeighbour) {
                continue;
            }

            double x0 = ix * CELL;
            double y0 = iy * CELL;
            double z0 = iz * CELL;
            double x1 = x0 + CELL;
            double y1 = y0 + CELL;
            double z1 = z0 + CELL;

            // The face lies on the cell boundary in the normal direction.
            if (nx > 0) {
                x0 = x1 = ix * CELL + CELL;
            } else if (nx < 0) {
                x0 = x1 = ix * CELL;
            } else if (ny > 0) {
                y0 = y1 = iy * CELL + CELL;
            } else if (ny < 0) {
                y0 = y1 = iy * CELL;
            } else if (nz > 0) {
                z0 = z1 = iz * CELL + CELL;
            } else {
                z0 = z1 = iz * CELL;
            }

            // The fixed side of the edge in the other tangent axis.
            if (side < 0) {
                if (sideAxis == 0) {
                    x0 = x1 = ix * CELL;
                } else if (sideAxis == 1) {
                    y0 = y1 = iy * CELL;
                } else {
                    z0 = z1 = iz * CELL;
                }
            } else {
                if (sideAxis == 0) {
                    x0 = x1 = ix * CELL + CELL;
                } else if (sideAxis == 1) {
                    y0 = y1 = iy * CELL + CELL;
                } else {
                    z0 = z1 = iz * CELL + CELL;
                }
            }

            // The edge spans the other cell boundary.
            if (alongAxis == 0) {
                x0 = ix * CELL;
                x1 = x0 + CELL;
            } else if (alongAxis == 1) {
                y0 = iy * CELL;
                y1 = y0 + CELL;
            } else {
                z0 = iz * CELL;
                z1 = z0 + CELL;
            }

            tessellator.addVertex(x0 - cameraX, y0 - cameraY, z0 - cameraZ);
            tessellator.addVertex(x1 - cameraX, y1 - cameraY, z1 - cameraZ);
        }
    }

    private static void addBoxEdges(Tessellator tessellator, AxisAlignedBB box,
                                    double cameraX, double cameraY, double cameraZ) {
        double minX = box.minX - cameraX;
        double minY = box.minY - cameraY;
        double minZ = box.minZ - cameraZ;
        double maxX = box.maxX - cameraX;
        double maxY = box.maxY - cameraY;
        double maxZ = box.maxZ - cameraZ;
        line(tessellator, minX, minY, minZ, maxX, minY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, minY, maxZ);
        line(tessellator, maxX, minY, maxZ, minX, minY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, minY, minZ);
        line(tessellator, minX, maxY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, maxY, minZ, maxX, maxY, maxZ);
        line(tessellator, maxX, maxY, maxZ, minX, maxY, maxZ);
        line(tessellator, minX, maxY, maxZ, minX, maxY, minZ);
        line(tessellator, minX, minY, minZ, minX, maxY, minZ);
        line(tessellator, maxX, minY, minZ, maxX, maxY, minZ);
        line(tessellator, maxX, minY, maxZ, maxX, maxY, maxZ);
        line(tessellator, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private static void line(Tessellator tessellator, double x1, double y1, double z1,
                             double x2, double y2, double z2) {
        tessellator.addVertex(x1, y1, z1);
        tessellator.addVertex(x2, y2, z2);
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x + 8192) & 0x3FFF) << 28
                | ((long) (y + 8192) & 0x3FFF) << 14
                | ((long) (z + 8192) & 0x3FFF);
    }
}
