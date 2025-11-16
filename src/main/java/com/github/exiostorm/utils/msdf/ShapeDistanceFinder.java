package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * Finds the distance between a point and a Shape. ContourCombiner dictates the distance metric and its data type.
 * @param <C> The ContourCombiner type that determines the distance calculation method
 */
public class ShapeDistanceFinder<C extends ContourCombiners.ContourCombiner<D>, D> {

    private final Shape shape;
    private final C contourCombiner;
    private final List<EdgeHolder> shapeEdgeCache;

    /**
     * Constructor - Passed shape object must persist until the distance finder is destroyed!
     */
    public ShapeDistanceFinder(Shape shape, C contourCombiner) {
        this.shape = shape;
        this.contourCombiner = contourCombiner;
        this.shapeEdgeCache = new ArrayList<>(shape.edgeCount());
        for (int i = 0; i < shape.edgeCount(); i++) {
            this.shapeEdgeCache.add(new EdgeHolder());
        }
    }

    /**
     * Finds the distance from origin. Not thread-safe! Is fastest when subsequent queries are close together.
     */
    public D distance(Vector2d origin) {
        contourCombiner.reset(origin);

        int edgeCacheIndex = 0;

        for (int contourIndex = 0; contourIndex < shape.contours.size(); contourIndex++) {
            Contours.Contour contour = shape.contours.get(contourIndex);
            if (!contour.edges.isEmpty()) {
                Object edgeSelector = contourCombiner.edgeSelector(contourIndex);

                List<EdgeHolder> edges = contour.edges;
                EdgeSegment prevEdge = edges.size() >= 2 ?
                        edges.get(edges.size() - 2).getEdgeSegment() :
                        edges.get(0).getEdgeSegment();
                EdgeSegment curEdge = edges.get(edges.size() - 1).getEdgeSegment();

                for (EdgeHolder edgeHolder : edges) {
                    EdgeSegment nextEdge = edgeHolder.getEdgeSegment();

                    // Ensure we don't exceed cache bounds
                    if (edgeCacheIndex < shapeEdgeCache.size()) {
                        Object edgeCache = shapeEdgeCache.get(edgeCacheIndex);
                        addEdgeToSelector(edgeSelector, edgeCache, prevEdge, curEdge, nextEdge);
                        edgeCacheIndex++;
                    }

                    prevEdge = curEdge;
                    curEdge = nextEdge;
                }
            }
        }

        return contourCombiner.distance();
    }
    public List<Object> getShapeEdgeCache() {
        return Collections.singletonList(shapeEdgeCache);
    }
    private void addEdgeToSelector(Object edgeSelector, Object edgeCache, EdgeSegment prevEdge, EdgeSegment curEdge, EdgeSegment nextEdge) {
        if (edgeSelector instanceof EdgeSelectors.TrueDistanceSelector &&
                edgeCache instanceof EdgeSelectors.TrueDistanceSelector.EdgeCache) {
            ((EdgeSelectors.TrueDistanceSelector) edgeSelector).addEdge(
                    (EdgeSelectors.TrueDistanceSelector.EdgeCache) edgeCache, prevEdge, curEdge, nextEdge);
        } else if (edgeSelector instanceof EdgeSelectors.MultiDistanceSelector &&
                edgeCache instanceof EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) {
            ((EdgeSelectors.MultiDistanceSelector) edgeSelector).addEdge(
                    (EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) edgeCache, prevEdge, curEdge, nextEdge);
        } else if (edgeSelector instanceof EdgeSelectors.MultiAndTrueDistanceSelector &&
                edgeCache instanceof EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) {
            ((EdgeSelectors.MultiAndTrueDistanceSelector) edgeSelector).addEdge(
                    (EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) edgeCache, prevEdge, curEdge, nextEdge);
        } else if (edgeSelector instanceof EdgeSelectors.PerpendicularDistanceSelector &&
                edgeCache instanceof EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) {
            ((EdgeSelectors.PerpendicularDistanceSelector) edgeSelector).addEdge(
                    (EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) edgeCache, prevEdge, curEdge, nextEdge);
        } else {
            System.err.println("Warning: Unsupported edge selector/cache combination: " +
                    edgeSelector.getClass().getSimpleName() + " with cache " +
                    (edgeCache != null ? edgeCache.getClass().getSimpleName() : "null"));
        }
    }

    /**
     * Finds the distance between shape and origin. Does not allocate result cache used to optimize performance of multiple queries.
     */
    public static <C extends ContourCombiners.ContourCombiner<D>, D>
    D oneShotDistance(Shape shape, Vector2d origin, C contourCombiner) {
        contourCombiner.reset(origin);
        for (int contourIndex = 0; contourIndex < shape.contours.size(); contourIndex++) {
            Contours.Contour contour = shape.contours.get(contourIndex);
            if (!contour.edges.isEmpty()) {
                Object edgeSelector = contourCombiner.edgeSelector(contourIndex);
                List<EdgeHolder> edges = contour.edges;
                EdgeSegment prevEdge = edges.size() >= 2 ?
                        edges.get(edges.size() - 2).getEdgeSegment() :
                        edges.get(0).getEdgeSegment();
                EdgeSegment curEdge = edges.get(edges.size() - 1).getEdgeSegment();
                for (EdgeHolder edgeHolder : edges) {
                    EdgeSegment nextEdge = edgeHolder.getEdgeSegment();
                    Object tempCache = null;

                    if (edgeSelector instanceof EdgeSelectors.TrueDistanceSelector) {
                        tempCache = new EdgeSelectors.TrueDistanceSelector.EdgeCache();
                        ((EdgeSelectors.TrueDistanceSelector) edgeSelector).addEdge(
                                (EdgeSelectors.TrueDistanceSelector.EdgeCache) tempCache, prevEdge, curEdge, nextEdge);
                    } else if (edgeSelector instanceof EdgeSelectors.MultiDistanceSelector) {
                        tempCache = new EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache();
                        ((EdgeSelectors.MultiDistanceSelector) edgeSelector).addEdge(
                                (EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) tempCache, prevEdge, curEdge, nextEdge);
                    } else if (edgeSelector instanceof EdgeSelectors.MultiAndTrueDistanceSelector) {
                        tempCache = new EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache();
                        ((EdgeSelectors.MultiAndTrueDistanceSelector) edgeSelector).addEdge(
                                (EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) tempCache, prevEdge, curEdge, nextEdge);
                    } else if (edgeSelector instanceof EdgeSelectors.PerpendicularDistanceSelector) {
                        tempCache = new EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache();
                        ((EdgeSelectors.PerpendicularDistanceSelector) edgeSelector).addEdge(
                                (EdgeSelectors.PerpendicularDistanceSelectorBase.EdgeCache) tempCache, prevEdge, curEdge, nextEdge);
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

    public D getContourCombiner() {
        return contourCombiner.distance();
    }
}

/**
 * Specialized ShapeDistanceFinder for simple true distance calculations
 */
class SimpleTrueShapeDistanceFinder
        extends ShapeDistanceFinder<ContourCombiners.SimpleContourCombiner, Double> {

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
