package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

import java.awt.geom.Rectangle2D;

public class EdgeHolder {
    // public to match original C++ usage (code expects .edge)
    public Vector2d point;
    public double absDistance;
    public double aDomainDistance, bDomainDistance;
    public double aPerpendicularDistance, bPerpendicularDistance;
    public EdgeSegment edge;
    public int color;

    public EdgeHolder() {
        this.edge = null;
        this.point = new Vector2d();
        this.absDistance = 0.0;
        this.aDomainDistance = 0;
        this.bDomainDistance = 0;
        this.aPerpendicularDistance = 0;
        this.bPerpendicularDistance = 0;
    }

    public EdgeHolder(EdgeSegment edge) {
        this.edge = edge;
    }

    // copy constructor
    public EdgeHolder(EdgeHolder orig) {
        this.edge = (orig != null && orig.edge != null) ? orig.edge : null;
        this.color = (orig != null) ? orig.color : 0;
    }

    // swap utility
    public static void swap(EdgeHolder a, EdgeHolder b) {
        EdgeSegment tmp = a.edge;
        a.edge = b.edge;
        b.edge = tmp;

        int tc = a.color;
        a.color = b.color;
        b.color = tc;
    }

    // getter / setter for code that used edgeSegment methods
    public EdgeSegment getEdgeSegment() {
        return edge;
    }

    public void setEdgeSegment(EdgeSegment e) {
        this.edge = e;
    }

    // delegates: keep behavior consistent with original library
    public void bound(Rectangle2D.Double bounds) {
        if (edge != null) {
            edge.bound(bounds);
        }
    }

    public void reverse() {
        if (edge != null) {
            edge.reverse();
        }
    }

    // optional convenience: if some code expects get()/toEdgeSegment()
    public EdgeSegment get() { return edge; }
    public EdgeSegment toEdgeSegment() { return edge; }
}