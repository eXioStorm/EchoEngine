package com.github.exiostorm.utils.msdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Fill rule dictates how intersection total is interpreted during rasterization.
 */
enum FillRule {
    FILL_NONZERO,
    FILL_ODD, // "even-odd"
    FILL_POSITIVE,
    FILL_NEGATIVE
}

/**
 * Represents a horizontal scanline intersecting a shape.
 */
public class Scanline {

    /**
     * An intersection with the scanline.
     */
    public static class Intersection {
        /** X coordinate. */
        public double x;
        /** Normalized Y direction of the oriented edge at the point of intersection. */
        public int direction;

        public Intersection() {}

        public Intersection(double x, int direction) {
            this.x = x;
            this.direction = direction;
        }

        public static final Comparator<Intersection> COMPARATOR = (a, b) -> Double.compare(a.x, b.x);
    }

    private List<Intersection> intersections;
    private int lastIndex;

    public Scanline() {
        this.intersections = new ArrayList<>();
        this.lastIndex = 0;
    }

    /**
     * Resolves the number of intersection into a binary fill value based on fill rule.
     */
    public static boolean interpretFillRule(int intersections, FillRule fillRule) {
        switch (fillRule) {
            case FILL_NONZERO:
                return intersections != 0;
            case FILL_ODD:
                return (intersections & 1) != 0;
            case FILL_POSITIVE:
                return intersections > 0;
            case FILL_NEGATIVE:
                return intersections < 0;
            default:
                return false;
        }
    }

    public static double overlap(Scanline a, Scanline b, double xFrom, double xTo, FillRule fillRule) {
        double total = 0;
        boolean aInside = false, bInside = false;
        int ai = 0, bi = 0;
        double ax = !a.intersections.isEmpty() ? a.intersections.get(ai).x : xTo;
        double bx = !b.intersections.isEmpty() ? b.intersections.get(bi).x : xTo;

        while (ax < xFrom || bx < xFrom) {
            double xNext = Math.min(ax, bx);
            if (ax == xNext && ai < a.intersections.size()) {
                aInside = interpretFillRule(a.intersections.get(ai).direction, fillRule);
                ax = ++ai < a.intersections.size() ? a.intersections.get(ai).x : xTo;
            }
            if (bx == xNext && bi < b.intersections.size()) {
                bInside = interpretFillRule(b.intersections.get(bi).direction, fillRule);
                bx = ++bi < b.intersections.size() ? b.intersections.get(bi).x : xTo;
            }
        }

        double x = xFrom;
        while (ax < xTo || bx < xTo) {
            double xNext = Math.min(ax, bx);
            if (aInside == bInside)
                total += xNext - x;
            if (ax == xNext && ai < a.intersections.size()) {
                aInside = interpretFillRule(a.intersections.get(ai).direction, fillRule);
                ax = ++ai < a.intersections.size() ? a.intersections.get(ai).x : xTo;
            }
            if (bx == xNext && bi < b.intersections.size()) {
                bInside = interpretFillRule(b.intersections.get(bi).direction, fillRule);
                bx = ++bi < b.intersections.size() ? b.intersections.get(bi).x : xTo;
            }
            x = xNext;
        }

        if (aInside == bInside)
            total += xTo - x;
        return total;
    }

    private void preprocess() {
        lastIndex = 0;
        if (!intersections.isEmpty()) {
            Collections.sort(intersections, Intersection.COMPARATOR);
            int totalDirection = 0;
            for (Intersection intersection : intersections) {
                totalDirection += intersection.direction;
                intersection.direction = totalDirection;
            }
        }
    }

    /** Populates the intersection list. */
    public void setIntersections(List<Intersection> intersections) {
        this.intersections = new ArrayList<>(intersections);
        preprocess();
    }

    private int moveTo(double x) {
        if (intersections.isEmpty())
            return -1;
        int index = lastIndex;
        if (x < intersections.get(index).x) {
            do {
                if (index == 0) {
                    lastIndex = 0;
                    return -1;
                }
                --index;
            } while (x < intersections.get(index).x);
        } else {
            while (index < intersections.size() - 1 && x >= intersections.get(index + 1).x)
                ++index;
        }
        lastIndex = index;
        return index;
    }

    /** Returns the number of intersections left of x. */
    public int countIntersections(double x) {
        return moveTo(x) + 1;
    }

    /** Returns the total sign of intersections left of x. */
    public int sumIntersections(double x) {
        int index = moveTo(x);
        if (index >= 0)
            return intersections.get(index).direction;
        return 0;
    }

    /** Decides whether the scanline is filled at x based on fill rule. */
    public boolean filled(double x, FillRule fillRule) {
        return interpretFillRule(sumIntersections(x), fillRule);
    }
}
