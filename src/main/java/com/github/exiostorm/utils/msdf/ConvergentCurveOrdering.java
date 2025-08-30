package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

import java.util.Arrays;

/**
 * For curves a, b converging at P = a.point(1) = b.point(0) with the same (opposite) direction,
 * determines the relative ordering in which they exit P (i.e. whether a is to the left or right of b
 * at the smallest positive radius around P)
 */
public class ConvergentCurveOrdering {

    /*
     * For non-degenerate curves A(t), B(t) (ones where all control points are distinct) both originating at P = A(0) = B(0) = *corner,
     * we are computing the limit of
     *
     *     sign(crossProduct( A(t / |A'(0)|) - P, B(t / |B'(0)|) - P ))
     *
     * for t -> 0 from 1. Of note is that the curves' parameter has to be normed by the first derivative at P,
     * which ensures that the limit approaches P at the same rate along both curves - omitting this was the main error of earlier versions of deconverge.
     *
     * For degenerate cubic curves (ones where the first control point equals the origin point), the denominator |A'(0)| is zero,
     * so to address that, we approach with the square root of t and use the derivative of A(sqrt(t)), which at t = 0 equals A''(0)/2
     * Therefore, in these cases, we replace one factor of the cross product with A(sqrt(2*t / |A''(0)|)) - P
     *
     * The cross product results in a polynomial (in respect to t or t^2 in the degenerate case),
     * the limit of sign of which at zero can be determined by the lowest order non-zero derivative,
     * which equals to the sign of the first non-zero polynomial coefficient in the order of increasing exponents.
     *
     * The polynomial's constant and linear terms are zero, so the first derivative is definitely zero as well.
     * The second derivative is assumed to be zero (or near zero) due to the curves being convergent - this is an input requirement
     * (otherwise the correct result is the sign of the cross product of their directions at t = 0).
     * Therefore, we skip the first and second derivatives.
     */

    private static void simplifyDegenerateCurve(Vector2d[] controlPoints, int[] order) {
        if (order[0] == 3 &&
                (controlPoints[1].equals(controlPoints[0]) || controlPoints[1].equals(controlPoints[3])) &&
                (controlPoints[2].equals(controlPoints[0]) || controlPoints[2].equals(controlPoints[3]))) {
            controlPoints[1].set(controlPoints[3]);
            order[0] = 1;
        }
        if (order[0] == 2 &&
                (controlPoints[1].equals(controlPoints[0]) || controlPoints[1].equals(controlPoints[2]))) {
            controlPoints[1].set(controlPoints[2]);
            order[0] = 1;
        }
        if (order[0] == 1 && controlPoints[0].equals(controlPoints[1]))
            order[0] = 0;
    }

    public static int convergentCurveOrdering(Vector2d corner, int controlPointsBefore, int controlPointsAfter,
                                              Vector2d[] pointsBefore, Vector2d[] pointsAfter) {
        if (!(controlPointsBefore > 0 && controlPointsAfter > 0))
            return 0;

        Vector2d a1 = new Vector2d(), a2 = new Vector2d(), a3 = new Vector2d();
        Vector2d b1 = new Vector2d(), b2 = new Vector2d(), b3 = new Vector2d();

        a1.set(pointsBefore[0]).sub(corner);
        b1.set(pointsAfter[0]).sub(corner);

        if (controlPointsBefore >= 2)
            a2.set(pointsBefore[1]).sub(pointsBefore[0]).sub(a1);
        if (controlPointsAfter >= 2)
            b2.set(pointsAfter[1]).sub(pointsAfter[0]).sub(b1);

        if (controlPointsBefore >= 3) {
            a3.set(pointsBefore[2]).sub(pointsBefore[1]).sub(new Vector2d(pointsBefore[1]).sub(pointsBefore[0])).sub(a2);
            a2.mul(3);
        }
        if (controlPointsAfter >= 3) {
            b3.set(pointsAfter[2]).sub(pointsAfter[1]).sub(new Vector2d(pointsAfter[1]).sub(pointsAfter[0])).sub(b2);
            b2.mul(3);
        }

        a1.mul(controlPointsBefore);
        b1.mul(controlPointsAfter);

        // Non-degenerate case
        if (a1.lengthSquared() > 1e-20 && b1.lengthSquared() > 1e-20) {
            double as = a1.length();
            double bs = b1.length();

            // Third derivative
            double d = as * crossProduct(a1, b2) + bs * crossProduct(a2, b1);
            if (Math.abs(d) > 1e-15)
                return sign(d);

            // Fourth derivative
            d = as * as * crossProduct(a1, b3) + as * bs * crossProduct(a2, b2) + bs * bs * crossProduct(a3, b1);
            if (Math.abs(d) > 1e-15)
                return sign(d);

            // Fifth derivative
            d = as * crossProduct(a2, b3) + bs * crossProduct(a3, b2);
            if (Math.abs(d) > 1e-15)
                return sign(d);

            // Sixth derivative
            return sign(crossProduct(a3, b3));
        }

        // Degenerate curve after corner (control point after corner equals corner)
        int s = 1;
        if (a1.lengthSquared() > 1e-20) { // !b1
            // Swap aN <-> bN and handle in if (b1)
            Vector2d temp = new Vector2d(b1);
            b1.set(a1);
            a1.set(temp);

            temp.set(a2); a2.set(b2); b2.set(temp);
            temp.set(a3); a3.set(b3); b3.set(temp);
            s = -1; // make sure to also flip output
        }

        // Degenerate curve before corner (control point before corner equals corner)
        if (b1.lengthSquared() > 1e-20) { // !a1
            // Two-and-a-half-th derivative
            double d = crossProduct(a3, b1);
            if (Math.abs(d) > 1e-15)
                return s * sign(d);

            // Third derivative
            d = crossProduct(a2, b2);
            if (Math.abs(d) > 1e-15)
                return s * sign(d);

            // Three-and-a-half-th derivative
            d = crossProduct(a3, b2);
            if (Math.abs(d) > 1e-15)
                return s * sign(d);

            // Fourth derivative
            d = crossProduct(a2, b3);
            if (Math.abs(d) > 1e-15)
                return s * sign(d);

            // Four-and-a-half-th derivative
            return s * sign(crossProduct(a3, b3));
        }

        // Degenerate curves on both sides of the corner (control point before and after corner equals corner)
        { // !a1 && !b1
            // Two-and-a-half-th derivative
            double d = Math.sqrt(a2.length()) * crossProduct(a2, b3) + Math.sqrt(b2.length()) * crossProduct(a3, b2);
            if (Math.abs(d) > 1e-15)
                return sign(d);

            // Third derivative
            return sign(crossProduct(a3, b3));
        }
    }

    public static int convergentCurveOrdering(EdgeHolder a, EdgeHolder b) {
        Vector2d[] controlPoints = new Vector2d[12];
        for (int i = 0; i < 12; i++) {
            controlPoints[i] = new Vector2d();
        }

        Vector2d corner = controlPoints[4];
        Vector2d[] aCpTmp = new Vector2d[4];
        for (int i = 0; i < 4; i++) {
            aCpTmp[i] = controlPoints[8 + i];
        }

        EdgeSegment aEdge = a.edge;
        EdgeSegment bEdge = b.edge;

        int aOrder = aEdge.EDGE_TYPE;
        int bOrder = bEdge.EDGE_TYPE;

        if (!(aOrder >= 1 && aOrder <= 3 && bOrder >= 1 && bOrder <= 3)) {
            // Not implemented - only linear, quadratic, and cubic curves supported
            return 0;
        }

        Vector2d[] aControlPoints = aEdge.controlPoints();
        Vector2d[] bControlPoints = bEdge.controlPoints();

        for (int i = 0; i <= aOrder; i++)
            aCpTmp[i].set(aControlPoints[i]);
        for (int i = 0; i <= bOrder; i++)
            controlPoints[4 + i].set(bControlPoints[i]);

        if (!aCpTmp[aOrder].equals(corner, 1e-10))
            return 0;

        int[] aOrderRef = {aOrder};
        int[] bOrderRef = {bOrder};
        simplifyDegenerateCurve(aCpTmp, aOrderRef);
        simplifyDegenerateCurve(Arrays.copyOfRange(controlPoints, 4, 8), bOrderRef);
        aOrder = aOrderRef[0];
        bOrder = bOrderRef[0];

        for (int i = 0; i < aOrder; i++)
            controlPoints[4 - aOrder + i].set(aCpTmp[i]);

        Vector2d[] pointsBefore = Arrays.copyOfRange(controlPoints, 4 - aOrder, 4);
        Vector2d[] pointsAfter = Arrays.copyOfRange(controlPoints, 5, 5 + bOrder);

        return convergentCurveOrdering(corner, aOrder, bOrder, pointsBefore, pointsAfter);
    }

    // Helper method for cross product in 2D
    private static double crossProduct(Vector2d a, Vector2d b) {
        return a.x * b.y - a.y * b.x;
    }

    // Helper method for sign function
    private static int sign(double value) {
        return value > 0 ? 1 : (value < 0 ? -1 : 0);
    }
}