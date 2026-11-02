package me.kuionc.crosshairselection.render;

/**
 * Shared 3x3 grid math for the GT overlay. The same cell boundaries are used for rendering,
 * X markers and the hit-vector remapping sent to GT.
 */
public final class GtGridMath {

    private GtGridMath() {
    }

    public static double half(double inset) {
        return 0.5D - Math.max(0.0D, Math.min(0.25D, inset));
    }

    public static double half(double inset, float rotation) {
        double base = half(inset);
        double radians = Math.toRadians(rotation);
        double extent = Math.abs(Math.cos(radians)) + Math.abs(Math.sin(radians));
        return base / Math.max(1.0D, extent);
    }

    public static int[] cellForHit(double u, double v, double inset, float rotation, float cellSize) {
        double half = half(inset, rotation);
        double edge = edgeWidth(half, cellSize);
        double gu = u - 0.5D;
        double gv = v - 0.5D;
        double radians = -Math.toRadians(rotation);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double ru = gu * cos - gv * sin;
        double rv = gu * sin + gv * cos;
        if (Math.abs(ru) > half || Math.abs(rv) > half) {
            return null;
        }
        int col = ru < -half + edge ? 0 : ru > half - edge ? 2 : 1;
        int row = rv < -half + edge ? 0 : rv > half - edge ? 2 : 1;
        return new int[]{col, row};
    }

    public static double[] cellCenter(int col, int row, double inset, float rotation, float cellSize) {
        double half = half(inset, rotation);
        double edge = edgeWidth(half, cellSize);
        double cu = col == 0 ? -half + edge / 2.0D : col == 2 ? half - edge / 2.0D : 0.0D;
        double cv = row == 0 ? -half + edge / 2.0D : row == 2 ? half - edge / 2.0D : 0.0D;
        double radians = Math.toRadians(rotation);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double ru = cu * cos + cv * sin;
        double rv = -cu * sin + cv * cos;
        return new double[]{0.5D + ru, 0.5D + rv};
    }

    public static double[] linePositions(double inset, float rotation, float cellSize) {
        double half = half(inset, rotation);
        double edge = edgeWidth(half, cellSize);
        return new double[]{-half, -half + edge, half - edge, half};
    }

    public static double[] cellBounds(int col, int row, double inset, float rotation, float cellSize) {
        double[] positions = linePositions(inset, rotation, cellSize);
        int c = Math.max(0, Math.min(2, col));
        int r = Math.max(0, Math.min(2, row));
        return new double[]{0.5D + positions[c], 0.5D + positions[c + 1],
                0.5D + positions[r], 0.5D + positions[r + 1]};
    }

    private static double edgeWidth(double half, float cellSize) {
        double edge = Math.max(0.05D, Math.min(0.45D, cellSize));
        return Math.min(edge, half * 0.8D);
    }

    /**
     * @return the face that GT will use for this cell, or -1 if the cell is invalid.
     */
    public static int sideForCell(int side, int col, int row) {
        if (col < 0 || col > 2 || row < 0 || row > 2) {
            return -1;
        }
        if (col == 1 && row == 1) {
            return side;
        }
        if (col != 1 && row != 1) {
            return side ^ 1;
        }
        if (col == 0) {
            return uMinus(side);
        }
        if (col == 2) {
            return uPlus(side);
        }
        if (row == 0) {
            return vMinus(side);
        }
        return vPlus(side);
    }

    /**
     * A canonical hit point on the original face that GT's determineWrenchingSide() maps to
     * the requested cell's logical side.
     */
    public static double[] canonicalHit(int col, int row) {
        if (col == 1 && row == 1) {
            return new double[]{0.5D, 0.5D};
        }
        double u = col == 0 ? 0.166D : col == 2 ? 0.834D : 0.5D;
        double v = row == 0 ? 0.166D : row == 2 ? 0.834D : 0.5D;
        return new double[]{u, v};
    }

    private static int uMinus(int side) {
        return side == 4 || side == 5 ? 2 : 4; // north for west/east faces, west otherwise
    }

    private static int uPlus(int side) {
        return side == 4 || side == 5 ? 3 : 5; // south for west/east faces, east otherwise
    }

    private static int vMinus(int side) {
        return side == 0 || side == 1 ? 2 : 0; // north for horizontal faces, down otherwise
    }

    private static int vPlus(int side) {
        return side == 0 || side == 1 ? 3 : 1; // south for horizontal faces, up otherwise
    }
}
