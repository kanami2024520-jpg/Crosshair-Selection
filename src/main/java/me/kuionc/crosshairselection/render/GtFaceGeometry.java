package me.kuionc.crosshairselection.render;

import me.kuionc.crosshairselection.config.GtOverlayConfig;
import me.kuionc.crosshairselection.config.GtXStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * The 3x3 grid and the input marker, expressed in face coordinates (u, v in 0..1).
 *
 * <p>Both the world overlay and the settings preview build their lines from here, so the preview
 * cannot drift from what is drawn in game. The grid line positions come from
 * {@link GtGridMath#linePositions}, which is also what the click remapping uses.
 */
public final class GtFaceGeometry {

    /** One straight line on the face. */
    public static final class Segment {

        public final double u1;
        public final double v1;
        public final double u2;
        public final double v2;
        /** Line width multiplier, used to keep the marker slightly heavier than the grid. */
        public final float weight;

        Segment(double u1, double v1, double u2, double v2, float weight) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
            this.weight = weight;
        }
    }

    /** Filled cell, only used by the {@link GtXStyle#FILL} marker. */
    public static final class Cell {

        public final double u1;
        public final double v1;
        public final double u2;
        public final double v2;

        Cell(double u1, double v1, double u2, double v2) {
            this.u1 = u1;
            this.v1 = v1;
            this.u2 = u2;
            this.v2 = v2;
        }
    }

    private GtFaceGeometry() {
    }

    /** The four grid lines: at -half, the two inner positions and +half, relative to the face centre. */
    public static List<Segment> gridLines(double inset, float cellSize) {
        List<Segment> lines = new ArrayList<Segment>(4);
        double half = GtGridMath.half(inset, 0.0F);
        for (double position : GtGridMath.linePositions(inset, 0.0F, cellSize)) {
            double u = 0.5D + position;
            double v = 0.5D + position;
            lines.add(new Segment(u, 0.5D - half, u, 0.5D + half, 1.0F));
            lines.add(new Segment(0.5D - half, v, 0.5D + half, v, 1.0F));
        }
        return lines;
    }

    /**
     * Marker lines for the given cell in the configured style.
     *
     * @return lines, or an empty list when the style is {@link GtXStyle#FILL}
     */
    public static List<Segment> markerLines(int col, int row) {
        return markerLines(col, row, GtOverlayConfig.gridInset, GtOverlayConfig.gridCellSize,
                GtOverlayConfig.xStyle);
    }

    public static List<Segment> markerLines(int col, int row, double inset, float cellSize, GtXStyle style) {
        List<Segment> lines = new ArrayList<Segment>(3);
        if (style == GtXStyle.FILL) {
            return lines;
        }
        double half = GtGridMath.half(inset, 0.0F);
        if (style == GtXStyle.FULL_X) {
            double[] bounds = GtGridMath.cellBounds(col, row, inset, 0.0F, cellSize);
            double u0 = bounds[0];
            double u1 = bounds[1];
            double v0 = bounds[2];
            double v1 = bounds[3];
            lines.add(new Segment(u0, v0, u1, v1, 1.0F));
            lines.add(new Segment(u0, v1, u1, v0, 1.0F));
            return lines;
        }

        double[] center = GtGridMath.cellCenter(col, row, inset, 0.0F, cellSize);
        double cu = center[0];
        double cv = center[1];
        double edge = Math.min(Math.max(0.05D, Math.min(0.45D, cellSize)), half * 0.8D);
        double size = Math.min(0.12D, Math.min(edge, half - edge) * 0.45D);

        if (style == GtXStyle.ARROW) {
            double du = col == 0 ? -1.0D : col == 2 ? 1.0D : 0.0D;
            double dv = row == 0 ? -1.0D : row == 2 ? 1.0D : 0.0D;
            if (du == 0.0D && dv == 0.0D) {
                // The centre cell has no outward direction; a dot reads better than an arrow that
                // would otherwise have to point somewhere arbitrary.
                double r = size * 0.45D;
                lines.add(new Segment(cu - r, cv, cu + r, cv, 1.0F));
                lines.add(new Segment(cu, cv - r, cu, cv + r, 1.0F));
                return lines;
            }
            double len = Math.sqrt(du * du + dv * dv);
            du /= len;
            dv /= len;
            double tipU = cu + du * size;
            double tipV = cv + dv * size;
            double baseU = cu - du * size * 0.55D;
            double baseV = cv - dv * size * 0.55D;
            double sideU = -dv * size * 0.7D;
            double sideV = du * size * 0.7D;
            lines.add(new Segment(baseU + sideU, baseV + sideV, tipU, tipV, 1.0F));
            lines.add(new Segment(baseU - sideU, baseV - sideV, tipU, tipV, 1.0F));
            return lines;
        }

        // CROSS: the default X marker, two diagonals through the cell centre.
        lines.add(new Segment(cu - size, cv - size, cu + size, cv + size, 1.15F));
        lines.add(new Segment(cu - size, cv + size, cu + size, cv - size, 1.15F));
        return lines;
    }

    /** The filled cell for {@link GtXStyle#FILL}, or null for every other style. */
    public static Cell markerFill(int col, int row) {
        if (GtOverlayConfig.xStyle != GtXStyle.FILL) {
            return null;
        }
        double[] bounds = GtGridMath.cellBounds(col, row, GtOverlayConfig.gridInset,
                0.0F, GtOverlayConfig.gridCellSize);
        return new Cell(bounds[0], bounds[2], bounds[1], bounds[3]);
    }

    /** Marker colour uses the grid colour with a fixed alpha, matching the world overlay. */
    public static int markerFillColour(int colour) {
        return (colour & 0x00FFFFFF) | 0x66000000;
    }
}
