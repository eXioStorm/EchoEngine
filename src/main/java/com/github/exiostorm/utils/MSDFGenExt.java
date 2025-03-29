package com.github.exiostorm.utils;

import org.joml.Vector2f;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MSDF generation utilities for Java AWT Shapes
 */
public class MSDFGenExt {

    // Constants for edge colors
    public static final int WHITE = 0;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int BLUE = 3;
    public static final int YELLOW = 4;
    public static final int CYAN = 5;
    public static final int MAGENTA = 6;

    /**
     * Represents a contour in the shape
     */
    public static class Contour {
        private List<EdgeHolder> edges = new ArrayList<>();

        public List<EdgeHolder> getEdges() {
            return edges;
        }
    }

    /**
     * Represents an edge segment with coloring information
     */
    public static class EdgeSegment {
        private Vector2f p0, p1, c0, c1; // Points and control points
        private int type; // LINE, QUADRATIC, CUBIC
        private int color = WHITE;

        public static final int LINE = 0;
        public static final int QUADRATIC = 1;
        public static final int CUBIC = 2;

        public EdgeSegment(Vector2f p0, Vector2f p1, int type) {
            this.p0 = new Vector2f(p0);
            this.p1 = new Vector2f(p1);
            this.type = type;
        }

        public EdgeSegment(Vector2f p0, Vector2f c0, Vector2f p1, int type) {
            this.p0 = new Vector2f(p0);
            this.p1 = new Vector2f(p1);
            this.c0 = new Vector2f(c0);
            this.type = type;
        }

        public EdgeSegment(Vector2f p0, Vector2f c0, Vector2f c1, Vector2f p1) {
            this.p0 = new Vector2f(p0);
            this.p1 = new Vector2f(p1);
            this.c0 = new Vector2f(c0);
            this.c1 = new Vector2f(c1);
            this.type = CUBIC;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }

        public Vector2f getPoint(float t) {
            if (type == LINE) {
                return new Vector2f(
                        p0.x + (p1.x - p0.x) * t,
                        p0.y + (p1.y - p0.y) * t
                );
            } else if (type == QUADRATIC) {
                float mt = 1 - t;
                return new Vector2f(
                        mt * mt * p0.x + 2 * mt * t * c0.x + t * t * p1.x,
                        mt * mt * p0.y + 2 * mt * t * c0.y + t * t * p1.y
                );
            } else { // CUBIC
                float mt = 1 - t;
                float mt2 = mt * mt;
                float t2 = t * t;
                return new Vector2f(
                        mt2 * mt * p0.x + 3 * mt2 * t * c0.x + 3 * mt * t2 * c1.x + t2 * t * p1.x,
                        mt2 * mt * p0.y + 3 * mt2 * t * c0.y + 3 * mt * t2 * c1.y + t2 * t * p1.y
                );
            }
        }

        public Vector2f direction(float t) {
            if (type == LINE) {
                return new Vector2f(p1.x - p0.x, p1.y - p0.y);
            } else if (type == QUADRATIC) {
                float mt = 1 - t;
                return new Vector2f(
                        2 * mt * (c0.x - p0.x) + 2 * t * (p1.x - c0.x),
                        2 * mt * (c0.y - p0.y) + 2 * t * (p1.y - c0.y)
                );
            } else { // CUBIC
                float mt = 1 - t;
                return new Vector2f(
                        3 * mt * mt * (c0.x - p0.x) + 6 * mt * t * (c1.x - c0.x) + 3 * t * t * (p1.x - c1.x),
                        3 * mt * mt * (c0.y - p0.y) + 6 * mt * t * (c1.y - c0.y) + 3 * t * t * (p1.y - c1.y)
                );
            }
        }

        public void splitInThirds(EdgeSegment part1, EdgeSegment part2, EdgeSegment part3) {
            if (type == LINE) {
                Vector2f p1Third = getPoint(1.0f/3);
                Vector2f p2Third = getPoint(2.0f/3);

                part1 = new EdgeSegment(p0, p1Third, LINE);
                part2 = new EdgeSegment(p1Third, p2Third, LINE);
                part3 = new EdgeSegment(p2Third, p1, LINE);
            } else if (type == QUADRATIC) {
                // De Casteljau algorithm for splitting
                Vector2f p1Third = getPoint(1.0f/3);
                Vector2f p2Third = getPoint(2.0f/3);

                Vector2f c0_1 = new Vector2f(
                        p0.x + (c0.x - p0.x) * (1.0f/3),
                        p0.y + (c0.y - p0.y) * (1.0f/3)
                );

                Vector2f c0_2 = new Vector2f(
                        p0.x + (c0.x - p0.x) * (2.0f/3),
                        p0.y + (c0.y - p0.y) * (2.0f/3)
                );

                Vector2f c1_1 = new Vector2f(
                        c0.x + (p1.x - c0.x) * (1.0f/3),
                        c0.y + (p1.y - c0.y) * (1.0f/3)
                );

                Vector2f c1_2 = new Vector2f(
                        c0.x + (p1.x - c0.x) * (2.0f/3),
                        c0.y + (p1.y - c0.y) * (2.0f/3)
                );

                part1 = new EdgeSegment(p0, c0_1, p1Third, QUADRATIC);
                part2 = new EdgeSegment(p1Third, c0_2, p2Third, QUADRATIC);
                part3 = new EdgeSegment(p2Third, c1_2, p1, QUADRATIC);
            } else { // CUBIC
                // Similar De Casteljau splitting for cubic curves
                Vector2f p1Third = getPoint(1.0f/3);
                Vector2f p2Third = getPoint(2.0f/3);

                // Calculate control points for the segments (De Casteljau algorithm)
                // This is simplified - full implementation would interpolate all control points
                part1 = new EdgeSegment(p0,
                        new Vector2f(p0).lerp(c0, 1.0f/3),
                        new Vector2f(p0).lerp(c0, 2.0f/3),
                        p1Third);

                part2 = new EdgeSegment(p1Third,
                        new Vector2f(c0).lerp(c1, 1.0f/3),
                        new Vector2f(c0).lerp(c1, 2.0f/3),
                        p2Third);

                part3 = new EdgeSegment(p2Third,
                        new Vector2f(c1).lerp(p1, 1.0f/3),
                        new Vector2f(c1).lerp(p1, 2.0f/3),
                        p1);
            }
        }
    }

    /**
     * Wrapper for EdgeSegment providing common access patterns
     */
    public static class EdgeHolder {
        private EdgeSegment edge;

        public EdgeHolder(EdgeSegment edge) {
            this.edge = edge;
        }

        public Vector2f direction(float t) {
            return edge.direction(t);
        }

        public void setColor(int color) {
            edge.setColor(color);
        }

        public int getColor() {
            return edge.getColor();
        }

        public void splitInThirds(EdgeSegment part1, EdgeSegment part2, EdgeSegment part3) {
            edge.splitInThirds(part1, part2, part3);
        }
    }

    /**
     * Extract contours from a Java AWT Shape
     */
    public static List<Contour> extractContours(Shape shape) {
        List<Contour> contours = new ArrayList<>();
        Contour currentContour = null;

        float[] coords = new float[6];
        Vector2f currentPoint = null;
        Vector2f startPoint = null;

        PathIterator pathIterator = shape.getPathIterator(null);

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    if (currentContour != null && !currentContour.getEdges().isEmpty()) {
                        contours.add(currentContour);
                    }
                    currentContour = new Contour();
                    currentPoint = new Vector2f(coords[0], coords[1]);
                    startPoint = new Vector2f(currentPoint);
                    break;

                case PathIterator.SEG_LINETO:
                    if (currentContour != null && currentPoint != null) {
                        Vector2f endPoint = new Vector2f(coords[0], coords[1]);
                        EdgeSegment edge = new EdgeSegment(currentPoint, endPoint, EdgeSegment.LINE);
                        currentContour.getEdges().add(new EdgeHolder(edge));
                        currentPoint = endPoint;
                    }
                    break;

                case PathIterator.SEG_QUADTO:
                    if (currentContour != null && currentPoint != null) {
                        Vector2f controlPoint = new Vector2f(coords[0], coords[1]);
                        Vector2f endPoint = new Vector2f(coords[2], coords[3]);
                        EdgeSegment edge = new EdgeSegment(currentPoint, controlPoint, endPoint, EdgeSegment.QUADRATIC);
                        currentContour.getEdges().add(new EdgeHolder(edge));
                        currentPoint = endPoint;
                    }
                    break;

                case PathIterator.SEG_CUBICTO:
                    if (currentContour != null && currentPoint != null) {
                        Vector2f controlPoint1 = new Vector2f(coords[0], coords[1]);
                        Vector2f controlPoint2 = new Vector2f(coords[2], coords[3]);
                        Vector2f endPoint = new Vector2f(coords[4], coords[5]);
                        EdgeSegment edge = new EdgeSegment(currentPoint, controlPoint1, controlPoint2, endPoint);
                        currentContour.getEdges().add(new EdgeHolder(edge));
                        currentPoint = endPoint;
                    }
                    break;

                case PathIterator.SEG_CLOSE:
                    if (currentContour != null && currentPoint != null && startPoint != null) {
                        if (!currentPoint.equals(startPoint)) {
                            EdgeSegment edge = new EdgeSegment(currentPoint, startPoint, EdgeSegment.LINE);
                            currentContour.getEdges().add(new EdgeHolder(edge));
                        }
                        contours.add(currentContour);
                        currentContour = null;
                    }
                    break;
            }

            pathIterator.next();
        }

        // Add the last contour if it wasn't closed
        if (currentContour != null && !currentContour.getEdges().isEmpty()) {
            contours.add(currentContour);
        }

        return contours;
    }

    /**
     * Apply edge coloring to a Java AWT Shape
     */
    public static List<Contour> edgeColoringSimple(Shape shape, double angleThreshold, long seed) {
        List<Contour> contours = extractContours(shape);
        double crossThreshold = Math.sin(angleThreshold);
        int color = initColor(seed);
        List<Integer> corners = new ArrayList<>();

        for (Contour contour : contours) {
            if (contour.getEdges().isEmpty()) {
                continue;
            }

            // Identify corners
            corners.clear();
            Vector2f prevDirection = contour.getEdges().get(contour.getEdges().size() - 1).direction(1);
            int index = 0;

            for (EdgeHolder edge : contour.getEdges()) {
                Vector2f dir0 = edge.direction(0);
                if (!dir0.isFinite()) {
                    // Handle degenerate cases (e.g., zero-length edges)
                    dir0.set(0, 0);
                }

                Vector2f normalized0 = new Vector2f(dir0).normalize();
                Vector2f normalized1 = new Vector2f(prevDirection).normalize();

                if (isCorner(normalized1, normalized0, crossThreshold)) {
                    corners.add(index);
                }
                prevDirection = edge.direction(1);
                index++;
            }

            // Smooth contour
            if (corners.isEmpty()) {
                color = switchColor(color, seed);
                for (EdgeHolder edge : contour.getEdges()) {
                    edge.setColor(color);
                }
            }
            // "Teardrop" case
            else if (corners.size() == 1) {
                int[] colors = new int[3];
                color = switchColor(color, seed);
                colors[0] = color;
                colors[1] = WHITE;
                color = switchColor(color, seed);
                colors[2] = color;

                int corner = corners.get(0);
                if (contour.getEdges().size() >= 3) {
                    int m = contour.getEdges().size();
                    for (int i = 0; i < m; ++i) {
                        contour.getEdges().get((corner + i) % m).setColor(colors[1 + symmetricalTrichotomy(i, m)]);
                    }
                } else if (contour.getEdges().size() >= 1) {
                    // Less than three edge segments for three colors => edges must be split
                    EdgeSegment[] parts = new EdgeSegment[7];

                    // Note: This is a simplified version that would need to be expanded
                    // for a complete implementation
                    EdgeHolder edgeHolder = contour.getEdges().get(0);
                    edgeHolder.splitInThirds(parts[0 + 3 * corner], parts[1 + 3 * corner], parts[2 + 3 * corner]);

                    if (contour.getEdges().size() >= 2) {
                        EdgeHolder edgeHolder2 = contour.getEdges().get(1);
                        edgeHolder2.splitInThirds(parts[3 - 3 * corner], parts[4 - 3 * corner], parts[5 - 3 * corner]);

                        parts[0].setColor(colors[0]);
                        parts[1].setColor(colors[0]);
                        parts[2].setColor(colors[1]);
                        parts[3].setColor(colors[1]);
                        parts[4].setColor(colors[2]);
                        parts[5].setColor(colors[2]);
                    } else {
                        parts[0].setColor(colors[0]);
                        parts[1].setColor(colors[1]);
                        parts[2].setColor(colors[2]);
                    }

                    contour.getEdges().clear();
                    for (int i = 0; i < parts.length && parts[i] != null; ++i) {
                        contour.getEdges().add(new EdgeHolder(parts[i]));
                    }
                }
            }
            // Multiple corners
            else {
                int cornerCount = corners.size();
                int spline = 0;
                int start = corners.get(0);
                int m = contour.getEdges().size();

                color = switchColor(color, seed);
                int initialColor = color;

                for (int i = 0; i < m; ++i) {
                    int idx = (start + i) % m;
                    if (spline + 1 < cornerCount && corners.get(spline + 1) == idx) {
                        ++spline;
                        color = switchColor(color, seed, (spline == cornerCount - 1) ? initialColor : 0);
                    }
                    contour.getEdges().get(idx).setColor(color);
                }
            }
        }

        return contours;
    }

    /**
     * Initialize color from a seed
     */
    private static int initColor(long seed) {
        Random random = new Random(seed);
        int color;
        do {
            color = 1 + random.nextInt(6); // Generates RED through MAGENTA (1-6)
        } while (color == WHITE);
        return color;
    }

    /**
     * Switch to a different color
     */
    private static int switchColor(int color, long seed) {
        return switchColor(color, seed, 0);
    }

    /**
     * Switch to a different color with a preferred alternative
     */
    private static int switchColor(int color, long seed, int preferredAlternative) {
        Random random = new Random(seed + color);
        if (preferredAlternative != 0 && preferredAlternative != color && random.nextInt(4) != 0) {
            return preferredAlternative;
        }

        int newColor;
        do {
            newColor = 1 + random.nextInt(6); // Generates RED through MAGENTA (1-6)
        } while (newColor == color || newColor == WHITE);

        return newColor;
    }

    /**
     * Determines if two edge directions form a corner
     */
    private static boolean isCorner(Vector2f dirA, Vector2f dirB, double crossThreshold) {
        float cross = dirA.x * dirB.y - dirA.y * dirB.x;
        return Math.abs(cross) > crossThreshold;
    }

    /**
     * Helper function for the teardrop case
     */
    private static int symmetricalTrichotomy(int index, int total) {
        return (2 * index < total) ? ((4 * index < total) ? -1 : 0) : 1;
    }
}