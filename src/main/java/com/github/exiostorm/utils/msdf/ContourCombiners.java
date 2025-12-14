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

        public SimpleContourCombiner(MsdfShape msdfShape) {
            this.shapeEdgeSelector = new EdgeSelectors.TrueDistanceSelector();
        }

        public void reset(Vector2d p) {
            shapeEdgeSelector.reset(p);
        }

        public Object edgeSelector(int i) {
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

        public SimpleMultiContourCombiner(MsdfShape msdfShape) {
            this.shapeEdgeSelector = new EdgeSelectors.MultiDistanceSelector();
        }

        public void reset(Vector2d p) {
            shapeEdgeSelector.reset(p);
        }

        public Object edgeSelector(int i) {
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

        public SimpleMultiAndTrueContourCombiner(MsdfShape msdfShape) {
            this.shapeEdgeSelector = new EdgeSelectors.MultiAndTrueDistanceSelector();
        }

        public void reset(Vector2d p) {
            shapeEdgeSelector.reset(p);
        }

        public Object edgeSelector(int i) {
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

        public OverlappingContourCombiner(MsdfShape msdfShape) {
            this.p = new Vector2d();
            this.windings = new ArrayList<>();
            this.edgeSelectors = new ArrayList<>();

            // Initialize windings from contours
            for (Contours.Contour contour : msdfShape.contours) {
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

        public Object edgeSelector(int i) {
            return edgeSelectors.get(i);
        }

        public Double distance() {
            int contourCount = edgeSelectors.size();

            if (contourCount == 0) {
                return -Double.MAX_VALUE; // Changed from -Double.MAX_VALUE
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
            double distance;

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

        public OverlappingMultiContourCombiner(MsdfShape msdfShape) {
            this.p = new Vector2d();
            this.windings = new ArrayList<>();
            this.edgeSelectors = new ArrayList<>();

            for (Contours.Contour contour : msdfShape.contours) {
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

        public Object edgeSelector(int i) {
            return edgeSelectors.get(i);
        }

        public EdgeSelectors.MultiDistance distance() {
            int contourCount = edgeSelectors.size();

            if (contourCount == 0) {
                EdgeSelectors.MultiDistance md = new EdgeSelectors.MultiDistance();
                md.c = md.m = md.y = -Double.MAX_VALUE; // changed from -Double.MAX_VALUE
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

                double resolvedDistance = median(edgeDistance.c, edgeDistance.m, edgeDistance.y);
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

            double innerScalarDistance = median(innerDistance.c, innerDistance.m, innerDistance.y);
            double outerScalarDistance = median(outerDistance.c, outerDistance.m, outerDistance.y);

            EdgeSelectors.MultiDistance distance;

            int winding = 0;
            if (innerScalarDistance >= 0 && Math.abs(innerScalarDistance) <= Math.abs(outerScalarDistance)) {
                distance = innerDistance;
                winding = 1;
                for (int i = 0; i < contourCount; i++) {
                    if (windings.get(i) > 0) {
                        EdgeSelectors.MultiDistance contourDistance = edgeSelectors.get(i).distance();
                        double contourScalar = median(contourDistance.c, contourDistance.m, contourDistance.y);
                        double distanceScalar = median(distance.c, distance.m, distance.y);
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
                        double contourScalar = median(contourDistance.c, contourDistance.m, contourDistance.y);
                        double distanceScalar = median(distance.c, distance.m, distance.y);
                        if (Math.abs(contourScalar) < Math.abs(innerScalarDistance) && contourScalar < distanceScalar) {
                            distance = contourDistance;
                        }
                    }
                }
            } else {
                return shapeDistance;
            }

            double distanceScalar = median(distance.c, distance.m, distance.y);
            for (int i = 0; i < contourCount; i++) {
                if (windings.get(i) != winding) {
                    EdgeSelectors.MultiDistance contourDistance = edgeSelectors.get(i).distance();
                    double contourScalar = median(contourDistance.c, contourDistance.m, contourDistance.y);
                    if (contourScalar * distanceScalar >= 0 && Math.abs(contourScalar) < Math.abs(distanceScalar)) {
                        distance = contourDistance;
                        distanceScalar = contourScalar;
                    }
                }
            }

            double shapeScalar = median(shapeDistance.c, shapeDistance.m, shapeDistance.y);
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

        public OverlappingMultiAndTrueContourCombiner(MsdfShape msdfShape) {
            this.p = new Vector2d();
            this.windings = new ArrayList<>();
            this.edgeSelectors = new ArrayList<>();

            for (Contours.Contour contour : msdfShape.contours) {
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

        public Object edgeSelector(int i) {
            return edgeSelectors.get(i);
        }

        public EdgeSelectors.MultiAndTrueDistance distance() {
            int contourCount = edgeSelectors.size();

            if (contourCount == 0) {
                EdgeSelectors.MultiAndTrueDistance md = new EdgeSelectors.MultiAndTrueDistance();
                md.c = md.m = md.y = md.a = -Double.MAX_VALUE;  // Changed from -Double.MAX_VALUE
                return md;
            }

            EdgeSelectors.MultiAndTrueDistanceSelector shapeEdgeSelector = new EdgeSelectors.MultiAndTrueDistanceSelector();
            EdgeSelectors.MultiAndTrueDistanceSelector innerEdgeSelector = new EdgeSelectors.MultiAndTrueDistanceSelector();
            EdgeSelectors.MultiAndTrueDistanceSelector outerEdgeSelector = new EdgeSelectors.MultiAndTrueDistanceSelector();

            shapeEdgeSelector.reset(p);
            innerEdgeSelector.reset(p);
            outerEdgeSelector.reset(p);

            for (int i = 0; i < contourCount; i++) {
                EdgeSelectors.MultiAndTrueDistance edgeDistance = edgeSelectors.get(i).distance();
                shapeEdgeSelector.merge(edgeSelectors.get(i));

                double resolvedDistance = MathUtils.median(edgeDistance.c, edgeDistance.m, edgeDistance.y);
                if (windings.get(i) > 0 && resolvedDistance >= 0) {
                    innerEdgeSelector.merge(edgeSelectors.get(i));
                }
                if (windings.get(i) < 0 && resolvedDistance <= 0) {
                    outerEdgeSelector.merge(edgeSelectors.get(i));
                }
            }

            EdgeSelectors.MultiAndTrueDistance shapeDistance = shapeEdgeSelector.distance();
            EdgeSelectors.MultiAndTrueDistance innerDistance = innerEdgeSelector.distance();
            EdgeSelectors.MultiAndTrueDistance outerDistance = outerEdgeSelector.distance();

            double innerScalarDistance = MathUtils.median(innerDistance.c, innerDistance.m, innerDistance.y);
            double outerScalarDistance = MathUtils.median(outerDistance.c, outerDistance.m, outerDistance.y);

            EdgeSelectors.MultiAndTrueDistance distance;

            int winding = 0;
            if (innerScalarDistance >= 0 && Math.abs(innerScalarDistance) <= Math.abs(outerScalarDistance)) {
                distance = innerDistance;
                winding = 1;
                for (int i = 0; i < contourCount; i++) {
                    if (windings.get(i) > 0) {
                        EdgeSelectors.MultiAndTrueDistance contourDistance = edgeSelectors.get(i).distance();
                        double contourScalar = MathUtils.median(contourDistance.c, contourDistance.m, contourDistance.y);
                        double distanceScalar = MathUtils.median(distance.c, distance.m, distance.y);
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
                        EdgeSelectors.MultiAndTrueDistance contourDistance = edgeSelectors.get(i).distance();
                        double contourScalar = MathUtils.median(contourDistance.c, contourDistance.m, contourDistance.y);
                        double distanceScalar = MathUtils.median(distance.c, distance.m, distance.y);
                        if (Math.abs(contourScalar) < Math.abs(innerScalarDistance) && contourScalar < distanceScalar) {
                            distance = contourDistance;
                        }
                    }
                }
            } else {
                return shapeDistance;
            }

            double distanceScalar = MathUtils.median(distance.c, distance.m, distance.y);
            for (int i = 0; i < contourCount; i++) {
                if (windings.get(i) != winding) {
                    EdgeSelectors.MultiAndTrueDistance contourDistance = edgeSelectors.get(i).distance();
                    double contourScalar = MathUtils.median(contourDistance.c, contourDistance.m, contourDistance.y);
                    if (contourScalar * distanceScalar >= 0 && Math.abs(contourScalar) < Math.abs(distanceScalar)) {
                        distance = contourDistance;
                        distanceScalar = contourScalar;
                    }
                }
            }
            double shapeScalar = MathUtils.median(shapeDistance.c, shapeDistance.m, shapeDistance.y);
            if (distanceScalar == shapeScalar) {
                distance = shapeDistance;
            }

            return distance;
        }

        private MsdfShape createShapeFromSelectors() {
            // This is a helper method that would need to be implemented based on your Shape class
            // For now, returning null as a placeholder
            return null;
        }
    }
}