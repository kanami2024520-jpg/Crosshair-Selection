package me.kuionc.crosshairselection.render;

import net.minecraft.util.AxisAlignedBB;

/**
 * A shape the selection box can outline. Boxes keep the old behaviour; cylinders are used for
 * GregTech-style round pipes.
 */
public final class OutlineShape {

    public enum Kind {
        BOX,
        UNION,
        CYLINDER
    }

    private final Kind kind;
    private final AxisAlignedBB box;
    private final java.util.List<AxisAlignedBB> boxes;
    private final boolean fill;
    private final int axis;
    private final double axisMin;
    private final double axisMax;
    private final double center1;
    private final double center2;
    private final double radius;

    private OutlineShape(Kind kind, AxisAlignedBB box, java.util.List<AxisAlignedBB> boxes, boolean fill,
                         int axis, double axisMin, double axisMax, double center1, double center2,
                         double radius) {
        this.kind = kind;
        this.box = box;
        this.boxes = boxes;
        this.fill = fill;
        this.axis = axis;
        this.axisMin = axisMin;
        this.axisMax = axisMax;
        this.center1 = center1;
        this.center2 = center2;
        this.radius = radius;
    }

    public static OutlineShape box(AxisAlignedBB box, boolean fill) {
        return new OutlineShape(Kind.BOX, box, null, fill, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static OutlineShape union(java.util.List<AxisAlignedBB> boxes) {
        return new OutlineShape(Kind.UNION, null, boxes, false, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /**
     * @param axis   0 = X, 1 = Y, 2 = Z
     * @param min    minimum coordinate along the axis (world)
     * @param max    maximum coordinate along the axis (world)
     * @param c1     world coordinate of the first perpendicular axis
     * @param c2     world coordinate of the second perpendicular axis
     * @param radius radius in block units
     */
    public static OutlineShape cylinder(int axis, double min, double max, double c1, double c2, double radius) {
        return new OutlineShape(Kind.CYLINDER, null, null, false, axis, min, max, c1, c2, radius);
    }

    public Kind getKind() {
        return kind;
    }

    public AxisAlignedBB getBox() {
        return box;
    }

    public java.util.List<AxisAlignedBB> getBoxes() {
        return boxes;
    }

    public boolean isFill() {
        return fill;
    }

    public int getAxis() {
        return axis;
    }

    public double getAxisMin() {
        return axisMin;
    }

    public double getAxisMax() {
        return axisMax;
    }

    public double getCenter1() {
        return center1;
    }

    public double getCenter2() {
        return center2;
    }

    public double getRadius() {
        return radius;
    }
}
