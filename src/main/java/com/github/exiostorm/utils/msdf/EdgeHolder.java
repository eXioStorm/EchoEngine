package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;

import java.awt.geom.Rectangle2D;
//TODO 20251214 after updating this class to not use it's own color variable everything breaks.
public class EdgeHolder {
    // public to match original C++ usage (code expects .edge)
    public Vector2d point;
    public double absDistance;
    public double aDomainDistance, bDomainDistance;
    public double aPerpendicularDistance, bPerpendicularDistance;
    public EdgeSegment edge;

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
        this();
        this.edge = edge;
    }

    // copy constructor
    public EdgeHolder(EdgeHolder orig) {
        this.edge = (orig != null && orig.edge != null) ? orig.edge : null;
    }/*
    public ColorHolder getColor() {
        return edge != null ? edge.edgeColor : null;
    }

    public void setColor(int colorValue) {
            if (this.edge.edgeColor == null) {
                this.edge.edgeColor = new ColorHolder(colorValue);
            } else {
                this.edge.edgeColor.color = colorValue;
            }
    }*/

    // swap utility
    public static void swap(EdgeHolder a, EdgeHolder b) {
        EdgeSegment tmp = a.edge;
        a.edge = b.edge;
        b.edge = tmp;
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
    public EdgeSegment get() { return edge; }
    public EdgeSegment toEdgeSegment() { return edge; }
}