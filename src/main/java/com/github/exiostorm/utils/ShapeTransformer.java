package com.github.exiostorm.utils;

import java.awt.*;
import java.awt.geom.*;

public class ShapeTransformer {
    /**
     * Applies scaling to a transparency map by resampling
     */
    public static boolean[] scaleBooleanImageArray(boolean[] originalMap, int originalWidth, int originalHeight, float scaleX, float scaleY) {
        int newWidth = Math.round(originalWidth * scaleX);
        int newHeight = Math.round(originalHeight * scaleY);
        boolean[] scaledMap = new boolean[newWidth * newHeight];

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Map back to original coordinates
                int origX = Math.round(x / scaleX);
                int origY = Math.round(y / scaleY);

                // Clamp to bounds
                origX = Math.max(0, Math.min(originalWidth - 1, origX));
                origY = Math.max(0, Math.min(originalHeight - 1, origY));

                scaledMap[y * newWidth + x] = originalMap[origY * originalWidth + origX];
            }
        }
        return scaledMap;
    }

    /**
     * Flips transparency map horizontally
     */
    public static boolean[] rectangleFlipHorizontal(boolean[] map, int width, int height) {
        boolean[] flippedMap = new boolean[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalIndex = y * width + x;
                int flippedIndex = y * width + (width - 1 - x);
                flippedMap[flippedIndex] = map[originalIndex];
            }
        }

        return flippedMap;
    }

    /**
     * Flips transparency map vertically
     */
    public static boolean[] rectangleFlipVertical(boolean[] map, int width, int height) {
        boolean[] flippedMap = new boolean[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalIndex = y * width + x;
                int flippedIndex = (height - 1 - y) * width + x;
                flippedMap[flippedIndex] = map[originalIndex];
            }
        }

        return flippedMap;
    }

    /**
     * Rotates transparency map by arbitrary angle using nearest neighbor sampling
     * Returns a new map with potentially different dimensions
     */
    public static TransformResult rectangleRotate(boolean[] map, int width, int height, float angleRadians) {
        if (angleRadians == 0) {
            return new TransformResult(map.clone(), width, height);
        }

        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians);

        // Calculate bounding box of rotated image
        float[] corners = {
                -width/2f, -height/2f,  // bottom-left
                width/2f, -height/2f,   // bottom-right
                width/2f, height/2f,    // top-right
                -width/2f, height/2f    // top-left
        };

        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        for (int i = 0; i < 4; i++) {
            float x = corners[i * 2];
            float y = corners[i * 2 + 1];

            float rotX = x * cos - y * sin;
            float rotY = x * sin + y * cos;

            minX = Math.min(minX, rotX);
            maxX = Math.max(maxX, rotX);
            minY = Math.min(minY, rotY);
            maxY = Math.max(maxY, rotY);
        }

        int newWidth = (int) Math.ceil(maxX - minX);
        int newHeight = (int) Math.ceil(maxY - minY);
        boolean[] rotatedMap = new boolean[newWidth * newHeight];

        float centerX = newWidth / 2f;
        float centerY = newHeight / 2f;

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Convert to centered coordinates
                float relX = x - centerX;
                float relY = y - centerY;

                // Reverse rotation to find source pixel
                float srcX = relX * cos + relY * sin + width / 2f;
                float srcY = -relX * sin + relY * cos + height / 2f;

                int srcXInt = Math.round(srcX);
                int srcYInt = Math.round(srcY);

                // Check bounds and sample
                if (srcXInt >= 0 && srcXInt < width && srcYInt >= 0 && srcYInt < height) {
                    rotatedMap[y * newWidth + x] = map[srcYInt * width + srcXInt];
                }
                // else remains false (transparent)
            }
        }

        return new TransformResult(rotatedMap, newWidth, newHeight);
    }

    /**
     * Applies the same transformations that your quad undergoes to the transparency map
     */
    public static TransformResult transformMap(boolean[] originalMap, int originalWidth, int originalHeight,
                                               float scaleX, float scaleY, boolean flipX, boolean flipY, float rotation) {
        boolean[] currentMap = originalMap;
        int currentWidth = originalWidth;
        int currentHeight = originalHeight;

        // Apply scaling first
        if (scaleX != 1.0f || scaleY != 1.0f) {
            currentMap = scaleBooleanImageArray(currentMap, currentWidth, currentHeight, scaleX, scaleY);
            currentWidth = Math.round(currentWidth * scaleX);
            currentHeight = Math.round(currentHeight * scaleY);
        }

        // Apply flipping
        if (flipX) {
            currentMap = rectangleFlipHorizontal(currentMap, currentWidth, currentHeight);
        }
        if (flipY) {
            currentMap = rectangleFlipVertical(currentMap, currentWidth, currentHeight);
        }

        // Apply rotation
        if (rotation != 0) {
            TransformResult result = rectangleRotate(currentMap, currentWidth, currentHeight, rotation);
            currentMap = result.map;
            currentWidth = result.width;
            currentHeight = result.height;
        }

        return new TransformResult(currentMap, currentWidth, currentHeight);
    }

    /**
     * Applies scaling to a Polygon
     */
    public static Polygon scalePolygon(Polygon original, float scaleX, float scaleY) {
        int[] newXPoints = new int[original.npoints];
        int[] newYPoints = new int[original.npoints];

        for (int i = 0; i < original.npoints; i++) {
            newXPoints[i] = Math.round(original.xpoints[i] * scaleX);
            newYPoints[i] = Math.round(original.ypoints[i] * scaleY);
        }

        return new Polygon(newXPoints, newYPoints, original.npoints);
    }

    /**
     * Flips Polygon horizontally around a center point
     */
    public static Polygon flipPolygonHorizontal(Polygon original, int centerX) {
        int[] newXPoints = new int[original.npoints];
        int[] newYPoints = new int[original.npoints];

        for (int i = 0; i < original.npoints; i++) {
            newXPoints[i] = 2 * centerX - original.xpoints[i];
            newYPoints[i] = original.ypoints[i];
        }

        return new Polygon(newXPoints, newYPoints, original.npoints);
    }

    /**
     * Flips Polygon horizontally around its bounding box center
     */
    public static Polygon flipPolygonHorizontal(Polygon original) {
        java.awt.Rectangle bounds = original.getBounds();
        int centerX = bounds.x + bounds.width / 2;
        return flipPolygonHorizontal(original, centerX);
    }

    /**
     * Flips Polygon vertically around a center point
     */
    public static Polygon flipPolygonVertical(Polygon original, int centerY) {
        int[] newXPoints = new int[original.npoints];
        int[] newYPoints = new int[original.npoints];

        for (int i = 0; i < original.npoints; i++) {
            newXPoints[i] = original.xpoints[i];
            newYPoints[i] = 2 * centerY - original.ypoints[i];
        }

        return new Polygon(newXPoints, newYPoints, original.npoints);
    }

    /**
     * Flips Polygon vertically around its bounding box center
     */
    public static Polygon flipPolygonVertical(Polygon original) {
        java.awt.Rectangle bounds = original.getBounds();
        int centerY = bounds.y + bounds.height / 2;
        return flipPolygonVertical(original, centerY);
    }

    /**
     * Rotates Polygon by arbitrary angle around a center point
     */
    public static Polygon rotatePolygon(Polygon original, float angleRadians, float centerX, float centerY) {
        if (angleRadians == 0) {
            return new Polygon(original.xpoints.clone(), original.ypoints.clone(), original.npoints);
        }

        float cos = (float) Math.cos(angleRadians);
        float sin = (float) Math.sin(angleRadians);

        int[] newXPoints = new int[original.npoints];
        int[] newYPoints = new int[original.npoints];

        for (int i = 0; i < original.npoints; i++) {
            // Translate to origin
            float x = original.xpoints[i] - centerX;
            float y = original.ypoints[i] - centerY;

            // Rotate
            float rotX = x * cos - y * sin;
            float rotY = x * sin + y * cos;

            // Translate back
            newXPoints[i] = Math.round(rotX + centerX);
            newYPoints[i] = Math.round(rotY + centerY);
        }

        return new Polygon(newXPoints, newYPoints, original.npoints);
    }

    /**
     * Rotates Polygon around its bounding box center
     */
    public static Polygon rotatePolygon(Polygon original, float angleRadians) {
        java.awt.Rectangle bounds = original.getBounds();
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;
        return rotatePolygon(original, angleRadians, centerX, centerY);
    }

    /**
     * Applies the same transformations sequence to a Polygon
     * This mirrors the transformMap method from your original code
     */
    public static Polygon transformPolygon(Polygon original, float scaleX, float scaleY,
                                           boolean flipX, boolean flipY, float rotation) {
        Polygon currentPolygon = new Polygon(original.xpoints.clone(), original.ypoints.clone(), original.npoints);

        // Apply scaling first
        if (scaleX != 1.0f || scaleY != 1.0f) {
            currentPolygon = scalePolygon(currentPolygon, scaleX, scaleY);
        }

        // Apply flipping
        if (flipX) {
            currentPolygon = flipPolygonHorizontal(currentPolygon);
        }
        if (flipY) {
            currentPolygon = flipPolygonVertical(currentPolygon);
        }

        // Apply rotation
        if (rotation != 0) {
            currentPolygon = rotatePolygon(currentPolygon, rotation);
        }

        return currentPolygon;
    }

    /**
     * Alternative implementation using AffineTransform for more precision
     * This approach maintains floating-point precision throughout the transformation
     */
    public static Polygon transformPolygonPrecise(Polygon original, float scaleX, float scaleY,
                                                  boolean flipX, boolean flipY, float rotation) {
        // Create the transformation matrix
        AffineTransform transform = new AffineTransform();

        // Get center point for rotation and flipping
        java.awt.Rectangle bounds = original.getBounds();
        float centerX = bounds.x + bounds.width / 2f;
        float centerY = bounds.y + bounds.height / 2f;

        // Apply transformations in reverse order (AffineTransform concatenates)
        if (rotation != 0) {
            transform.rotate(rotation, centerX, centerY);
        }

        if (flipX || flipY) {
            transform.translate(centerX, centerY);
            transform.scale(flipX ? -1 : 1, flipY ? -1 : 1);
            transform.translate(-centerX, -centerY);
        }

        if (scaleX != 1.0f || scaleY != 1.0f) {
            transform.scale(scaleX, scaleY);
        }

        // Apply transformation to create a Path2D
        Path2D transformedPath = new Path2D.Float();
        transformedPath.append(original.getPathIterator(transform), false);

        // Convert back to Polygon
        return pathToPolygon(transformedPath);
    }

    /**
     * Helper method to convert Path2D back to Polygon
     */
    private static Polygon pathToPolygon(Path2D path) {
        PathIterator iterator = path.getPathIterator(null);
        float[] coords = new float[6];

        int[] xPoints = new int[100]; // Initial capacity
        int[] yPoints = new int[100];
        int pointCount = 0;

        while (!iterator.isDone()) {
            int type = iterator.currentSegment(coords);

            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                // Expand arrays if needed
                if (pointCount >= xPoints.length) {
                    int[] newXPoints = new int[xPoints.length * 2];
                    int[] newYPoints = new int[yPoints.length * 2];
                    System.arraycopy(xPoints, 0, newXPoints, 0, pointCount);
                    System.arraycopy(yPoints, 0, newYPoints, 0, pointCount);
                    xPoints = newXPoints;
                    yPoints = newYPoints;
                }

                xPoints[pointCount] = Math.round(coords[0]);
                yPoints[pointCount] = Math.round(coords[1]);
                pointCount++;
            }

            iterator.next();
        }

        // Trim arrays to actual size
        int[] finalXPoints = new int[pointCount];
        int[] finalYPoints = new int[pointCount];
        System.arraycopy(xPoints, 0, finalXPoints, 0, pointCount);
        System.arraycopy(yPoints, 0, finalYPoints, 0, pointCount);

        return new Polygon(finalXPoints, finalYPoints, pointCount);
    }

    /**
     * Helper class to return transformed map with its new dimensions
     */
    public static class TransformResult {
        public final boolean[] map;
        public final int width;
        public final int height;

        public TransformResult(boolean[] map, int width, int height) {
            this.map = map;
            this.width = width;
            this.height = height;
        }
    }
}
