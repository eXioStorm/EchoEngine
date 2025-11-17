package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;

import java.awt.geom.PathIterator;

public class ShapeConverter {

    public static MsdfShape fromAwtShape(java.awt.Shape awtShape) {

        MsdfShape msdfShape =
                new MsdfShape();

        PathIterator it = awtShape.getPathIterator(null, 0.01);
        double[] coords = new double[6];

        Vector2d last = null;
        Vector2d moveStart = null;

        Contours.Contour contour = null;

        while (!it.isDone()) {
            int type = it.currentSegment(coords);

            switch (type) {

                case PathIterator.SEG_MOVETO: {
                    if (contour != null && !contour.edges.isEmpty())
                        closeIfNeeded(contour, moveStart);

                    contour = msdfShape.addContour();

                    moveStart = new Vector2d(coords[0], coords[1]);
                    last = new Vector2d(coords[0], coords[1]);
                    break;
                }

                case PathIterator.SEG_LINETO: {
                    Vector2d p = new Vector2d(coords[0], coords[1]);
                    contour.edges.add(new EdgeHolder(
                            EdgeSegment.create(last, p, EdgeColorEnum.WHITE.getValue())
                    ));
                    last = p;
                    break;
                }

                case PathIterator.SEG_QUADTO: {
                    Vector2d c = new Vector2d(coords[0], coords[1]);
                    Vector2d p = new Vector2d(coords[2], coords[3]);
                    contour.edges.add(new EdgeHolder(
                            EdgeSegment.create(last, c, p, EdgeColorEnum.WHITE.getValue())
                    ));
                    last = p;
                    break;
                }

                case PathIterator.SEG_CUBICTO: {
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
                    closeIfNeeded(contour, moveStart);
                    last = moveStart;
                    break;
                }
            }

            it.next();
        }

        if (contour != null && !contour.edges.isEmpty())
            closeIfNeeded(contour, moveStart);

        msdfShape.orientContours(); // required by MSDF

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