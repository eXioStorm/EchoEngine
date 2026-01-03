package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;

public class EdgeSelectors {

    public static class MultiDistance {
        public double r, g, b;
    }

    public static class MultiAndTrueDistance extends MultiDistance {
        public double a;
    }
    public static class TrueDistanceSelector {

        private Vector2d p;
        private SignedDistance minDistance;

        public TrueDistanceSelector() {
            this.p = new Vector2d();
            this.minDistance = new SignedDistance();
        }

        public void reset(Vector2d p) {
            double delta = 1.001 * new Vector2d(p).sub(this.p).length();
            minDistance.distance += Math.signum(minDistance.distance) * delta;
            this.p.set(p);
        }

        public void addEdge(EdgeHolder cache, EdgeSegment prevEdge, EdgeSegment edge, EdgeSegment nextEdge) {
            double delta = 1.001 * p.sub(cache.point, new Vector2d()).length();
            if (cache.absDistance - delta <= Math.abs(minDistance.distance)) {
                double[] dummy = new double[1];
                SignedDistance distance = edge.signedDistance(p, dummy);
                if (distance.compareTo(minDistance) < 0) {
                    minDistance = distance;
                }
                cache.point.set(p);
                cache.absDistance = Math.abs(distance.distance);
            }
        }

        public void merge(TrueDistanceSelector other) {
            if (other.minDistance.compareTo(minDistance) < 0) {
                minDistance = other.minDistance;
            }
        }

        public double distance() {
            return minDistance.distance;
        }
    }

    public static class PerpendicularDistanceSelectorBase {

        protected SignedDistance minTrueDistance;
        protected double minNegativePerpendicularDistance;
        protected double minPositivePerpendicularDistance;
        protected EdgeSegment nearEdge;
        protected double nearEdgeParam;

        public PerpendicularDistanceSelectorBase() {
            this.minTrueDistance = new SignedDistance();
            this.minNegativePerpendicularDistance = -Math.abs(minTrueDistance.distance);
            this.minPositivePerpendicularDistance = Math.abs(minTrueDistance.distance);
            this.nearEdge = null;
            this.nearEdgeParam = 0;
        }

        public static boolean getPerpendicularDistance(double[] distance, Vector2d ep, Vector2d edgeDir) {
            double ts = ep.dot(edgeDir);
            if (ts > 0) {
                double perpendicularDistance = ep.x * edgeDir.y - ep.y * edgeDir.x;
                if (Math.abs(perpendicularDistance) < Math.abs(distance[0])) {
                    distance[0] = perpendicularDistance;
                    return true;
                }
            }
            return false;
        }

        public void reset(double delta) {
            minTrueDistance.distance += Math.signum(minTrueDistance.distance) * delta;
            minNegativePerpendicularDistance = -Math.abs(minTrueDistance.distance);
            minPositivePerpendicularDistance = Math.abs(minTrueDistance.distance);
            nearEdge = null;
            nearEdgeParam = 0;
        }

        public boolean isEdgeRelevant(EdgeHolder cache, EdgeSegment edge, Vector2d p) {
            double delta = 1.001 * p.sub(cache.point, new Vector2d()).length();
            return (
                    cache.absDistance - delta <= Math.abs(minTrueDistance.distance) ||
                            Math.abs(cache.aDomainDistance) < delta ||
                            Math.abs(cache.bDomainDistance) < delta ||
                            (cache.aDomainDistance > 0 && (
                                    cache.aPerpendicularDistance < 0 ?
                                            cache.aPerpendicularDistance + delta >= minNegativePerpendicularDistance :
                                            cache.aPerpendicularDistance - delta <= minPositivePerpendicularDistance
                            )) ||
                            (cache.bDomainDistance > 0 && (
                                    cache.bPerpendicularDistance < 0 ?
                                            cache.bPerpendicularDistance + delta >= minNegativePerpendicularDistance :
                                            cache.bPerpendicularDistance - delta <= minPositivePerpendicularDistance
                            ))
            );
        }

        public void addEdgeTrueDistance(EdgeSegment edge, SignedDistance distance, double param) {
            if (distance.compareTo(minTrueDistance) < 0) {
                minTrueDistance = distance;
                nearEdge = edge;
                nearEdgeParam = param;
            }
        }

        public void addEdgePerpendicularDistance(double distance) {
            if (distance <= 0 && distance > minNegativePerpendicularDistance)
                minNegativePerpendicularDistance = distance;
            if (distance >= 0 && distance < minPositivePerpendicularDistance)
                minPositivePerpendicularDistance = distance;
        }

        public void merge(PerpendicularDistanceSelectorBase other) {
            if (other.minTrueDistance.compareTo(minTrueDistance) < 0) {
                minTrueDistance = other.minTrueDistance;
                nearEdge = other.nearEdge;
                nearEdgeParam = other.nearEdgeParam;
            }
            if (other.minNegativePerpendicularDistance > minNegativePerpendicularDistance)
                minNegativePerpendicularDistance = other.minNegativePerpendicularDistance;
            if (other.minPositivePerpendicularDistance < minPositivePerpendicularDistance)
                minPositivePerpendicularDistance = other.minPositivePerpendicularDistance;
        }

        public double computeDistance(Vector2d p) {
            double minDistance = minTrueDistance.distance < 0 ?
                    minNegativePerpendicularDistance : minPositivePerpendicularDistance;
            if (nearEdge != null) {
                SignedDistance distance = minTrueDistance;
                nearEdge.distanceToPerpendicularDistance(distance, p, nearEdgeParam);
                if (Math.abs(distance.distance) < Math.abs(minDistance))
                    minDistance = distance.distance;
            }
            return minDistance;
        }

        public SignedDistance trueDistance() {
            return minTrueDistance;
        }
    }

    public static class PerpendicularDistanceSelector extends PerpendicularDistanceSelectorBase {
        private Vector2d p;

        public PerpendicularDistanceSelector() {
            this.p = new Vector2d();
        }

        public void reset(Vector2d p) {
            double delta = 1.001 * p.sub(this.p, new Vector2d()).length();
            super.reset(delta);
            this.p.set(p);
        }

        public void addEdge(EdgeHolder cache, EdgeSegment prevEdge, EdgeSegment edge, EdgeSegment nextEdge) {
            if (isEdgeRelevant(cache, edge, p)) {
                double[] param = new double[1];
                SignedDistance distance = edge.signedDistance(p, param);
                addEdgeTrueDistance(edge, distance, param[0]);
                cache.point.set(p);
                cache.absDistance = Math.abs(distance.distance);

                // (Vector math translated directly; assumes helper functions exist)
                Vector2d ap = p.sub(edge.point(0), new Vector2d());
                Vector2d bp = p.sub(edge.point(1), new Vector2d());
                Vector2d aDir = edge.direction(0).normalize(new Vector2d());
                Vector2d bDir = edge.direction(1).normalize(new Vector2d());
                Vector2d prevDir = prevEdge.direction(1).normalize(new Vector2d());
                Vector2d nextDir = nextEdge.direction(0).normalize(new Vector2d());

                double add = ap.dot(prevDir.add(aDir, new Vector2d()).normalize());
                double bdd = -bp.dot(bDir.add(nextDir, new Vector2d()).normalize());

                if (add > 0) {
                    double pd = distance.distance;
                    double[] pdArr = { pd };
                    if (getPerpendicularDistance(pdArr, ap, aDir.negate(new Vector2d()))) {
                        pd = -pdArr[0];
                        addEdgePerpendicularDistance(pd);
                    }
                    cache.aPerpendicularDistance = pd;
                }
                if (bdd > 0) {
                    double pd = distance.distance;
                    double[] pdArr = { pd };
                    if (getPerpendicularDistance(pdArr, bp, bDir)) {
                        pd = pdArr[0];
                        addEdgePerpendicularDistance(pd);
                    }
                    cache.bPerpendicularDistance = pd;
                }

                cache.aDomainDistance = add;
                cache.bDomainDistance = bdd;
            }
        }

        public double distance() {
            return computeDistance(p);
        }
    }

    public static class MultiDistanceSelector {
        private Vector2d p;
        private PerpendicularDistanceSelectorBase r, g, b;

        public MultiDistanceSelector() {
            this.p = new Vector2d();
            this.r = new PerpendicularDistanceSelectorBase();
            this.g = new PerpendicularDistanceSelectorBase();
            this.b = new PerpendicularDistanceSelectorBase();
        }

        public void reset(Vector2d p) {
            double delta = 1.001 * p.sub(this.p, new Vector2d()).length();
            r.reset(delta);
            g.reset(delta);
            b.reset(delta);
            this.p.set(p);
        }
        public void addEdge(EdgeHolder cache,
                            EdgeSegment prevEdge, EdgeSegment edge, EdgeSegment nextEdge) {
            boolean redRelevant = (edge.edgeColor & EdgeColorEnum.RED.getValue().color) != 0 &&
                    r.isEdgeRelevant(cache, edge, p);
            boolean greenRelevant = (edge.edgeColor & EdgeColorEnum.GREEN.getValue().color) != 0 &&
                    g.isEdgeRelevant(cache, edge, p);
            boolean blueRelevant = (edge.edgeColor & EdgeColorEnum.BLUE.getValue().color) != 0 &&
                    b.isEdgeRelevant(cache, edge, p);

            if (redRelevant || greenRelevant || blueRelevant) {
                //TODO 20260104 might need to manually convert this, Claude doesn't like param. supposedly unused.
                DoubleReference param = new DoubleReference();
                SignedDistance distance = edge.signedDistance(p, new double[] {param.getValue()});

                if ((edge.edgeColor & EdgeColorEnum.RED.getValue().color) != 0) {
                    r.addEdgeTrueDistance(edge, distance, param.getValue());
                }
                if ((edge.edgeColor & EdgeColorEnum.GREEN.getValue().color) != 0) {
                    g.addEdgeTrueDistance(edge, distance, param.getValue());
                }
                if ((edge.edgeColor & EdgeColorEnum.BLUE.getValue().color) != 0) {
                    b.addEdgeTrueDistance(edge, distance, param.getValue());
                }

                cache.point.set(p);
                cache.absDistance = Math.abs(distance.distance);

                Vector2d ap = new Vector2d(p).sub(edge.point(0));
                Vector2d bp = new Vector2d(p).sub(edge.point(1));
                Vector2d aDir = edge.direction(0).normalize();
                Vector2d bDir = edge.direction(1).normalize();
                Vector2d prevDir = prevEdge.direction(1).normalize();
                Vector2d nextDir = nextEdge.direction(0).normalize();

                double add = ap.dot(new Vector2d(prevDir).add(aDir).normalize());
                double bdd = -bp.dot(new Vector2d(bDir).add(nextDir).normalize());

                if (add > 0) {
                    DoubleReference pd = new DoubleReference(distance.distance);
                    if (PerpendicularDistanceSelectorBase.getPerpendicularDistance(new double[] {pd.getValue()}, ap, new Vector2d(aDir).negate())) {
                        pd.setValue(-pd.getValue());
                        if ((edge.edgeColor & EdgeColorEnum.RED.getValue().color) != 0) {
                            r.addEdgePerpendicularDistance(pd.getValue());
                        }
                        if ((edge.edgeColor & EdgeColorEnum.GREEN.getValue().color) != 0) {
                            g.addEdgePerpendicularDistance(pd.getValue());
                        }
                        if ((edge.edgeColor & EdgeColorEnum.BLUE.getValue().color) != 0) {
                            b.addEdgePerpendicularDistance(pd.getValue());
                        }
                    }
                    cache.aPerpendicularDistance = pd.getValue();
                }

                if (bdd > 0) {
                    DoubleReference pd = new DoubleReference(distance.distance);
                    if (PerpendicularDistanceSelectorBase.getPerpendicularDistance(new double[] {pd.getValue()}, bp, bDir)) {
                        if ((edge.edgeColor & EdgeColorEnum.RED.getValue().color) != 0) {
                            r.addEdgePerpendicularDistance(pd.getValue());
                        }
                        if ((edge.edgeColor & EdgeColorEnum.GREEN.getValue().color) != 0) {
                            g.addEdgePerpendicularDistance(pd.getValue());
                        }
                        if ((edge.edgeColor & EdgeColorEnum.BLUE.getValue().color) != 0) {
                            b.addEdgePerpendicularDistance(pd.getValue());
                        }
                    }
                    cache.bPerpendicularDistance = pd.getValue();
                }

                cache.aDomainDistance = add;
                cache.bDomainDistance = bdd;
            }
        }

        public void merge(MultiDistanceSelector other) {
            r.merge(other.r);
            g.merge(other.g);
            b.merge(other.b);
        }

        public MultiDistance distance() {
            MultiDistance md = new MultiDistance();
            md.r = r.computeDistance(p);
            md.g = g.computeDistance(p);
            md.b = b.computeDistance(p);
            return md;
        }

        public SignedDistance trueDistance() {
            SignedDistance d = r.trueDistance();
            if (g.trueDistance().compareTo(d) < 0) d = g.trueDistance();
            if (b.trueDistance().compareTo(d) < 0) d = b.trueDistance();
            return d;
        }
    }

    public static class MultiAndTrueDistanceSelector extends MultiDistanceSelector {
        public MultiAndTrueDistance distance() {
            MultiDistance base = super.distance();
            MultiAndTrueDistance mtd = new MultiAndTrueDistance();
            mtd.r = base.r;
            mtd.g = base.g;
            mtd.b = base.b;
            mtd.a = super.trueDistance().distance;
            return mtd;
        }
    }
    public interface EdgeSelector<D> {
        /** Called once per sample point before adding edges. */
        void reset(Vector2d p);

        /**
         * Called while collecting edges for a single contour.
         * Each concrete selector uses its own EdgeCache type; use Object if you prefer,
         * but typed caches are better (see examples below).
         */
        void addEdge(Object edgeCache, EdgeSegment prevEdge, EdgeSegment edge, EdgeSegment nextEdge);

        /** Merge another selector (same concrete type) into this one. */
        void merge(EdgeSelector<D> other);

        /** Return the distance structure for this selector. */
        D distance();
    }
}

