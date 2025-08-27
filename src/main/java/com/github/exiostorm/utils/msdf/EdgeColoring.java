package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

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
        final int[] colors = { CYAN.getValue(), MAGENTA.getValue(), YELLOW.getValue() };
        return colors[seedExtract3(seedHolder)];
    }

    void edgeColoringSimple(java.util.List<Contours.Contour> contours, double angleThreshold, SeedHolder seedHolder) {
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
                colors[1].color = WHITE.getValue();
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
        ColorHolder.color = (shifted | (shifted >> 3)) & WHITE.getValue(); // Now WHITE = 7, so this works correctly
    }
    private static void switchColor(ColorHolder ColorHolder, SeedHolder seedHolder, int banned) {
        int combined = ColorHolder.color & banned;
        if (combined == RED.getValue() || combined == GREEN.getValue() || combined == BLUE.getValue()) {
            ColorHolder.color = combined ^ WHITE.getValue();
        } else {
            switchColor(ColorHolder, seedHolder);
        }
    }
}
