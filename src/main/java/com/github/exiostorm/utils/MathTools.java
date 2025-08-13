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

}
