package com.github.exiostorm.utils.msdf;


import org.joml.Vector2d;
import java.util.ArrayList;
import java.util.List;

import static com.github.exiostorm.utils.msdf.MathUtils.median;

public class ContourCombiners {

    /**
     * Base interface for all contour combiners
     */
    public interface ContourCombiner<T> {
        void reset(Vector2d p);
        Object edgeSelector(int contourIndex); // use Object or a generic bound if needed
        T distance();
    }


    /**
     * Simply selects the nearest contour.
     */
    public static class SimpleContourCombiner implements ContourCombiner<Double> {

        private EdgeSelectors.TrueDistanceSelector shapeEdgeSelector;

        public SimpleContourCombiner(Shape shape) {
            this.shapeEdgeSelector = new EdgeSelectors.TrueDistanceSelector();
        }

        public void reset(Vector2d p) {
            shapeEdgeSelector.reset(p);
        }

        public EdgeSelectors.TrueDistanceSelector edgeSelector(int i) {
            return shapeEdgeSelector;
        }

        public Double distance() {
            return shapeEdgeSelector.distance();
        }
    }

    /**
     * Multi-distance version of SimpleContourCombiner
     */
    public static class SimpleMultiContourCombiner implements ContourCombiner<EdgeSelectors.MultiDistance> {

        private EdgeSelectors.MultiDistanceSelector shapeEdgeSelector;

        public SimpleMultiContourCombiner(Shape shape) {
            this.shapeEdgeSelector = new EdgeSelectors.MultiDistanceSelector();
        }

        public void reset(Vector2d p) {
            shapeEdgeSelector.reset(p);
        }

        public EdgeSelectors.MultiDistanceSelector edgeSelector(int i) {
            return shapeEdgeSelector;
        }

        public EdgeSelectors.MultiDistance distance() {
            return shapeEdgeSelector.distance();
        }
    }

    /**
     * Multi and true distance version of SimpleContourCombiner
     */
    public static class SimpleMultiAndTrueContourCombiner implements ContourCombiner<EdgeSelectors.MultiAndTrueDistance> {

        private EdgeSelectors.MultiAndTrueDistanceSelector shapeEdgeSelector;

        public SimpleMultiAndTrueContourCombiner(Shape shape) {
            this.shapeEdgeSelector = new EdgeSelectors.MultiAndTrueDistanceSelector();
        }

        public void reset(Vector2d p) {
            shapeEdgeSelector.reset(p);
        }

        public EdgeSelectors.MultiAndTrueDistanceSelector edgeSelector(int i) {
            return shapeEdgeSelector;
        }

        public EdgeSelectors.MultiAndTrueDistance distance() {
            return shapeEdgeSelector.distance();
        }
    }

    /**
     * Selects the nearest contour that actually forms a border between filled and unfilled area.
     */
    public static class OverlappingContourCombiner implements ContourCombiner<Double> {

        private Vector2d p;
        private List<Integer> windings;
        private List<EdgeSelectors.TrueDistanceSelector> edgeSelectors;

        public OverlappingContourCombiner(Shape shape) {
            this.p = new Vector2d();
            this.windings = new ArrayList<>();
            this.edgeSelectors = new ArrayList<>();

            // Initialize windings from contours
            for (Contours.Contour contour : shape.contours) {
                windings.add(contour.winding());
                edgeSelectors.add(new EdgeSelectors.TrueDistanceSelector());
            }
        }

        public void reset(Vector2d p) {
            this.p.set(p);
            for (EdgeSelectors.TrueDistanceSelector contourEdgeSelector : edgeSelectors) {
                contourEdgeSelector.reset(p);
            }
        }

        public EdgeSelectors.TrueDistanceSelector edgeSelector(int i) {
            return edgeSelectors.get(i);
        }

        public Double distance() {
            int contourCount = edgeSelectors.size();

            if (contourCount == 0) {
                return -Double.MAX_VALUE;
            }

            EdgeSelectors.TrueDistanceSelector shapeEdgeSelector = new EdgeSelectors.TrueDistanceSelector();
            EdgeSelectors.TrueDistanceSelector innerEdgeSelector = new EdgeSelectors.TrueDistanceSelector();
            EdgeSelectors.TrueDistanceSelector outerEdgeSelector = new EdgeSelectors.TrueDistanceSelector();

            shapeEdgeSelector.reset(p);
            innerEdgeSelector.reset(p);
            outerEdgeSelector.reset(p);

            for (int i = 0; i < contourCount; i++) {
                double edgeDistance = edgeSelectors.get(i).distance();
                shapeEdgeSelector.merge(edgeSelectors.get(i));

                if (windings.get(i) > 0 && edgeDistance >= 0) {
                    innerEdgeSelector.merge(edgeSelectors.get(i));
                }
                if (windings.get(i) < 0 && edgeDistance <= 0) {
                    outerEdgeSelector.merge(edgeSelectors.get(i));
                }
            }

            double shapeDistance = shapeEdgeSelector.distance();
            double innerDistance = innerEdgeSelector.distance();
            double outerDistance = outerEdgeSelector.distance();
            double distance = -Double.MAX_VALUE;

            int winding = 0;
            if (innerDistance >= 0 && Math.abs(innerDistance) <= Math.abs(outerDistance)) {
                distance = innerDistance;
                winding = 1;
                for (int i = 0; i < contourCount; i++) {
                    if (windings.get(i) > 0) {
                        double contourDistance = edgeSelectors.get(i).distance();
                        if (Math.abs(contourDistance) < Math.abs(outerDistance) && contourDistance > distance) {
                            distance = contourDistance;
                        }
                    }
                }
            } else if (outerDistance <= 0 && Math.abs(outerDistance) < Math.abs(innerDistance)) {
                distance = outerDistance;
                winding = -1;
                for (int i = 0; i < contourCount; i++) {
                    if (windings.get(i) < 0) {
                        double contourDistance = edgeSelectors.get(i).distance();
                        if (Math.abs(contourDistance) < Math.abs(innerDistance) && contourDistance < distance) {
                            distance = contourDistance;
                        }
                    }
                }
            } else {
                return shapeDistance;
            }

            for (int i = 0; i < contourCount; i++) {
                if (windings.get(i) != winding) {
                    double contourDistance = edgeSelectors.get(i).distance();
                    if (contourDistance * distance >= 0 && Math.abs(contourDistance) < Math.abs(distance)) {
                        distance = contourDistance;
                    }
                }
            }

            if (distance == shapeDistance) {
                distance = shapeDistance;
            }

            return distance;
        }
    }

    /**
     * Multi-distance version of OverlappingContourCombiner
     */
    public static class OverlappingMultiContourCombiner implements ContourCombiner<EdgeSelectors.MultiDistance> {

        private Vector2d p;
        private List<Integer> windings;
        private List<EdgeSelectors.MultiDistanceSelector> edgeSelectors;

        public OverlappingMultiContourCombiner(Shape shape) {
            this.p = new Vector2d();
            this.windings = new ArrayList<>();
            this.edgeSelectors = new ArrayList<>();

            for (Contours.Contour contour : shape.contours) {
                windings.add(contour.winding());
                edgeSelectors.add(new EdgeSelectors.MultiDistanceSelector());
            }
        }

        public void reset(Vector2d p) {
            this.p.set(p);
            for (EdgeSelectors.MultiDistanceSelector contourEdgeSelector : edgeSelectors) {
                contourEdgeSelector.reset(p);
            }
        }

        public EdgeSelectors.MultiDistanceSelector edgeSelector(int i) {
            return edgeSelectors.get(i);
        }

        public EdgeSelectors.MultiDistance distance() {
            int contourCount = edgeSelectors.size();

            if (contourCount == 0) {
                EdgeSelectors.MultiDistance md = new EdgeSelectors.MultiDistance();
                md.r = md.g = md.b = -Double.MAX_VALUE;
                return md;
            }

            EdgeSelectors.MultiDistanceSelector shapeEdgeSelector = new EdgeSelectors.MultiDistanceSelector();
            EdgeSelectors.MultiDistanceSelector innerEdgeSelector = new EdgeSelectors.MultiDistanceSelector();
            EdgeSelectors.MultiDistanceSelector outerEdgeSelector = new EdgeSelectors.MultiDistanceSelector();

            shapeEdgeSelector.reset(p);
            innerEdgeSelector.reset(p);
            outerEdgeSelector.reset(p);

            for (int i = 0; i < contourCount; i++) {
                EdgeSelectors.MultiDistance edgeDistance = edgeSelectors.get(i).distance();
                shapeEdgeSelector.merge(edgeSelectors.get(i));

                double resolvedDistance = median(edgeDistance.r, edgeDistance.g, edgeDistance.b);
                if (windings.get(i) > 0 && resolvedDistance >= 0) {
                    innerEdgeSelector.merge(edgeSelectors.get(i));
                }
                if (windings.get(i) < 0 && resolvedDistance <= 0) {
                    outerEdgeSelector.merge(edgeSelectors.get(i));
                }
            }

            EdgeSelectors.MultiDistance shapeDistance = shapeEdgeSelector.distance();
            EdgeSelectors.MultiDistance innerDistance = innerEdgeSelector.distance();
            EdgeSelectors.MultiDistance outerDistance = outerEdgeSelector.distance();

            double innerScalarDistance = median(innerDistance.r, innerDistance.g, innerDistance.b);
            double outerScalarDistance = median(outerDistance.r, outerDistance.g, outerDistance.b);

            EdgeSelectors.MultiDistance distance = new EdgeSelectors.MultiDistance();
            distance.r = distance.g = distance.b = -Double.MAX_VALUE;

            int winding = 0;
            if (innerScalarDistance >= 0 && Math.abs(innerScalarDistance) <= Math.abs(outerScalarDistance)) {
                distance = innerDistance;
                winding = 1;
                for (int i = 0; i < contourCount; i++) {
                    if (windings.get(i) > 0) {
                        EdgeSelectors.MultiDistance contourDistance = edgeSelectors.get(i).distance();
                        double contourScalar = median(contourDistance.r, contourDistance.g, contourDistance.b);
                        double distanceScalar = median(distance.r, distance.g, distance.b);
                        if (Math.abs(contourScalar) < Math.abs(outerScalarDistance) && contourScalar > distanceScalar) {
                            distance = contourDistance;
                        }
                    }
                }
            } else if (outerScalarDistance <= 0 && Math.abs(outerScalarDistance) < Math.abs(innerScalarDistance)) {
                distance = outerDistance;
                winding = -1;
                for (int i = 0; i < contourCount; i++) {
                    if (windings.get(i) < 0) {
                        EdgeSelectors.MultiDistance contourDistance = edgeSelectors.get(i).distance();
                        double contourScalar = median(contourDistance.r, contourDistance.g, contourDistance.b);
                        double distanceScalar = median(distance.r, distance.g, distance.b);
                        if (Math.abs(contourScalar) < Math.abs(innerScalarDistance) && contourScalar < distanceScalar) {
                            distance = contourDistance;
                        }
                    }
                }
            } else {
                return shapeDistance;
            }

            double distanceScalar = median(distance.r, distance.g, distance.b);
            for (int i = 0; i < contourCount; i++) {
                if (windings.get(i) != winding) {
                    EdgeSelectors.MultiDistance contourDistance = edgeSelectors.get(i).distance();
                    double contourScalar = median(contourDistance.r, contourDistance.g, contourDistance.b);
                    if (contourScalar * distanceScalar >= 0 && Math.abs(contourScalar) < Math.abs(distanceScalar)) {
                        distance = contourDistance;
                        distanceScalar = contourScalar;
                    }
                }
            }

            double shapeScalar = median(shapeDistance.r, shapeDistance.g, shapeDistance.b);
            if (distanceScalar == shapeScalar) {
                distance = shapeDistance;
            }

            return distance;
        }
    }

    /**
     * Multi and true distance version of OverlappingContourCombiner
     */
    public static class OverlappingMultiAndTrueContourCombiner implements ContourCombiner<EdgeSelectors.MultiAndTrueDistance> {

        private Vector2d p;
        private List<Integer> windings;
        private List<EdgeSelectors.MultiAndTrueDistanceSelector> edgeSelectors;

        public OverlappingMultiAndTrueContourCombiner(Shape shape) {
            this.p = new Vector2d();
            this.windings = new ArrayList<>();
            this.edgeSelectors = new ArrayList<>();

            for (Contours.Contour contour : shape.contours) {
                windings.add(contour.winding());
                edgeSelectors.add(new EdgeSelectors.MultiAndTrueDistanceSelector());
            }
        }

        public void reset(Vector2d p) {
            this.p.set(p);
            for (EdgeSelectors.MultiAndTrueDistanceSelector contourEdgeSelector : edgeSelectors) {
                contourEdgeSelector.reset(p);
            }
        }

        public EdgeSelectors.MultiAndTrueDistanceSelector edgeSelector(int i) {
            return edgeSelectors.get(i);
        }

        public EdgeSelectors.MultiAndTrueDistance distance() {
            // Similar logic to OverlappingMultiContourCombiner but returns MultiAndTrueDistance
            OverlappingMultiContourCombiner multiCombiner = new OverlappingMultiContourCombiner(createShapeFromSelectors());

            // Copy state
            multiCombiner.p.set(this.p);
            multiCombiner.windings.clear();
            multiCombiner.windings.addAll(this.windings);

            // Convert selectors (this is a simplification - in practice you'd need proper conversion)
            EdgeSelectors.MultiDistance multiDist = multiCombiner.distance();
            EdgeSelectors.MultiAndTrueDistance result = new EdgeSelectors.MultiAndTrueDistance();
            result.r = multiDist.r;
            result.g = multiDist.g;
            result.b = multiDist.b;

            // Calculate true distance
            double minTrueDistance = Double.MAX_VALUE;
            for (EdgeSelectors.MultiAndTrueDistanceSelector selector : edgeSelectors) {
                EdgeSelectors.MultiAndTrueDistance dist = selector.distance();
                if (Math.abs(dist.a) < Math.abs(minTrueDistance)) {
                    minTrueDistance = dist.a;
                }
            }
            result.a = minTrueDistance;

            return result;
        }

        private Shape createShapeFromSelectors() {
            // This is a helper method that would need to be implemented based on your Shape class
            // For now, returning null as a placeholder
            return null;
        }
    }
}