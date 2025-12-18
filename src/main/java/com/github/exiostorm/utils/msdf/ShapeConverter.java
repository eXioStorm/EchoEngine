package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;

import java.awt.geom.PathIterator;

public class ShapeConverter {
    public static MsdfShape fromAwtShape(java.awt.Shape awtShape) {
        MsdfShape msdfShape = new MsdfShape();

        PathIterator it = awtShape.getPathIterator(null);
        double[] coords = new double[6];

        Vector2d last = null;
        Vector2d moveStart = null;
        Contours.Contour contour = null;

        // For merging collinear line segments
        Vector2d pendingLineStart = null;
        Vector2d pendingLineEnd = null;

        while (!it.isDone()) {
            int type = it.currentSegment(coords);

            switch (type) {
                case PathIterator.SEG_MOVETO: {
                    // Flush any pending line
                    if (pendingLineStart != null && contour != null) {
                        contour.edges.add(new EdgeHolder(
                                EdgeSegment.create(pendingLineStart, pendingLineEnd, EdgeColorEnum.WHITE.getValue())
                        ));
                        pendingLineStart = null;
                    }

                    if (contour != null && !contour.edges.isEmpty())
                        closeIfNeeded(contour, moveStart);

                    contour = msdfShape.addContour();
                    moveStart = new Vector2d(coords[0], coords[1]);
                    last = new Vector2d(moveStart.x, moveStart.y);
                    break;
                }

                case PathIterator.SEG_LINETO: {
                    Vector2d p = new Vector2d(coords[0], coords[1]);

                    if (last.equals(p, 1e-9)) {
                        break; // Skip zero-length segments
                    }

                    // Try to merge collinear lines
                    if (pendingLineStart != null) {
                        Vector2d v1 = new Vector2d(pendingLineEnd).sub(pendingLineStart).normalize();
                        Vector2d v2 = new Vector2d(p).sub(pendingLineEnd).normalize();

                        double dot = v1.dot(v2);
                        if (Math.abs(dot - 1.0) < 0.001) { // Collinear threshold
                            // Extend the line
                            pendingLineEnd = p;
                            last = p;
                            break;
                        } else {
                            // Flush the pending line
                            contour.edges.add(new EdgeHolder(
                                    EdgeSegment.create(pendingLineStart, pendingLineEnd, EdgeColorEnum.WHITE.getValue())
                            ));
                        }
                    }

                    // Start new pending line
                    pendingLineStart = last;
                    pendingLineEnd = p;
                    last = p;
                    break;
                }

                case PathIterator.SEG_QUADTO: {
                    // Flush any pending line
                    if (pendingLineStart != null) {
                        contour.edges.add(new EdgeHolder(
                                EdgeSegment.create(pendingLineStart, pendingLineEnd, EdgeColorEnum.WHITE.getValue())
                        ));
                        pendingLineStart = null;
                    }

                    Vector2d c = new Vector2d(coords[0], coords[1]);
                    Vector2d p = new Vector2d(coords[2], coords[3]);
                    contour.edges.add(new EdgeHolder(
                            EdgeSegment.create(last, c, p, EdgeColorEnum.WHITE.getValue())
                    ));
                    last = p;
                    break;
                }

                case PathIterator.SEG_CUBICTO: {
                    // Flush any pending line
                    if (pendingLineStart != null) {
                        contour.edges.add(new EdgeHolder(
                                EdgeSegment.create(pendingLineStart, pendingLineEnd, EdgeColorEnum.WHITE.getValue())
                        ));
                        pendingLineStart = null;
                    }

                    Vector2d c1 = new Vector2d(coords[0], coords[1]);
                    Vector2d c2 = new Vector2d(coords[2], coords[3]);
                    Vector2d p = new Vector2d(coords[4], coords[5]);
                    contour.edges.add(new EdgeHolder(
                            EdgeSegment.create(last, c1, c2, p, EdgeColorEnum.WHITE.getValue())
                    ));
                    last = p;
                    break;
                }

                case PathIterator.SEG_CLOSE: {
                    // Flush any pending line
                    if (pendingLineStart != null) {
                        contour.edges.add(new EdgeHolder(
                                EdgeSegment.create(pendingLineStart, pendingLineEnd, EdgeColorEnum.WHITE.getValue())
                        ));
                        pendingLineStart = null;
                    }

                    closeIfNeeded(contour, moveStart);
                    last = new Vector2d(moveStart.x, moveStart.y);
                    break;
                }
            }

            it.next();
        }

        // Flush final pending line
        if (pendingLineStart != null && contour != null) {
            contour.edges.add(new EdgeHolder(
                    EdgeSegment.create(pendingLineStart, pendingLineEnd, EdgeColorEnum.WHITE.getValue())
            ));
        }

        if (contour != null && !contour.edges.isEmpty())
            closeIfNeeded(contour, moveStart);

        msdfShape.orientContours();

        return msdfShape;
    }

    private static void closeIfNeeded(Contours.Contour contour, Vector2d moveStart) {
        if (contour == null || contour.edges.isEmpty())
            return;

        EdgeSegment lastSeg = contour.edges.getLast().edge;
        Vector2d end = lastSeg.point(1.0);

        if (!end.equals(moveStart, 1e-12)) {
            contour.edges.add(new EdgeHolder(
                    EdgeSegment.create(end, moveStart, EdgeColorEnum.WHITE.getValue())
            ));
        }
    }
}