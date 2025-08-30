package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;
import java.util.List;
import java.util.ArrayList;

/**
 * Finds the distance between a point and a Shape. ContourCombiner dictates the distance metric and its data type.
 * @param <T> The ContourCombiner type that determines the distance calculation method
 */
public class ShapeDistanceFinder<T extends ContourCombiners.ContourCombiner> {

    private final Shape shape;
    private final T contourCombiner;
    private final List<EdgeHolder> shapeEdgeHolder;

    /**
     * Constructor - Passed shape object must persist until the distance finder is destroyed!
     */
    public ShapeDistanceFinder(Shape shape, T contourCombiner) {
        this.shape = shape;
        this.contourCombiner = contourCombiner;
        this.shapeEdgeHolder = new ArrayList<>(shape.edgeCount());

        // Initialize edge cache
        for (int i = 0; i < shape.edgeCount(); i++) {
            this.shapeEdgeHolder.add(new EdgeHolder());
        }
    }

    /**
     * Finds the distance from origin. Not thread-safe! Is fastest when subsequent queries are close together.
     */
    public double distance(Vector2d origin) {
        contourCombiner.reset(origin);

        int EdgeHolderIndex = 0;

        for (Contours.Contour contour : shape.contours) {
            if (!contour.edges.isEmpty()) {
                Object edgeSelector = contourCombiner.edgeSelector(shape.contours.indexOf(contour));

                List<EdgeHolder> edges = contour.edges;
                EdgeSegment prevEdge = edges.size() >= 2 ?
                        edges.get(edges.size() - 2).edge :
                        edges.get(0).edge;
                EdgeSegment curEdge = edges.get(edges.size() - 1).edge;

                for (EdgeHolder edgeHolder : edges) {
                    EdgeSegment nextEdge = edgeHolder.edge;
                    edgeSelector.addEdge(shapeEdgeHolder.get(EdgeHolderIndex++), prevEdge, curEdge, nextEdge);
                    prevEdge = curEdge;
                    curEdge = nextEdge;
                }
            }
        }

        return contourCombiner.distance();
    }

    /**
     * Finds the distance between shape and origin. Does not allocate result cache used to optimize performance of multiple queries.
     */
    public static Object oneShotDistance(Shape shape, Vector2d origin, ContourCombiners.ContourCombiner contourCombiner) {
        contourCombiner.reset(origin);

        for (Contours.Contour contour : shape.contours) {
            if (!contour.edges.isEmpty()) {
                Object edgeSelector = contourCombiner.edgeSelector(shape.contours.indexOf(contour));

                List<EdgeHolder> edges = contour.edges;
                EdgeSegment prevEdge = edges.size() >= 2 ?
                        edges.get(edges.size() - 2).edge :
                        edges.get(0).edge;
                EdgeSegment curEdge = edges.get(edges.size() - 1).edge;

                for (EdgeHolder edgeHolder : edges) {
                    EdgeSegment nextEdge = edgeHolder.edge;
                    EdgeHolder dummy = new EdgeHolder();

                    // Call addEdge based on the selector type
                    if (edgeSelector instanceof EdgeSelectors.TrueDistanceSelector) {
                        ((EdgeSelectors.TrueDistanceSelector) edgeSelector).addEdge(
                                dummy, prevEdge, curEdge, nextEdge);
                    } else if (edgeSelector instanceof EdgeSelectors.MultiDistanceSelector) {
                        ((EdgeSelectors.MultiDistanceSelector) edgeSelector).addEdge(
                                dummy, prevEdge, curEdge, nextEdge);
                    } else if (edgeSelector instanceof EdgeSelectors.MultiAndTrueDistanceSelector) {
                        ((EdgeSelectors.MultiAndTrueDistanceSelector) edgeSelector).addEdge(
                                dummy, prevEdge, curEdge, nextEdge);
                    }

                    prevEdge = curEdge;
                    curEdge = nextEdge;
                }
            }
        }

        return contourCombiner.distance();
    }

    // Getters
    public Shape getShape() {
        return shape;
    }

    public T getContourCombiner() {
        return contourCombiner;
    }
}

/**
 * Specialized ShapeDistanceFinder for simple true distance calculations
 */
class SimpleTrueShapeDistanceFinder extends ShapeDistanceFinder<ContourCombiners.SimpleContourCombiner> {

    public SimpleTrueShapeDistanceFinder(Shape shape) {
        super(shape, new ContourCombiners.SimpleContourCombiner(shape));
    }

    /**
     * Static convenience method for one-shot distance calculations
     */
    public static double oneShotDistance(Shape shape, Vector2d origin) {
        ContourCombiners.SimpleContourCombiner combiner = new ContourCombiners.SimpleContourCombiner(shape);
        return (double) ShapeDistanceFinder.oneShotDistance(shape, origin, combiner);
    }
}
