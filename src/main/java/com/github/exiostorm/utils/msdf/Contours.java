package com.github.exiostorm.utils.msdf;

import org.joml.Vector2f;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Contours {
    public class Contour {

        public Map<EdgeHolder> edges = new ArrayList<>();


        private static double shoelace(Point a, Point b) {
            return (b.x - a.x) * (a.y + b.y);
        }

        public void addEdge(EdgeHolder edge) {
            edges.add(edge);
        }

        // C++11 rvalue reference version ignored (Java has no rvalue refs)
        // public void addEdge(EdgeHolder edge) { edges.add(edge); }

        public EdgeHolder addEdge() {
            edges.add(null); // placeholder for resize behavior
            return edges.get(edges.size() - 1);
        }

        private static void boundPoint(Rectangle2D.Double bounds, Point p) {
            bounds.add(p.x, p.y);
        }

        public void bound(Rectangle2D.Double bounds) {
            for (java.util.Iterator<EdgeHolder> edge = edges.iterator(); edge.hasNext();) {
                edge.next().bound(bounds);
            }
        }

        public void boundMiters(Rectangle2D.Double bounds, double border, double miterLimit, int polarity) {
            if (edges.isEmpty())
                return;
            Vector2f prevDir = edges.get(edges.size() - 1).direction(1).normalize(true);
            for (java.util.Iterator<EdgeHolder> edgeIter = edges.iterator(); edgeIter.hasNext();) {
                EdgeHolder edge = edgeIter.next();
                Vector2f dir = edge.direction(0).normalize(true).negate();
                if (polarity * crossProduct(prevDir, dir) >= 0) {
                    double miterLength = miterLimit;
                    double q = 0.5 * (1 - dotProduct(prevDir, dir));
                    if (q > 0)
                        miterLength = Math.min(1 / Math.sqrt(q), miterLimit);
                    Point miter = edge.point(0).add(
                            (prevDir.add(dir)).normalize(true).scale(border * miterLength)
                    );
                    boundPoint(bounds, miter);
                }
                prevDir = edge.direction(1).normalize(true);
            }
        }

        public int winding() {
            if (edges.isEmpty())
                return 0;
            double total = 0;
            if (edges.size() == 1) {
                Point a = edges.get(0).point(0);
                Point b = edges.get(0).point(1 / 3.0);
                Point c = edges.get(0).point(2 / 3.0);
                total += shoelace(a, b);
                total += shoelace(b, c);
                total += shoelace(c, a);
            } else if (edges.size() == 2) {
                Point a = edges.get(0).point(0);
                Point b = edges.get(0).point(0.5);
                Point c = edges.get(1).point(0);
                Point d = edges.get(1).point(0.5);
                total += shoelace(a, b);
                total += shoelace(b, c);
                total += shoelace(c, d);
                total += shoelace(d, a);
            } else {
                Point prev = edges.get(edges.size() - 1).point(0);
                for (java.util.Iterator<EdgeHolder> edgeIter = edges.iterator(); edgeIter.hasNext();) {
                    Point cur = edgeIter.next().point(0);
                    total += shoelace(prev, cur);
                    prev = cur;
                }
            }
            return sign(total);
        }

        public void reverse() {
            for (int i = edges.size() / 2; i > 0; --i) {
                EdgeHolder.swap(edges.get(i - 1), edges.get(edges.size() - i));
            }
            for (java.util.Iterator<EdgeHolder> edgeIter = edges.iterator(); edgeIter.hasNext();) {
                edgeIter.next().reverse();
            }
        }
    }
}
