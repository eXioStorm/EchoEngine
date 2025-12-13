package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class Contours {
    public static class Contour {

        public List<EdgeHolder> edges = new ArrayList<>();
        private static double shoelace(Vector2d a, Vector2d b) {
            return (b.x - a.x) * (a.y + b.y);
        }
        public void addEdge(EdgeHolder edge) {
            edges.add(edge);
        }

        private static void boundPoint(Rectangle2D.Double bounds, Vector2d p) {
            bounds.add(p.x, p.y);
        }

        public void bound(Rectangle2D.Double bounds) {
            for (java.util.Iterator<EdgeHolder> edge = edges.iterator(); edge.hasNext();) {
                edge.next().bound(bounds);
            }
        }

        public void boundMiters(Rectangle2D.Double bounds, double border, double miterLimit, int polarity) {
            if (edges.isEmpty()) {
                return;
            }
            Vector2d prevDir = edges.get(edges.size() - 1).edge.direction(1).normalize(new Vector2d());
            for (EdgeHolder edgeHolder : edges) {
                Vector2d dir = edgeHolder.edge.direction(0).normalize(new Vector2d()).negate(new Vector2d());
                if (polarity * prevDir.dot(dir) >= 0) {
                    double miterLength = miterLimit;
                    double q = 0.5 * (1 - prevDir.dot(dir));
                    if (q > 0) {
                        miterLength = Math.min(1 / Math.sqrt(q), miterLimit);
                    }
                    Vector2d miter = new Vector2d(edgeHolder.edge.point(0)).add(
                            new Vector2d(prevDir).add(dir).normalize(new Vector2d()).mul((float) (border * miterLength))
                    );
                }
                prevDir.set(edgeHolder.edge.direction(1).normalize(new Vector2d()));
            }
        }

        public int winding() {
            if (edges.isEmpty()) {
                return 0;
            }
            double total = 0;
            if (edges.size() == 1) {
                Vector2d a = edges.get(0).edge.point(0);
                Vector2d b = edges.get(0).edge.point(1.0/3.0f);
                Vector2d c = edges.get(0).edge.point(2.0/3.0f);
                total += shoelace(a, b);
                total += shoelace(b, c);
                total += shoelace(c, a);
            }
            else if (edges.size() == 2) {
                Vector2d a = edges.get(0).edge.point(0);
                Vector2d b = edges.get(0).edge.point(0.5f);
                Vector2d c = edges.get(1).edge.point(0);
                Vector2d d = edges.get(1).edge.point(0.5f);
                total += shoelace(a, b);
                total += shoelace(b, c);
                total += shoelace(c, d);
                total += shoelace(d, a);
            }
            else {
                Vector2d prev = edges.get(edges.size() - 1).edge.point(0);
                for (EdgeHolder edgeHolder : edges) {
                    Vector2d cur = edgeHolder.edge.point(0);
                    total += shoelace(prev, cur);
                    prev = cur;
                }
            }
            return (int) Math.signum(total);
        }

        public void reverse() {
            Collections.reverse(edges);
            for (EdgeHolder edge : edges) {
                edge.reverse();
            }
        }
    }
}
