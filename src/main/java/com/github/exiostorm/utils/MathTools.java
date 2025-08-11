package com.github.exiostorm.utils;

import org.apache.commons.collections4.MultiValuedMap;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MathTools {
    /**
     * This is our bin packer, use other methods for our texture Atlas...
     * @param rectangleMap Map<String, Rectangle> for packing Rectangles to an area.
     */
    //TODO [0] need to change this to multivaluedmap to allow multiple textures per grouping.
    public static <K> Rectangle rectanglePacker2D(Map<K, Rectangle> rectangleMap) {
        List<Map.Entry<K, Rectangle>> sortedTextures = new ArrayList<>(rectangleMap.entrySet());
        List<Rectangle> freeRectangles = new ArrayList<>();
        Rectangle calculatedSize = new Rectangle(0,0,0,0);
        sortedTextures.sort(Comparator.comparingInt(e -> -e.getValue().width * e.getValue().height));

        //freeRectangles.clear();
        freeRectangles.add(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)); // Initial large free area

        calculatedSize.setBounds(0,0,0,0);

        for (Map.Entry<K, Rectangle> entry : sortedTextures) {
            Rectangle rectangle = entry.getValue();

            int bestAreaFit = Integer.MAX_VALUE;
            Rectangle bestRect = null;
            int bestIndex = -1;

            for (int i = 0; i < freeRectangles.size(); i++) {
                Rectangle freeRect = freeRectangles.get(i);
                if (freeRect.width >= rectangle.width && freeRect.height >= rectangle.height) {
                    int areaFit = freeRect.width * freeRect.height - rectangle.width * rectangle.height;
                    if (areaFit < bestAreaFit) {
                        bestRect = new Rectangle(freeRect.x, freeRect.y, rectangle.width, rectangle.height);
                        bestAreaFit = areaFit;
                        bestIndex = i;
                    }
                }
            }

            if (bestRect != null) {
                Rectangle freeRect = freeRectangles.remove(bestIndex);

                if (freeRect.width > bestRect.width) {
                    freeRectangles.add(new Rectangle(bestRect.x + bestRect.width, bestRect.y,
                            freeRect.width - bestRect.width, freeRect.height));
                }
                if (freeRect.height > bestRect.height) {
                    freeRectangles.add(new Rectangle(bestRect.x, bestRect.y + bestRect.height,
                            bestRect.width, freeRect.height - bestRect.height));
                }
                rectangle.setLocation(bestRect.x, bestRect.y);
                rectangleMap.put(entry.getKey(), bestRect);
                calculatedSize.x = bestRect.x;
                calculatedSize.y = bestRect.y;
                calculatedSize.width = Math.max(calculatedSize.width, bestRect.x + bestRect.width);
                calculatedSize.height = Math.max(calculatedSize.height, bestRect.y + bestRect.height);
            }
        }
        //TODO return the total size of this calculation
        return calculatedSize;
    }
    public static <K, V> Rectangle rectanglePacker2D(MultiValuedMap<K, Map<V, Rectangle>> multiValuedRectangleMap) {
        List<Map.Entry<V, Rectangle>> sortedTextures = new ArrayList<>();

        for (K key : multiValuedRectangleMap.keySet()) {
            for (Map<V, Rectangle> innerMap : multiValuedRectangleMap.get(key)) {
                sortedTextures.addAll(innerMap.entrySet());
            }
        }

        List<Rectangle> freeRectangles = new ArrayList<>();
        Rectangle calculatedSize = new Rectangle(0, 0, 0, 0);
        sortedTextures.sort(Comparator.comparingInt(e -> -e.getValue().width * e.getValue().height));

        freeRectangles.add(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE));

        for (Map.Entry<V, Rectangle> entry : sortedTextures) {
            Rectangle rectangle = entry.getValue();

            int bestAreaFit = Integer.MAX_VALUE;
            int bestX = 0;
            int bestY = 0;
            int bestIndex = -1;

            for (int i = 0; i < freeRectangles.size(); i++) {
                Rectangle freeRect = freeRectangles.get(i);
                if (freeRect.width >= rectangle.width && freeRect.height >= rectangle.height) {
                    int areaFit = freeRect.width * freeRect.height - rectangle.width * rectangle.height;
                    if (areaFit < bestAreaFit) {
                        bestX = freeRect.x;
                        bestY = freeRect.y;
                        bestAreaFit = areaFit;
                        bestIndex = i;
                    }
                }
            }

            if (bestIndex != -1) {
                Rectangle freeRect = freeRectangles.remove(bestIndex);

                // Update position of the original rectangle (this is the key fix)
                rectangle.setLocation(bestX, bestY);

                // Create new free rectangles
                if (freeRect.width > rectangle.width) {
                    freeRectangles.add(new Rectangle(bestX + rectangle.width, bestY,
                            freeRect.width - rectangle.width, freeRect.height));
                }
                if (freeRect.height > rectangle.height) {
                    freeRectangles.add(new Rectangle(bestX, bestY + rectangle.height,
                            rectangle.width, freeRect.height - rectangle.height));
                }

                // Update calculated size
                calculatedSize.width = Math.max(calculatedSize.width, bestX + rectangle.width);
                calculatedSize.height = Math.max(calculatedSize.height, bestY + rectangle.height);
            }
        }
        return calculatedSize;
    }
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
