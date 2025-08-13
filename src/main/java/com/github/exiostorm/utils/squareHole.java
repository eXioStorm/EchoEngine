package com.github.exiostorm.utils;

import java.awt.*;

public class squareHole {
    /**
     * Generates a simple rectangular polygon from the texture's dimensions
     * Creates a 4-point polygon representing the full texture area
     */
    public static Polygon generateSimplePolygon(int width, int height) {
        int[] xPoints = {0, (int)width, (int)width, 0};
        int[] yPoints = {0, 0, (int)height, (int)height};
        return setPolygonBoundary(xPoints, yPoints, 4);
    }
    /**
     * Sets the polygon boundary for this UI element
     * @param xPoints Array of x coordinates (relative to element's position)
     * @param yPoints Array of y coordinates (relative to element's position)
     * @param nPoints Number of points
     */
    public static Polygon setPolygonBoundary(int[] xPoints, int[] yPoints, int nPoints) {
        return new Polygon(xPoints, yPoints, nPoints);
    }
}
