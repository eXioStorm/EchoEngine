package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.github.exiostorm.utils.msdf.EdgeSegment.*;

/**
 * Vector shape representation.
 */
public class MsdfShape {

    // Threshold of the dot product of adjacent edge directions to be considered convergent.
    public static final double CORNER_DOT_EPSILON = 0.000001;

    private static final double DECONVERGE_OVERSHOOT = 1.11111111111111111; // moves control points slightly more than necessary to account for floating-point errors

    public static class Bounds {
        public double l, b, r, t;

        public Bounds() {}

        public Bounds(double l, double b, double r, double t) {
            this.l = l;
            this.b = b;
            this.r = r;
            this.t = t;
        }
    }

    private static class Intersection {
        double x;
        int direction;
        int contourIndex;

        static final Comparator<Intersection> COMPARATOR = (a, b) -> Double.compare(a.x, b.x);
    }

    /** The list of contours the shape consists of. */
    public List<Contours.Contour> contours;

    /** Specifies whether the shape uses bottom-to-top (false) or top-to-bottom (true) Y coordinates. */
    //public boolean inverseYAxis;

    public MsdfShape() {
        this.contours = new ArrayList<>();
        //this.inverseYAxis = false;
    }

    /** Adds a contour. */
    public void addContour(Contours.Contour contour) {
        contours.add(contour);
    }

    /** Adds a blank contour and returns its reference. */
    public Contours.Contour addContour() {
        Contours.Contour newContour = new Contours.Contour();
        contours.add(newContour);
        return newContour;
    }

    /** Performs basic checks to determine if the object represents a valid shape. */
    public boolean validate() {
        for (Contours.Contour contour : contours) {
            if (!contour.edges.isEmpty()) {
                Vector2d corner = contour.edges.get(contour.edges.size() - 1).edge.point(1);
                for (EdgeHolder edge : contour.edges) {
                    if (edge == null || edge.edge == null)
                        return false;
                    if (!edge.edge.point(0).equals(corner, 1e-10))
                        return false;
                    corner = edge.edge.point(1);
                }
            }
        }
        return true;
    }

    private static void deconvergeEdge(EdgeHolder edgeHolder, int param, Vector2d vector) {
        EdgeSegment edge = edgeHolder.edge;
        switch (edge.EDGE_TYPE) {
            case QUADRATIC:
                edgeHolder.setEdgeSegment(((QuadraticSegment) edge).convertToCubic());
                edge = edgeHolder.edge;
                // fallthrough
            case CUBIC:
                CubicSegment cubic = (CubicSegment) edge;
                Vector2d[] p = cubic.controlPoints();
                switch (param) {
                    case 0:
                        Vector2d diff0 = new Vector2d(p[1]).sub(p[0]);
                        p[1].add(new Vector2d(vector).mul(diff0.length()));
                        break;
                    case 1:
                        Vector2d diff1 = new Vector2d(p[2]).sub(p[3]);
                        p[2].add(new Vector2d(vector).mul(diff1.length()));
                        break;
                }
                break;
        }
    }

    /** Normalizes the shape geometry for distance field generation. */
    public void normalize() {
        for (Contours.Contour contour : contours) {
            if (contour.edges.size() == 1) {
                EdgeSegment[] parts = new EdgeSegment[3];
                contour.edges.get(0).edge.splitInThirds(parts);
                contour.edges.clear();
                contour.edges.add(new EdgeHolder(parts[0]));
                contour.edges.add(new EdgeHolder(parts[1]));
                contour.edges.add(new EdgeHolder(parts[2]));
            } else {
                // Push apart convergent edge segments
                EdgeHolder prevEdge = contour.edges.get(contour.edges.size() - 1);
                for (EdgeHolder edge : contour.edges) {
                    Vector2d prevDir = prevEdge.edge.direction(1).normalize();
                    Vector2d curDir = edge.edge.direction(0).normalize();

                    if (prevDir.dot(curDir) < CORNER_DOT_EPSILON - 1) {
                        double factor = DECONVERGE_OVERSHOOT * Math.sqrt(1 - (CORNER_DOT_EPSILON - 1) * (CORNER_DOT_EPSILON - 1)) / (CORNER_DOT_EPSILON - 1);
                        Vector2d axis = new Vector2d(curDir).sub(prevDir).normalize().mul(factor);

                        if (ConvergentCurveOrdering.convergentCurveOrdering(prevEdge, edge) < 0)
                            axis.negate();

                        deconvergeEdge(prevEdge, 1, getOrthogonal(axis, true));
                        deconvergeEdge(edge, 0, getOrthogonal(axis, false));
                    }
                    prevEdge = edge;
                }
            }
        }
    }

    /** Adjusts the bounding box to fit the shape. */
    public void bound(Rectangle2D.Double bounds) {
        for (Contours.Contour contour : contours) {
            contour.bound(bounds);
        }
    }

    /** Adjusts the bounding box to fit the shape border's mitered corners. */
    public void boundMiters(Rectangle2D.Double bounds, double border, double miterLimit, int polarity) {
        for (Contours.Contour contour : contours) {
            contour.boundMiters(bounds, border, miterLimit, polarity);
        }
    }

    /** Computes the minimum bounding box that fits the shape, optionally with a (mitered) border. */
    public Bounds getBounds(double border, double miterLimit, int polarity) {
        final double LARGE_VALUE = 1e240;
        Rectangle2D.Double bounds = new Rectangle2D.Double(LARGE_VALUE, LARGE_VALUE, -2*LARGE_VALUE, -2*LARGE_VALUE);

        bound(bounds);

        if (border > 0) {
            bounds.x -= border;
            bounds.y -= border;
            bounds.width += 2 * border;
            bounds.height += 2 * border;

            if (miterLimit > 0) {
                boundMiters(bounds, border, miterLimit, polarity);
            }
        }

        return new Bounds(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height);
    }

    public Bounds getBounds() {
        return getBounds(0, 0, 0);
    }

    /** Outputs the scanline that intersects the shape at y. */
    public void scanline(Scanline line, double y) {
        List<Scanline.Intersection> intersections = new ArrayList<>();
        double[] x = new double[3];
        int[] dy = new int[3];

        for (Contours.Contour contour : contours) {
            for (EdgeHolder edge : contour.edges) {
                int n = edge.edge.scanlineIntersections(x, dy, y);
                for (int i = 0; i < n; i++) {
                    intersections.add(new Scanline.Intersection(x[i], dy[i]));
                }
            }
        }

        line.setIntersections(intersections);
    }

    /** Returns the total number of edge segments */
    public int edgeCount() {
        int total = 0;
        for (Contours.Contour contour : contours) {
            total += contour.edges.size();
        }
        return total;
    }

    /** Assumes its contours are unoriented (even-odd fill rule). Attempts to orient them to conform to the non-zero winding rule. */
    public void orientContours() {
        final double ratio = 0.5 * (Math.sqrt(5) - 1); // an irrational number to minimize chance of intersecting a corner or other point of interest
        int[] orientations = new int[contours.size()];
        List<Intersection> intersections = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {
            if (orientations[i] == 0 && !contours.get(i).edges.isEmpty()) {
                // Find a Y that crosses the contour
                double y0 = contours.get(i).edges.get(0).edge.point(0).y;
                double y1 = y0;

                for (EdgeHolder edge : contours.get(i).edges) {
                    if (y0 != y1) break;
                    y1 = edge.edge.point(1).y;
                }

                for (EdgeHolder edge : contours.get(i).edges) {
                    if (y0 != y1) break;
                    y1 = edge.edge.point(ratio).y; // in case all endpoints are in a horizontal line
                }

                double y = mix(y0, y1, ratio);

                // Scanline through whole shape at Y
                double[] x = new double[3];
                int[] dy = new int[3];

                for (int j = 0; j < contours.size(); j++) {
                    for (EdgeHolder edge : contours.get(j).edges) {
                        int n = edge.edge.scanlineIntersections(x, dy, y);
                        for (int k = 0; k < n; k++) {
                            Intersection intersection = new Intersection();
                            intersection.x = x[k];
                            intersection.direction = dy[k];
                            intersection.contourIndex = j;
                            intersections.add(intersection);
                        }
                    }
                }

                if (!intersections.isEmpty()) {
                    Collections.sort(intersections, Intersection.COMPARATOR);

                    // Disqualify multiple intersections
                    for (int j = 1; j < intersections.size(); j++) {
                        if (intersections.get(j).x == intersections.get(j-1).x) {
                            intersections.get(j).direction = 0;
                            intersections.get(j-1).direction = 0;
                        }
                    }

                    // Inspect scanline and deduce orientations of intersected contours
                    for (int j = 0; j < intersections.size(); j++) {
                        if (intersections.get(j).direction != 0) {
                            orientations[intersections.get(j).contourIndex] +=
                                    2 * ((j & 1) ^ (intersections.get(j).direction > 0 ? 1 : 0)) - 1;
                        }
                    }
                    intersections.clear();
                }
            }
        }

        // Reverse contours that have the opposite orientation
        for (int i = 0; i < contours.size(); i++) {
            if (orientations[i] < 0) {
                contours.get(i).reverse();
            }
        }
    }

    // Helper method for linear interpolation
    private static double mix(double a, double b, double t) {
        return a + t * (b - a);
    }

    // Helper method to get orthogonal vector
    private static Vector2d getOrthogonal(Vector2d v, boolean polarity) {
        return polarity ? new Vector2d(-v.y, v.x) : new Vector2d(v.y, -v.x);
    }

    // Helper method for sign function
    private static int sign(double value) {
        return value > 0 ? 1 : (value < 0 ? -1 : 0);
    }
}