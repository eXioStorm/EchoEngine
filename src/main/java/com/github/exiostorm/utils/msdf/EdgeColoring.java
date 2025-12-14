package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import static com.github.exiostorm.utils.msdf.enums.EdgeColorEnum.*;

public class EdgeColoring {
    private static int symmetricalTrichotomy(int position, int n) {
        return (int) ((3+2.875*position/(n-1)-1.4375+.5)-3);
    }
    private static boolean isCorner(Vector2d dirA, Vector2d dirB, double crossThreshold) {
        double dot = dirA.dot(dirB);
        double cross = dirA.x * dirB.y - dirA.y * dirB.x;
        return dot <= 0 || Math.abs(cross) > crossThreshold;
    }
    private static int seedExtract2(SeedHolder seedHolder) {
        int result = (int)(seedHolder.seed & 1);
        seedHolder.seed >>= 1;
        return result;
    }
    static int seedExtract3(SeedHolder seedHolder) {
        int v = (int)(seedHolder.seed % 3);
        seedHolder.seed /= 3;
        return v;
    }
    static int initColor(SeedHolder seedHolder) {
        final int[] colors = { CYAN.getValue().color, MAGENTA.getValue().color, YELLOW.getValue().color };
        return colors[seedExtract3(seedHolder)];
    }
    public static List<Contours.Contour> extractContours(Shape shape) {
        List<Contours.Contour> contours = new ArrayList<>();
        Contours.Contour currentContour = null;

        float[] coords = new float[6];
        Vector2d currentPoint = null;  // Changed to Vector2d
        Vector2d startPoint = null;    // Changed to Vector2d
        //TODO 20251118 might be a bug here with it getting confused between our own Shape class, and awt.Shape
        PathIterator pathIterator = shape.getPathIterator(null);

        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    if (currentContour != null && !currentContour.edges.isEmpty()) {
                        contours.add(currentContour);
                    }
                    currentContour = new Contours.Contour();
                    currentPoint = new Vector2d(coords[0], coords[1]);
                    startPoint = new Vector2d(currentPoint);
                    break;

                case PathIterator.SEG_LINETO:
                    if (currentContour != null && currentPoint != null) {
                        Vector2d endPoint = new Vector2d(coords[0], coords[1]);
                        // Use static factory method with EdgeColor parameter
                        EdgeSegment edge = EdgeSegment.create(currentPoint, endPoint, EdgeColorEnum.WHITE.getValue());
                        currentContour.edges.add(new EdgeHolder(edge));
                        currentPoint = endPoint;
                    }
                    break;

                case PathIterator.SEG_QUADTO:
                    if (currentContour != null && currentPoint != null) {
                        Vector2d controlPoint = new Vector2d(coords[0], coords[1]);
                        Vector2d endPoint = new Vector2d(coords[2], coords[3]);
                        // Use static factory method for quadratic
                        EdgeSegment edge = EdgeSegment.create(currentPoint, controlPoint, endPoint, EdgeColorEnum.WHITE.getValue());
                        currentContour.edges.add(new EdgeHolder(edge));
                        currentPoint = endPoint;
                    }
                    break;

                case PathIterator.SEG_CUBICTO:
                    if (currentContour != null && currentPoint != null) {
                        Vector2d controlPoint1 = new Vector2d(coords[0], coords[1]);
                        Vector2d controlPoint2 = new Vector2d(coords[2], coords[3]);
                        Vector2d endPoint = new Vector2d(coords[4], coords[5]);
                        // Use static factory method for cubic
                        EdgeSegment edge = EdgeSegment.create(currentPoint, controlPoint1, controlPoint2, endPoint, EdgeColorEnum.WHITE.getValue());
                        currentContour.edges.add(new EdgeHolder(edge));
                        currentPoint = endPoint;
                    }
                    break;

                case PathIterator.SEG_CLOSE:
                    if (currentContour != null && currentPoint != null && startPoint != null) {
                        // Check if points are different (using a small epsilon for double comparison)
                        if (Math.abs(currentPoint.x - startPoint.x) > 0.001 ||
                                Math.abs(currentPoint.y - startPoint.y) > 0.001) {
                            EdgeSegment edge = EdgeSegment.create(currentPoint, startPoint, EdgeColorEnum.WHITE.getValue());
                            currentContour.edges.add(new EdgeHolder(edge));
                        }
                        contours.add(currentContour);
                        currentContour = null;
                    }
                    break;
            }

            pathIterator.next();
        }

        // Add the last contour if it wasn't closed
        if (currentContour != null && !currentContour.edges.isEmpty()) {
            contours.add(currentContour);
        }

        return contours;
    }

    // DEBUG VERSION - Let's add logging to see what's actually happening

    public static void edgeColoringSimple(java.util.List<Contours.Contour> contours, double angleThreshold, SeedHolder seedHolder) {
        //TODO 20251214 ChatGPT Says we're missing the logic for dot, and we only have cross
        double crossThreshold = Math.sin(angleThreshold);
        ColorHolder colorHolder = new ColorHolder(initColor(seedHolder));
        java.util.List<Integer> corners = new java.util.ArrayList<>();
        int contourIndex = 0;

        System.out.println("=== Starting edgeColoringSimple ===");

        for (java.util.Iterator<Contours.Contour> contourIter = contours.iterator(); contourIter.hasNext();) {
            Contours.Contour contour = contourIter.next();
            if (contour.edges.isEmpty())
                continue;

            System.out.println("\n--- Contour " + contourIndex + " with " + contour.edges.size() + " edges ---");

            {
                corners.clear();
                Vector2d prevDirection = contour.edges.get(contour.edges.size() - 1).edge.direction(1);
                int index = 0;
                for (java.util.Iterator<EdgeHolder> edgeIter = contour.edges.iterator(); edgeIter.hasNext(); index++) {
                    EdgeHolder edgeHolder = edgeIter.next();
                    if (isCorner(prevDirection.normalize(), edgeHolder.edge.direction(0).normalize(), crossThreshold))
                        corners.add(index);
                    prevDirection = edgeHolder.edge.direction(1);
                }
            }

            System.out.println("Corners found: " + corners.size() + " at indices: " + corners);
            int m = contour.edges.size();

            if (m == 1) {
                splitSingleEdgeContour(contour, seedHolder);
                continue;
            }

            if (m == 2) {
                splitTwoEdgeContour(contour, seedHolder);
                continue;
            }


            if (corners.isEmpty()) {
                switchColor(colorHolder, seedHolder);
                int color = colorHolder.color;

                for (EdgeHolder edge : contour.edges) {
                    edge.edge.edgeColor = color;
                    switchColor(colorHolder, seedHolder);
                    color = colorHolder.color;
                }
            }

            else if (corners.size() == 1) {
                System.out.println("TEARDROP CASE");
                ColorHolder[] colors = new ColorHolder[3];
                switchColor(colorHolder, seedHolder);
                colors[0] = new ColorHolder(colorHolder.color);
                colors[1] = new ColorHolder(WHITE.getValue().color);
                switchColor(colorHolder, seedHolder);
                colors[2] = new ColorHolder(colorHolder.color);

                System.out.println("Colors: [" + colors[0].color + ", " + colors[1].color + ", " + colors[2].color + "]");

                int corner = corners.get(0);
                if (contour.edges.size() >= 3) {
                    //int m = contour.edges.size();
                    System.out.println("Using trichotomy for " + m + " edges, corner at " + corner);
                    for (int i = 0; i < m; ++i) {
                        int colorIndex = 1 + symmetricalTrichotomy(i, m);
                        System.out.println("  Edge " + ((corner + i) % m) + " gets color[" + colorIndex + "] = " + colors[colorIndex].color);
                        contour.edges.get((corner + i) % m).edge.edgeColor = colors[colorIndex].color;
                    }
                } else if (contour.edges.size() >= 1) {
                    System.out.println("SPLITTING EDGES (edges: " + contour.edges.size() + ", corner: " + corner + ")");
                    EdgeSegment[] parts = new EdgeSegment[7];
                    EdgeSegment[] firstSplit = new EdgeSegment[3];

                    contour.edges.get(0).edge.splitInThirds(firstSplit);
                    parts[0 + 3 * corner] = firstSplit[0];
                    parts[1 + 3 * corner] = firstSplit[1];
                    parts[2 + 3 * corner] = firstSplit[2];

                    System.out.println("First edge split into positions: " + (0 + 3*corner) + ", " + (1 + 3*corner) + ", " + (2 + 3*corner));

                    if (contour.edges.size() >= 2) {
                        EdgeSegment[] secondSplit = new EdgeSegment[3];
                        contour.edges.get(1).edge.splitInThirds(secondSplit);
                        parts[3 - 3 * corner] = secondSplit[0];
                        parts[4 - 3 * corner] = secondSplit[1];
                        parts[5 - 3 * corner] = secondSplit[2];

                        System.out.println("Second edge split into positions: " + (3 - 3*corner) + ", " + (4 - 3*corner) + ", " + (5 - 3*corner));

                        // Assign colors
                        parts[0].edgeColor = colors[0].color;
                        parts[1].edgeColor = colors[0].color;
                        parts[2].edgeColor = colors[1].color;
                        parts[3].edgeColor = colors[1].color;
                        parts[4].edgeColor = colors[2].color;
                        parts[5].edgeColor = colors[2].color;

                        System.out.println("Color assignment:");
                        for (int i = 0; i < 6; i++) {
                            if (parts[i] != null) {
                                System.out.println("  parts[" + i + "].edgeColor = " + parts[i].edgeColor);
                            }
                        }
                    } else {
                        parts[0].edgeColor = colors[0].color;
                        parts[1].edgeColor = colors[1].color;
                        parts[2].edgeColor = colors[2].color;

                        System.out.println("Single edge - colors: " + colors[0].color + ", " + colors[1].color + ", " + colors[2].color);
                    }

                    // Rebuild edges
                    contour.edges.clear();
                    System.out.println("Rebuilding edge list...");
                    for (int i = 0; i < parts.length; ++i) {
                        if (parts[i] != null) {
                            EdgeHolder newHolder = new EdgeHolder(parts[i]);
                            System.out.println("  Adding parts[" + i + "] with edgeColor: " + parts[i].edgeColor);
                            System.out.println("  EdgeHolder.edge.edgeColor: " + newHolder.edge.edgeColor);
                            contour.edges.add(newHolder);
                        }
                    }
                    System.out.println("Final edge count: " + contour.edges.size());
                }
            }
            else {
                int cornerCount = corners.size();
                int spline = 0;
                int start = corners.get(0);
                //int m = contour.edges.size();
                switchColor(colorHolder, seedHolder);
                ColorHolder initialColor = new ColorHolder(colorHolder.color);
                ColorHolder currentColor = new ColorHolder(colorHolder.color); // Create a new holder for this contour

                System.out.println("Multiple corners case: " + cornerCount + " corners");

                for (int i = 0; i < m; ++i) {
                    int index = (start + i) % m;

                    if (spline + 1 < cornerCount && corners.get(spline + 1) == index) {
                        ++spline;
                        switchColor(
                                currentColor,
                                seedHolder,
                                (spline == cornerCount - 1)
                                        ? initialColor.color
                                        : WHITE.getValue().color
                        );
                    }

                    contour.edges.get(index).edge.edgeColor = (currentColor.color);
                }
            }
            contourIndex++;
        }

        System.out.println("\n=== Finished edgeColoringSimple ===");
    }
    private static void splitSingleEdgeContour(
            Contours.Contour contour,
            SeedHolder seedHolder
    ) {
        EdgeHolder edge = contour.edges.get(0);

        EdgeSegment[] parts = new EdgeSegment[3];
        edge.edge.splitInThirds(parts);

        ColorHolder color = new ColorHolder(initColor(seedHolder));

        contour.edges.clear();

        for (int i = 0; i < 3; i++) {
            EdgeSegment part = parts[i];
            part.edgeColor = color.color;
            contour.edges.add(new EdgeHolder(part));
            switchColor(color, seedHolder);
        }
    }
    private static void splitTwoEdgeContour(
            Contours.Contour contour,
            SeedHolder seedHolder
    ) {
        EdgeSegment[] partsA = new EdgeSegment[3];
        EdgeSegment[] partsB = new EdgeSegment[3];

        contour.edges.get(0).edge.splitInThirds(partsA);
        contour.edges.get(1).edge.splitInThirds(partsB);

        ColorHolder color = new ColorHolder(initColor(seedHolder));

        contour.edges.clear();

        // A0 A1 A2 B0 B1 B2
        for (int i = 0; i < 3; i++) {
            partsA[i].edgeColor = color.color;
            contour.edges.add(new EdgeHolder(partsA[i]));
            switchColor(color, seedHolder);
        }

        for (int i = 0; i < 3; i++) {
            partsB[i].edgeColor = color.color;
            contour.edges.add(new EdgeHolder(partsB[i]));
            switchColor(color, seedHolder);
        }
    }

    private static String colorToString(int color) {
        boolean cyan = (color & CYAN.getValue().color) != 0;
        boolean magenta = (color & MAGENTA.getValue().color) != 0;
        boolean yellow = (color & YELLOW.getValue().color) != 0;

        java.util.List<String> channels = new java.util.ArrayList<>();
        if (cyan) channels.add("C");
        if (magenta) channels.add("M");
        if (yellow) channels.add("Y");

        if (channels.isEmpty()) return "NONE(0x" + Integer.toHexString(color) + ")";
        return String.join("+", channels) + "(0x" + Integer.toHexString(color) + ")";
    }
    private static void switchColor(ColorHolder ColorHolder, SeedHolder seedHolder) {
        int shifted = ColorHolder.color << (1 + seedExtract2(seedHolder));
        ColorHolder.color = (shifted | (shifted >> 3)) & WHITE.getValue().color; // Now WHITE = 7, so this works correctly
    }
    private static void switchColor(ColorHolder colorHolder, SeedHolder seedHolder, int banned) {
        int combined = colorHolder.color & banned;
        if (combined == CYAN.getValue().color || combined == MAGENTA.getValue().color || combined == YELLOW.getValue().color) {
            colorHolder.color = combined ^ WHITE.getValue().color;
        } else {
            switchColor(colorHolder, seedHolder);
        }
    }
}
