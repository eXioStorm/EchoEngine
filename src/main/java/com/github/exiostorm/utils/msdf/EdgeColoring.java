package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2f;

import java.awt.*;

import static com.github.exiostorm.utils.msdf.enums.EdgeColorEnum.*;

public class EdgeColoring {
    static int seedExtract3(Seed seed) {
        int v = (int)(seed.value % 3);
        seed.value /= 3;
        return v;
    }
    static EdgeColorEnum initColor(Seed seed) {
        final EdgeColorEnum[] colors = { EdgeColorEnum.CYAN, EdgeColorEnum.MAGENTA, EdgeColorEnum.YELLOW };
        return colors[seedExtract3(seed)];
    }

    void edgeColoringSimple(Shape shape, double angleThreshold, Seed seed) {
        double crossThreshold = Math.sin(angleThreshold);
        EdgeColorEnum color = initColor(seed);
        java.util.List<Integer> corners = new java.util.ArrayList<>();

        for (java.util.Iterator<Contours.Contour> contourIter = shape.contours.iterator(); contourIter.hasNext();) {
            Contours.Contour contour = contourIter.next();
            if (contour.edges.isEmpty())
                continue;

            { // Identify corners
                corners.clear();
                Vector2f prevDirection = contour.edges(contour.edges.size() - 1).direction(1);
                int index = 0;
                for (java.util.Iterator<EdgeHolder> edgeIter = contour.edges.iterator(); edgeIter.hasNext(); index++) {
                    EdgeHolder edge = edgeIter.next();
                    if (isCorner(prevDirection.normalize(), edge.direction(0).normalize(), crossThreshold))
                        corners.add(index);
                    prevDirection = edge.direction(1);
                }
            }

            // Smooth contour
            if (corners.isEmpty()) {
                switchColor(color, seed);
                for (java.util.Iterator<EdgeHolder> edgeIter = contour.edges.iterator(); edgeIter.hasNext();) {
                    EdgeHolder edge = edgeIter.next();
                    edge.color = color;
                }
            }
            // "Teardrop" case
            else if (corners.size() == 1) {
                EdgeColorEnum[] colors = new EdgeColorEnum[3];
                switchColor(color, seed);
                colors[0] = color;
                colors[1] = WHITE;
                switchColor(color, seed);
                colors[2] = color;

                int corner = corners.get(0);
                if (contour.edges.size() >= 3) {
                    int m = contour.edges.size();
                    for (int i = 0; i < m; ++i)
                        contour.edges.get((corner + i) % m).color = colors[1 + symmetricalTrichotomy(i, m)];
                } else if (contour.edges.size() >= 1) {
                    // Less than three edge segments for three colors => edges must be split
                    EdgeSegment[] parts = new EdgeSegment[7];
                    contour.edges.get(0).splitInThirds(parts[0 + 3 * corner], parts[1 + 3 * corner], parts[2 + 3 * corner]);
                    if (contour.edges.size() >= 2) {
                        contour.edges.get(1).splitInThirds(parts[3 - 3 * corner], parts[4 - 3 * corner], parts[5 - 3 * corner]);
                        parts[0].color = parts[1].color = colors[0];
                        parts[2].color = parts[3].color = colors[1];
                        parts[4].color = parts[5].color = colors[2];
                    } else {
                        parts[0].color = colors[0];
                        parts[1].color = colors[1];
                        parts[2].color = colors[2];
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
                switchColor(color, seed);
                EdgeColorEnum initialColor = color;
                for (int i = 0; i < m; ++i) {
                    int index = (start + i) % m;
                    if (spline + 1 < cornerCount && corners.get(spline + 1) == index) {
                        ++spline;
                        switchColor(color, seed, new EdgeColorEnum((spline == cornerCount - 1) ? initialColor : null));
                    }
                    contour.edges.get(index).color = color;
                }
            }
        }
    }
}
