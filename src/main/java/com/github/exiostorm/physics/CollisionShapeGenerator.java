package com.github.exiostorm.physics;

import java.util.*;
import java.io.*;

import com.github.exiostorm.graphics.Texture;
import org.json.*;

import static com.github.exiostorm.physics.CollisionShapeGenerator.BoundingBox.saveCollisionShapesToJson;

public class CollisionShapeGenerator {

    // Simple bounding box class for Sweep and Prune
    public static class BoundingBox {
        public final int minX, minY, maxX, maxY;
        public final int width, height;

        public BoundingBox(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.width = maxX - minX + 1;
            this.height = maxY - minY + 1;
        }

        /**
         * Saves collision shapes to JSON file
         */
        static void saveCollisionShapesToJson(Texture texture, List<BoundingBox> boundingBoxes,
                                              String method, int parameter) {
            try {
                JSONObject root = new JSONObject();

                // Metadata
                JSONObject metadata = new JSONObject();
                metadata.put("textureWidth", texture.getWidth());
                metadata.put("textureHeight", texture.getHeight());
                metadata.put("method", method);
                metadata.put("parameter", parameter);
                metadata.put("boundingBoxCount", boundingBoxes.size());
                metadata.put("generatedAt", System.currentTimeMillis());
                root.put("metadata", metadata);

                // Bounding boxes array
                JSONArray boxesArray = new JSONArray();
                for (BoundingBox box : boundingBoxes) {
                    boxesArray.put(box.toJSON());
                }
                root.put("boundingBoxes", boxesArray);

                // Statistics
                JSONObject stats = new JSONObject();
                if (!boundingBoxes.isEmpty()) {
                    int totalArea = boundingBoxes.stream().mapToInt(BoundingBox::getArea).sum();
                    int minArea = boundingBoxes.stream().mapToInt(BoundingBox::getArea).min().orElse(0);
                    int maxArea = boundingBoxes.stream().mapToInt(BoundingBox::getArea).max().orElse(0);
                    double avgArea = boundingBoxes.stream().mapToInt(BoundingBox::getArea).average().orElse(0);

                    stats.put("totalArea", totalArea);
                    stats.put("minBoxArea", minArea);
                    stats.put("maxBoxArea", maxArea);
                    stats.put("avgBoxArea", avgArea);

                    double coverage = (double) totalArea / (texture.getWidth() * texture.getHeight()) * 100;
                    stats.put("textureCoveragePercent", coverage);
                }
                root.put("statistics", stats);

                // Generate filename
                String baseFilename = texture.getPath() != null ?
                        texture.getPath().replaceAll("\\.[^.]+$", "") : "texture";
                String filename = String.format("%s_collision_%s.json", baseFilename, method);

                // Write to file
                try (FileWriter fileWriter = new FileWriter(filename)) {
                    fileWriter.write(root.toString(2)); // Pretty print with 2-space indent
                }

                System.out.println("Collision shapes saved to: " + filename);

            } catch (Exception e) {
                System.err.println("Error saving collision shapes to JSON: " + e.getMessage());
                e.printStackTrace();
            }
        }

        /**
         * Loads collision shapes from JSON file
         */
        public static List<BoundingBox> loadCollisionShapesFromJson(String filename) {
            List<BoundingBox> boundingBoxes = new ArrayList<>();

            try {
                // Read JSON file
                String content = new String(java.nio.file.Files.readAllBytes(
                        java.nio.file.Paths.get(filename)));
                JSONObject root = new JSONObject(content);

                // Parse bounding boxes
                JSONArray boxesArray = root.getJSONArray("boundingBoxes");
                for (int i = 0; i < boxesArray.length(); i++) {
                    JSONObject boxJson = boxesArray.getJSONObject(i);
                    boundingBoxes.add(BoundingBox.fromJSON(boxJson));
                }

                // Print metadata for verification
                if (root.has("metadata")) {
                    JSONObject metadata = root.getJSONObject("metadata");
                    System.out.println("Loaded collision shapes: " + metadata.getInt("boundingBoxCount") +
                            " boxes using " + metadata.getString("method") + " method");
                }

            } catch (Exception e) {
                System.err.println("Error loading collision shapes from JSON: " + e.getMessage());
                e.printStackTrace();
            }

            return boundingBoxes;
        }

        public int getArea() {
            return width * height;
        }

        /**
         * Converts this BoundingBox to a JSON object
         */
        public JSONObject toJSON() {
            JSONObject json = new JSONObject();
            json.put("minX", minX);
            json.put("minY", minY);
            json.put("maxX", maxX);
            json.put("maxY", maxY);
            json.put("width", width);
            json.put("height", height);
            json.put("area", getArea());
            return json;
        }

        /**
         * Creates a BoundingBox from a JSON object
         */
        public static BoundingBox fromJSON(JSONObject json) {
            return new BoundingBox(
                    json.getInt("minX"),
                    json.getInt("minY"),
                    json.getInt("maxX"),
                    json.getInt("maxY")
            );
        }

        @Override
        public String toString() {
            return String.format("BBox[(%d,%d) to (%d,%d), %dx%d]",
                    minX, minY, maxX, maxY, width, height);
        }
    }

    /**
     * Converts a transparency map into bounding boxes for collision detection.
     * Uses flood fill to find connected opaque regions and creates AABBs for each.
     *
     * @param transparencyMap boolean array where true = transparent, false = opaque
     * @param width image width
     * @param height image height
     * @param minRegionSize minimum pixels in a region to create a bounding box
     * @param save whether to save the results to JSON
     * @param texture the texture object (for JSON filename generation)
     * @return List of bounding boxes representing opaque regions
     */
    public static List<BoundingBox> generateCollisionShapes(boolean[] transparencyMap,
                                                            int width, int height,
                                                            int minRegionSize,
                                                            boolean save,
                                                            Texture texture) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boolean[] visited = new boolean[width * height];

        // Find all connected opaque regions
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;

                // Skip if already visited or if pixel is transparent
                if (visited[index] || transparencyMap[index]) {
                    continue;
                }

                // Flood fill to find connected opaque region
                List<Point> region = floodFill(transparencyMap, visited, x, y, width, height);

                // Only create bounding box if region is large enough
                if (region.size() >= minRegionSize) {
                    BoundingBox bbox = createBoundingBox(region);
                    boundingBoxes.add(bbox);
                }
            }
        }

        if (save && texture != null) {
            saveCollisionShapesToJson(texture, boundingBoxes, "connected", minRegionSize);
        }

        return boundingBoxes;
    }

    /**
     * Overloaded method with default minimum region size and no save
     */
    public static List<BoundingBox> generateCollisionShapes(boolean[] transparencyMap,
                                                            int width, int height) {
        return generateCollisionShapes(transparencyMap, width, height, 4, false, null);
    }

    /**
     * Overloaded method with save option
     */
    public static List<BoundingBox> generateCollisionShapes(boolean[] transparencyMap,
                                                            int width, int height,
                                                            boolean save,
                                                            Texture texture) {
        return generateCollisionShapes(transparencyMap, width, height, 4, save, texture);
    }

    /**
     * Flood fill algorithm to find connected opaque pixels
     */
    private static List<Point> floodFill(boolean[] transparencyMap, boolean[] visited,
                                         int startX, int startY, int width, int height) {
        List<Point> region = new ArrayList<>();
        Stack<Point> stack = new Stack<>();
        stack.push(new Point(startX, startY));

        while (!stack.isEmpty()) {
            Point p = stack.pop();
            int x = p.x;
            int y = p.y;
            int index = y * width + x;

            // Skip if out of bounds, already visited, or transparent
            if (x < 0 || x >= width || y < 0 || y >= height ||
                    visited[index] || transparencyMap[index]) {
                continue;
            }

            visited[index] = true;
            region.add(p);

            // Add 4-connected neighbors
            stack.push(new Point(x + 1, y));
            stack.push(new Point(x - 1, y));
            stack.push(new Point(x, y + 1));
            stack.push(new Point(x, y - 1));
        }

        return region;
    }

    /**
     * Creates a bounding box from a list of points
     */
    private static BoundingBox createBoundingBox(List<Point> points) {
        if (points.isEmpty()) {
            return new BoundingBox(0, 0, 0, 0);
        }

        int minX = points.get(0).x;
        int maxX = points.get(0).x;
        int minY = points.get(0).y;
        int maxY = points.get(0).y;

        for (Point p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }

        return new BoundingBox(minX, minY, maxX, maxY);
    }

    /**
     * Alternative method that creates a simplified collision shape using
     * rectangular decomposition for better performance with complex shapes
     */
    public static List<BoundingBox> generateOptimizedCollisionShapes(boolean[] transparencyMap,
                                                                     int width, int height,
                                                                     int maxBoxSize,
                                                                     boolean save,
                                                                     Texture texture) {
        List<BoundingBox> boundingBoxes = new ArrayList<>();
        boolean[] processed = new boolean[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;

                // Skip if already processed or transparent
                if (processed[index] || transparencyMap[index]) {
                    continue;
                }

                // Find largest rectangle starting at this point
                BoundingBox rect = findLargestRectangle(transparencyMap, processed,
                        x, y, width, height, maxBoxSize);
                if (rect.getArea() > 0) {
                    boundingBoxes.add(rect);
                    markRectangleAsProcessed(processed, rect, width);
                }
            }
        }

        if (save && texture != null) {
            saveCollisionShapesToJson(texture, boundingBoxes, "optimized", maxBoxSize);
        }

        return boundingBoxes;
    }

    /**
     * Overloaded method without save option
     */
    public static List<BoundingBox> generateOptimizedCollisionShapes(boolean[] transparencyMap,
                                                                     int width, int height,
                                                                     int maxBoxSize) {
        return generateOptimizedCollisionShapes(transparencyMap, width, height, maxBoxSize, false, null);
    }

    /**
     * Finds the largest rectangle of opaque pixels starting at (x, y)
     */
    private static BoundingBox findLargestRectangle(boolean[] transparencyMap, boolean[] processed,
                                                    int startX, int startY, int width, int height,
                                                    int maxSize) {
        int bestArea = 0;
        BoundingBox bestRect = new BoundingBox(startX, startY, startX, startY);

        // Try different rectangle sizes
        for (int w = 1; w <= Math.min(maxSize, width - startX); w++) {
            for (int h = 1; h <= Math.min(maxSize, height - startY); h++) {
                if (isRectangleValid(transparencyMap, processed, startX, startY, w, h, width, height)) {
                    int area = w * h;
                    if (area > bestArea) {
                        bestArea = area;
                        bestRect = new BoundingBox(startX, startY, startX + w - 1, startY + h - 1);
                    }
                }
            }
        }

        return bestRect;
    }

    /**
     * Checks if a rectangle contains only opaque, unprocessed pixels
     */
    private static boolean isRectangleValid(boolean[] transparencyMap, boolean[] processed,
                                            int x, int y, int w, int h, int width, int height) {
        for (int dy = 0; dy < h; dy++) {
            for (int dx = 0; dx < w; dx++) {
                int px = x + dx;
                int py = y + dy;
                if (px >= width || py >= height) return false;

                int index = py * width + px;
                if (transparencyMap[index] || processed[index]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Marks all pixels in a rectangle as processed
     */
    private static void markRectangleAsProcessed(boolean[] processed, BoundingBox rect, int width) {
        for (int y = rect.minY; y <= rect.maxY; y++) {
            for (int x = rect.minX; x <= rect.maxX; x++) {
                processed[y * width + x] = true;
            }
        }
    }

    // Simple Point class
    private static class Point {
        final int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }

    // Example usage and testing method
    public static void demonstrateUsage() {
        // Example: 8x6 transparency map with some opaque regions
        boolean[] testMap = {
                true,  true,  false, false, true,  true,  true,  true,   // Row 0
                true,  false, false, false, false, true,  true,  true,   // Row 1
                false, false, false, false, false, false, true,  true,   // Row 2
                false, false, true,  true,  false, false, false, true,   // Row 3
                false, false, true,  true,  false, false, false, false,  // Row 4
                true,  true,  true,  true,  true,  false, false, false   // Row 5
        };

        int width = 8, height = 6;

        System.out.println("=== Connected Regions Method ===");
        //List<BoundingBox> boxes1 = generateCollisionShapes(testMap, width, height, 2);
        //for (BoundingBox box : boxes1) {
        //    System.out.println(box);
        //}

        System.out.println("\n=== Optimized Rectangular Decomposition ===");
        List<BoundingBox> boxes2 = generateOptimizedCollisionShapes(testMap, width, height, 10);
        for (BoundingBox box : boxes2) {
            System.out.println(box);
        }
    }
}