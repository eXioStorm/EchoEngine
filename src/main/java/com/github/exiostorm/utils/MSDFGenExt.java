package com.github.exiostorm.utils;

import org.joml.Vector2f;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * MSDF generation utilities for Java AWT Shapes
 */
public class MSDFGenExt {

    // Constants for edge colors
    public static final int BLACK = 0;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int BLUE = 4;
    public static final int YELLOW = RED | GREEN;   // 3
    public static final int MAGENTA = RED | BLUE;   // 5
    public static final int CYAN = GREEN | BLUE;    // 6
    public static final int WHITE = RED | GREEN | BLUE; // 7

    /**
     * Represents a contour in the shape
     */
    public static class Contour {
        private List<EdgeHolder> edges = new ArrayList<>();

        public List<EdgeHolder> getEdges() {
            return edges;
        }

        public void addEdge(EdgeHolder edge) {
            edges.add(edge);
        }

        public boolean isEmpty() {
            return edges.isEmpty();
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

        public int getType() {
            return type;
        }

        public Vector2f getP0() { return new Vector2f(p0); }
        public Vector2f getP1() { return new Vector2f(p1); }
        public Vector2f getC0() { return c0 != null ? new Vector2f(c0) : null; }
        public Vector2f getC1() { return c1 != null ? new Vector2f(c1) : null; }

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

        public EdgeSegment[] splitInThirds() {
            EdgeSegment[] parts = new EdgeSegment[3];

            if (type == LINE) {
                Vector2f p1Third = getPoint(1.0f/3);
                Vector2f p2Third = getPoint(2.0f/3);

                parts[0] = new EdgeSegment(p0, p1Third, LINE);
                parts[1] = new EdgeSegment(p1Third, p2Third, LINE);
                parts[2] = new EdgeSegment(p2Third, p1, LINE);
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

                Vector2f c1_2 = new Vector2f(
                        c0.x + (p1.x - c0.x) * (2.0f/3),
                        c0.y + (p1.y - c0.y) * (2.0f/3)
                );

                parts[0] = new EdgeSegment(p0, c0_1, p1Third, QUADRATIC);
                parts[1] = new EdgeSegment(p1Third, c0_2, p2Third, QUADRATIC);
                parts[2] = new EdgeSegment(p2Third, c1_2, p1, QUADRATIC);
            } else { // CUBIC
                // De Casteljau splitting for cubic curves
                Vector2f p1Third = getPoint(1.0f/3);
                Vector2f p2Third = getPoint(2.0f/3);

                // Calculate intermediate control points using De Casteljau's algorithm
                Vector2f q0 = new Vector2f(p0).lerp(c0, 1.0f/3);
                Vector2f q1 = new Vector2f(c0).lerp(c1, 1.0f/3);
                Vector2f q2 = new Vector2f(c1).lerp(p1, 1.0f/3);
                Vector2f r0 = new Vector2f(q0).lerp(q1, 1.0f/3);
                Vector2f r1 = new Vector2f(q1).lerp(q2, 1.0f/3);

                parts[0] = new EdgeSegment(p0, q0, r0, p1Third);
                parts[1] = new EdgeSegment(p1Third, r0, r1, p2Third);
                parts[2] = new EdgeSegment(p2Third, r1, q2, p1);
            }

            // Copy color to all parts
            for (EdgeSegment part : parts) {
                part.setColor(this.color);
            }

            return parts;
        }

        public double getDistance(Vector2f point) {
            if (type == LINE) {
                // Proper line segment distance calculation
                Vector2f ab = new Vector2f(p1).sub(p0);
                Vector2f ap = new Vector2f(point).sub(p0);

                float abLength2 = ab.lengthSquared();
                if (abLength2 == 0) return point.distance(p0); // Degenerate case

                float t = Math.max(0, Math.min(1, ap.dot(ab) / abLength2));
                Vector2f closest = new Vector2f(p0).add(new Vector2f(ab).mul(t));
                return point.distance(closest);
            }

            // For curves, use more samples and better algorithm
            double minDist = Double.MAX_VALUE;
            int samples = 100;

            for (int i = 0; i <= samples; i++) {
                float t = i / (float)samples;
                Vector2f edgePoint = getPoint(t);
                double dist = point.distance(edgePoint);
                minDist = Math.min(minDist, dist);
            }

            return minDist;
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

        public EdgeSegment getEdge() {
            return edge;
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

        public EdgeSegment[] splitInThirds() {
            return edge.splitInThirds();
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
                        if (!currentPoint.equals(startPoint, 0.001f)) { // Use epsilon comparison
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

    private static class ColorWrapper {
        int color;

        ColorWrapper(int color) {
            this.color = color;
        }
    }

    // Helper class to simulate C++ reference behavior for seed
    private static class SeedWrapper {
        long seed;

        SeedWrapper(long seed) {
            this.seed = seed;
        }
    }

    /**
     * CORRECTED: Extract 2 bits from seed (matching C++ exactly)
     */
    private static int seedExtract2(SeedWrapper seedWrapper) {
        int result = (int)(seedWrapper.seed & 1); // Extract lowest bit only
        seedWrapper.seed >>= 1; // Shift by 1 bit
        return result;
    }

    /**
     * CORRECTED: Extract 3 bits from seed (matching C++ exactly)
     */
    private static int seedExtract3(SeedWrapper seedWrapper) {
        int result = (int)(seedWrapper.seed % 3); // Use modulo 3
        seedWrapper.seed /= 3; // Divide by 3
        return result;
    }

    /**
     * CORRECTED: Initialize color to match C++ exactly
     */
    private static int initColor(long seed) {
        SeedWrapper seedWrapper = new SeedWrapper(seed);
        int[] colors = {CYAN, MAGENTA, YELLOW}; // Match C++ order
        return colors[seedExtract3(seedWrapper)];
    }

    /**
     * CORRECTED: Switch to a different color using deterministic bit operations (C++ version)
     */
    private static void switchColor(ColorWrapper colorWrapper, SeedWrapper seedWrapper) {
        int shifted = colorWrapper.color << (1 + seedExtract2(seedWrapper));
        colorWrapper.color = (shifted | (shifted >> 3)) & WHITE; // Now WHITE = 7, so this works correctly
    }

    /**
     * CORRECTED: Switch to a different color with banned color (C++ version)
     */
    private static void switchColor(ColorWrapper colorWrapper, SeedWrapper seedWrapper, int banned) {
        int combined = colorWrapper.color & banned;
        if (combined == RED || combined == GREEN || combined == BLUE) {
            colorWrapper.color = combined ^ WHITE;
        } else {
            switchColor(colorWrapper, seedWrapper);
        }
    }

    /**
     * CORRECTED: Determines if two edge directions form a corner (matching C++)
     */
    private static boolean isCorner(Vector2f dirA, Vector2f dirB, double crossThreshold) {
        float dot = dirA.dot(dirB);
        float cross = dirA.x * dirB.y - dirA.y * dirB.x;
        return dot <= 0 || Math.abs(cross) > crossThreshold; // Added missing dot product check
    }

    /**
     * Helper function for the teardrop case
     */
    private static int symmetricalTrichotomy(int index, int total) {
        return (int)(3 + 2.875 * index / (total - 1) - 1.4375 + 0.5) - 3; // Match C++ formula exactly
    }

    /**
     * Check if a Vector2f is finite (not NaN or infinite)
     */
    private static boolean isFinite(Vector2f vec) {
        return Float.isFinite(vec.x) && Float.isFinite(vec.y);
    }

    /**
     * CORRECTED: Apply edge coloring to a Java AWT Shape
     */
    public static List<Contour> edgeColoringSimple(Shape shape, double angleThreshold, long seed) {
        List<Contour> contours = extractContours(shape);
        double crossThreshold = Math.sin(angleThreshold);
        ColorWrapper colorWrapper = new ColorWrapper(initColor(seed));
        SeedWrapper seedWrapper = new SeedWrapper(seed);
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
                if (!isFinite(dir0)) {
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
                switchColor(colorWrapper, seedWrapper);
                for (EdgeHolder edge : contour.getEdges()) {
                    edge.setColor(colorWrapper.color);
                }
            }
            // "Teardrop" case
            else if (corners.size() == 1) {
                int[] colors = new int[3];
                switchColor(colorWrapper, seedWrapper);
                colors[0] = colorWrapper.color;
                colors[1] = WHITE; // Now properly using WHITE = 7
                switchColor(colorWrapper, seedWrapper);
                colors[2] = colorWrapper.color;

                int corner = corners.get(0);
                if (contour.getEdges().size() >= 3) {
                    int m = contour.getEdges().size();
                    for (int i = 0; i < m; ++i) {
                        contour.getEdges().get((corner + i) % m).setColor(colors[1 + symmetricalTrichotomy(i, m)]);
                    }
                } else if (contour.getEdges().size() >= 1) {
                    // Edge splitting logic
                    EdgeSegment[] parts = new EdgeSegment[7];

                    EdgeSegment[] firstSplit = contour.getEdges().get(0).splitInThirds();
                    parts[0 + 3 * corner] = firstSplit[0];
                    parts[1 + 3 * corner] = firstSplit[1];
                    parts[2 + 3 * corner] = firstSplit[2];

                    if (contour.getEdges().size() >= 2) {
                        EdgeSegment[] secondSplit = contour.getEdges().get(1).splitInThirds();
                        parts[3 - 3 * corner] = secondSplit[0];
                        parts[4 - 3 * corner] = secondSplit[1];
                        parts[5 - 3 * corner] = secondSplit[2];

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
                    for (EdgeSegment part : parts) {
                        if (part != null) {
                            contour.getEdges().add(new EdgeHolder(part));
                        }
                    }
                }
            }
            // Multiple corners
            else {
                int cornerCount = corners.size();
                int spline = 0;
                int start = corners.get(0);
                int m = contour.getEdges().size();

                switchColor(colorWrapper, seedWrapper);
                int initialColor = colorWrapper.color;

                for (int i = 0; i < m; ++i) {
                    int idx = (start + i) % m;
                    if (spline + 1 < cornerCount && corners.get(spline + 1) == idx) {
                        ++spline;
                        // Use the C++ version with banned color
                        int bannedColor = (spline == cornerCount - 1) ? initialColor : BLACK;
                        switchColor(colorWrapper, seedWrapper, bannedColor);
                    }
                    contour.getEdges().get(idx).setColor(colorWrapper.color);
                }
            }
        }

        return contours;
    }

    /**
     * Calculate bounds of contours
     */
    public static Rectangle2D.Double calculateBounds(List<Contour> contours) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;

        for (Contour contour : contours) {
            for (EdgeHolder edgeHolder : contour.getEdges()) {
                EdgeSegment edge = edgeHolder.getEdge();

                // Sample points along the edge to find bounds
                for (int i = 0; i <= 20; i++) {
                    float t = i / 20.0f;
                    Vector2f point = edge.getPoint(t);

                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                }
            }
        }

        if (minX == Double.MAX_VALUE) {
            return new Rectangle2D.Double(0, 0, 0, 0);
        }

        return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Calculate signed distance from point to nearest edge in contours
     */
    public static double calculateSignedDistance(Vector2f point, List<Contour> contours) {
        double minDistance = Double.MAX_VALUE;
        boolean inside = false;

        for (Contour contour : contours) {
            boolean contourInside = isPointInsideContour(point, contour);
            inside = inside || contourInside;

            for (EdgeHolder edgeHolder : contour.getEdges()) {
                double dist = edgeHolder.getEdge().getDistance(point);
                minDistance = Math.min(minDistance, dist);
            }
        }

        return inside ? -minDistance : minDistance;
    }

    /**
     * Check if point is inside a contour using ray casting algorithm
     */
    private static boolean isPointInsideContour(Vector2f point, Contour contour) {
        int intersections = 0;
        final float EPSILON = 1e-6f;

        for (EdgeHolder edgeHolder : contour.getEdges()) {
            EdgeSegment edge = edgeHolder.getEdge();

            // Use more samples for better accuracy
            int samples = 50; // Increased from 20
            Vector2f prev = edge.getPoint(0);

            for (int i = 1; i <= samples; i++) {
                Vector2f curr = edge.getPoint(i / (float)samples);

                // Avoid edge cases where ray passes through vertex
                if (Math.abs(curr.y - point.y) < EPSILON) {
                    curr.y += EPSILON;
                }
                if (Math.abs(prev.y - point.y) < EPSILON) {
                    prev.y += EPSILON;
                }

                // Check if horizontal ray from point crosses this segment
                if ((prev.y > point.y) != (curr.y > point.y)) {
                    float intersectX = prev.x + (curr.x - prev.x) * (point.y - prev.y) / (curr.y - prev.y);
                    if (intersectX > point.x) {
                        intersections++;
                    }
                }

                prev = new Vector2f(curr); // Make a copy to avoid reference issues
            }
        }

        return (intersections % 2) == 1;
    }

    /**
     * Calculate multi-channel signed distance at a point
     */
    public static Vector3d calculateMSDF(Vector2f point, List<Contour> contours, double range) {
        double[] distances = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};

        // Initialize all channels to a large positive distance first
        for (int i = 0; i < 3; i++) {
            distances[i] = range; // Start with maximum range
        }

        for (Contour contour : contours) {
            boolean inside = isPointInsideContour(point, contour);

            for (EdgeHolder edgeHolder : contour.getEdges()) {
                EdgeSegment edge = edgeHolder.getEdge();
                double dist = edge.getDistance(point);
                int color = edge.getColor();

                // Apply sign based on inside/outside
                double signedDist = inside ? -dist : dist;

                // Map color to channels
                int[] channels = getChannelsForColor(color);

                for (int channel : channels) {
                    if (channel >= 0 && channel < 3) {
                        // Keep the distance with smallest absolute value for each channel
                        if (Math.abs(signedDist) < Math.abs(distances[channel])) {
                            distances[channel] = signedDist;
                        }
                    }
                }
            }
        }

        // Normalize to range [-0.5, 0.5] with proper clamping
        double r = Math.max(-0.5, Math.min(0.5, distances[0] / range * 0.5));
        double g = Math.max(-0.5, Math.min(0.5, distances[1] / range * 0.5));
        double b = Math.max(-0.5, Math.min(0.5, distances[2] / range * 0.5));

        return new Vector3d(r, g, b);
    }

    /**
     * Generate the actual MSDF from colored contours
     */
    public static BufferedImage generateMSDF(List<MSDFGenExt.Contour> contours, int size, double range) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        // Calculate bounds of the shape
        Rectangle2D bounds = calculateBounds(contours);
        if (bounds.getWidth() == 0 || bounds.getHeight() == 0) {
            return image;
        }

        // Calculate scale to fit glyph in texture with padding
        double padding = range * 2; // Increase padding for better results
        double scaleX = (size - 2 * padding) / bounds.getWidth();
        double scaleY = (size - 2 * padding) / bounds.getHeight();
        double scale = Math.min(scaleX, scaleY);

        // Calculate translation to center the glyph
        double translateX = (size - bounds.getWidth() * scale) / 2 - bounds.getX() * scale;
        double translateY = (size - bounds.getHeight() * scale) / 2 - bounds.getY() * scale;

        // Generate MSDF for each pixel
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // Convert pixel coordinates to shape space
                double shapeX = (x - translateX) / scale;
                double shapeY = (y - translateY) / scale;

                Vector2f point = new Vector2f((float)shapeX, (float)shapeY);

                // Calculate multi-channel signed distance
                Vector3d msdf = calculateMSDF(point, contours, range / scale);

                // Convert to RGB values (0-255) with proper rounding
                // Map from [-0.5, 0.5] to [0, 255]
                int r = (int)Math.round(Math.max(0, Math.min(255, (msdf.x + 0.5) * 255.0)));
                int g = (int)Math.round(Math.max(0, Math.min(255, (msdf.y + 0.5) * 255.0)));
                int b = (int)Math.round(Math.max(0, Math.min(255, (msdf.z + 0.5) * 255.0)));

                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }


    /**
     * CORRECTED: Map edge colors to RGB channels with proper bit patterns
     */
    private static int[] getChannelsForColor(int color) {
        return switch (color) {
            case RED -> new int[]{0}; // R channel
            case GREEN -> new int[]{1}; // G channel
            case BLUE -> new int[]{2}; // B channel
            case YELLOW -> new int[]{0, 1}; // RG channels (RED | GREEN)
            case CYAN -> new int[]{1, 2}; // GB channels (GREEN | BLUE)
            case MAGENTA -> new int[]{0, 2}; // RB channels (RED | BLUE)
            case WHITE -> new int[]{0, 1, 2}; // All channels (RED | GREEN | BLUE)
            default -> new int[]{0, 1, 2}; // All channels for any other value
        };
    }

    // Helper methods you need to implement:
    private double calculateOptimalScale(Rectangle2D bounds, int textureSize, double range) {
        // Match C++ auto-frame logic
        double boundsWidth = bounds.getWidth() + 2 * range;
        double boundsHeight = bounds.getHeight() + 2 * range;

        if (boundsWidth * textureSize < boundsHeight * textureSize) {
            return textureSize / boundsHeight;
        } else {
            return textureSize / boundsWidth;
        }
    }

    private Vector2d calculateTranslation(Rectangle2D bounds, double scale, int textureSize) {
        // Center the glyph in the texture
        double centerX = textureSize / (2.0 * scale) - (bounds.getX() + bounds.getWidth() / 2.0);
        double centerY = textureSize / (2.0 * scale) - (bounds.getY() + bounds.getHeight() / 2.0);
        return new Vector2d(centerX, centerY);
    }

    private boolean shouldApplyScanlinePass() {
        // Match C++ logic for when scanline pass is needed
        return true; // Simplified - C++ has complex conditions
    }
}