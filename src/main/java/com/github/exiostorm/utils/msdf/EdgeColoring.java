package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;
import org.joml.Vector2f;

import java.awt.*;
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

    public static void edgeColoringSimple(java.util.List<Contours.Contour> contours, double angleThreshold, SeedHolder seedHolder) {
        double crossThreshold = Math.sin(angleThreshold);
        ColorHolder colorHolder =  new ColorHolder(initColor(seedHolder));
        java.util.List<Integer> corners = new java.util.ArrayList<>();

        for (java.util.Iterator<Contours.Contour> contourIter = contours.iterator(); contourIter.hasNext();) {
            Contours.Contour contour = contourIter.next();
            if (contour.edges.isEmpty())
                continue;

            { // Identify corners
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

            // Smooth contour
            if (corners.isEmpty()) {
                switchColor(colorHolder, seedHolder);
                for (java.util.Iterator<EdgeHolder> edgeIter = contour.edges.iterator(); edgeIter.hasNext();) {
                    EdgeHolder edge = edgeIter.next();
                    edge.color = colorHolder.color;
                }
            }
            // "Teardrop" case
            else if (corners.size() == 1) {
                ColorHolder[] colors = new ColorHolder[3];
                switchColor(colorHolder, seedHolder);
                colors[0].color = colorHolder.color;
                colors[1].color = WHITE.getValue().color;
                switchColor(colorHolder, seedHolder);
                colors[2].color = colorHolder.color;

                int corner = corners.get(0);
                if (contour.edges.size() >= 3) {
                    int m = contour.edges.size();
                    for (int i = 0; i < m; ++i)
                        contour.edges.get((corner + i) % m).color = colors[1 + symmetricalTrichotomy(i, m)].color;
                } else if (contour.edges.size() >= 1) {
                    // Less than three edge segments for three colors => edges must be split
                    EdgeSegment[] parts = new EdgeSegment[7];
                    EdgeSegment[] firstSplit = new EdgeSegment[3];
                    contour.edges.get(0).edge.splitInThirds(firstSplit);
                    parts[0 + 3 * corner] = firstSplit[0];
                    parts[1 + 3 * corner] = firstSplit[1];
                    parts[2 + 3 * corner] = firstSplit[2];
                    if (contour.edges.size() >= 2) {
                        EdgeSegment[] secondSplit = new EdgeSegment[3];
                        contour.edges.get(1).edge.splitInThirds(secondSplit);
                        parts[3 - 3 * corner] = secondSplit[0];
                        parts[4 - 3 * corner] = secondSplit[1];
                        parts[5 - 3 * corner] = secondSplit[2];

                        // Fixed: access edgeColor property correctly
                        parts[0].edgeColor.color = colors[0].color;
                        parts[1].edgeColor.color = colors[0].color;
                        parts[2].edgeColor.color = colors[1].color;
                        parts[3].edgeColor.color = colors[1].color;
                        parts[4].edgeColor.color = colors[2].color;
                        parts[5].edgeColor.color = colors[2].color;
                    } else {
                        parts[0].edgeColor.color = colors[0].color;
                        parts[1].edgeColor.color = colors[1].color;
                        parts[2].edgeColor.color = colors[2].color;
                    }
                    contour.edges.clear();
                    for (int i = 0; i < parts.length && parts[i] != null; ++i)
                        contour.edges.add(new EdgeHolder(parts[i]));
                }
            }
            // Multiple corners
            else {
                int cornerCount = corners.size();
                int spline = 0;
                int start = corners.get(0);
                int m = contour.edges.size();
                switchColor(colorHolder, seedHolder);
                ColorHolder initialColor = colorHolder;
                for (int i = 0; i < m; ++i) {
                    int index = (start + i) % m;
                    if (spline + 1 < cornerCount && corners.get(spline + 1) == index) {
                        ++spline;
                        switchColor(colorHolder, seedHolder, new ColorHolder((spline == cornerCount - 1) ? initialColor.color : 0).color);
                    }
                    contour.edges.get(index).color = colorHolder.color;
                }
            }
        }
    }
    private static void switchColor(ColorHolder ColorHolder, SeedHolder seedHolder) {
        int shifted = ColorHolder.color << (1 + seedExtract2(seedHolder));
        ColorHolder.color = (shifted | (shifted >> 3)) & WHITE.getValue().color; // Now WHITE = 7, so this works correctly
    }
    private static void switchColor(ColorHolder ColorHolder, SeedHolder seedHolder, int banned) {
        int combined = ColorHolder.color & banned;
        if (combined == RED.getValue().color || combined == GREEN.getValue().color || combined == BLUE.getValue().color) {
            ColorHolder.color = combined ^ WHITE.getValue().color;
        } else {
            switchColor(ColorHolder, seedHolder);
        }
    }
}
