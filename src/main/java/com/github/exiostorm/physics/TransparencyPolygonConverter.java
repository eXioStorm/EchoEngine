package com.github.exiostorm.physics;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import org.json.*;
import java.io.*;
import java.nio.file.*;

//TODO [!][!!][!!!][20250813]
// Big ol' hunk of AI slop. going to take awhile to go through and personally identify contents of this class.

//TODO [!][!!][!!!][20250813]
// Maybe forget about polygon generation for now, just generate a simple "square" polygon to start, hand have the information handling polygon collision?
public class TransparencyPolygonConverter {

    /**
     * Converts a transparency map into a list of polygons representing non-transparent areas
     */
    public static List<Polygon> generatePolygonsFromTransparencyMap(boolean[] transparencyMap, int width, int height, boolean save, String textureId) {
        if (transparencyMap == null) return new ArrayList<>();

        // Try to load existing polygons first
        if (save) {
            List<Polygon> cached = loadPolygonsFromJson(textureId);
            if (cached != null) return cached;
        }

        // Create a working copy of the transparency map
        boolean[][] visited = new boolean[height][width];
        List<Polygon> polygons = new ArrayList<>();

        // Find all connected non-transparent regions
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                if (!transparencyMap[index] && !visited[y][x]) { // Non-transparent and not visited
                    Polygon polygon = tracePolygon(transparencyMap, visited, width, height, x, y);
                    if (polygon != null && polygon.npoints > 2) {
                        polygons.add(polygon);
                    }
                }
            }
        }

        // Simplify polygons to reduce vertex count
        polygons = simplifyPolygons(polygons);

        if (save) {
            savePolygonsToJson(textureId, polygons);
        }

        return polygons;
    }

    /**
     * Traces the outline of a non-transparent region using Moore neighborhood tracing
     */
    private static Polygon tracePolygon(boolean[] transparencyMap, boolean[][] visited, int width, int height, int startX, int startY) {
        List<Point> boundary = new ArrayList<>();

        // Find the leftmost pixel of this region
        Point start = findLeftmostPixel(transparencyMap, width, height, startX, startY);
        if (start == null) return null;

        // Moore neighborhood directions (8-connected)
        int[] dx = {-1, -1, -1, 0, 1, 1, 1, 0};
        int[] dy = {-1, 0, 1, 1, 1, 0, -1, -1};

        Point current = new Point(start.x, start.y);
        int direction = 7; // Start looking left

        do {
            boundary.add(new Point(current.x, current.y));
            markRegionAsVisited(transparencyMap, visited, width, height, current.x, current.y);

            // Find next boundary pixel
            int nextDir = (direction + 6) % 8; // Start looking 90 degrees counterclockwise
            Point next = null;

            for (int i = 0; i < 8; i++) {
                int newX = current.x + dx[nextDir];
                int newY = current.y + dy[nextDir];

                if (isValidPixel(newX, newY, width, height) &&
                        !transparencyMap[newY * width + newX]) { // Non-transparent
                    next = new Point(newX, newY);
                    direction = nextDir;
                    break;
                }
                nextDir = (nextDir + 1) % 8;
            }

            if (next == null) break;
            current = next;

        } while (!current.equals(start) && boundary.size() < width * height);

        if (boundary.size() < 3) return null;

        // Convert to Polygon
        Polygon polygon = new Polygon();
        for (Point p : boundary) {
            polygon.addPoint(p.x, p.y);
        }

        return polygon;
    }

    /**
     * Finds the leftmost non-transparent pixel in a region
     */
    private static Point findLeftmostPixel(boolean[] transparencyMap, int width, int height, int startX, int startY) {
        // Use flood fill to find all pixels in this region, then return leftmost
        Queue<Point> queue = new ArrayDeque<>();
        Set<Point> region = new HashSet<>();
        queue.add(new Point(startX, startY));

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty() && region.size() < 10000) { // Limit to prevent infinite loops
            Point p = queue.poll();
            if (region.contains(p)) continue;

            int index = p.y * width + p.x;
            if (transparencyMap[index]) continue; // Skip transparent pixels

            region.add(p);

            for (int i = 0; i < 4; i++) {
                int newX = p.x + dx[i];
                int newY = p.y + dy[i];
                Point newPoint = new Point(newX, newY);

                if (isValidPixel(newX, newY, width, height) && !region.contains(newPoint)) {
                    queue.add(newPoint);
                }
            }
        }

        return region.stream().min(Comparator.comparingInt(p -> p.x)).orElse(null);
    }

    /**
     * Marks all pixels in a region as visited using flood fill
     */
    private static void markRegionAsVisited(boolean[] transparencyMap, boolean[][] visited, int width, int height, int startX, int startY) {
        Queue<Point> queue = new ArrayDeque<>();
        queue.add(new Point(startX, startY));

        int[] dx = {-1, 1, 0, 0};
        int[] dy = {0, 0, -1, 1};

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (visited[p.y][p.x]) continue;

            int index = p.y * width + p.x;
            if (transparencyMap[index]) continue; // Skip transparent pixels

            visited[p.y][p.x] = true;

            for (int i = 0; i < 4; i++) {
                int newX = p.x + dx[i];
                int newY = p.y + dy[i];

                if (isValidPixel(newX, newY, width, height) && !visited[newY][newX]) {
                    queue.add(new Point(newX, newY));
                }
            }
        }
    }

    private static boolean isValidPixel(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Simplifies polygons by removing redundant vertices
     */
    private static List<Polygon> simplifyPolygons(List<Polygon> polygons) {
        List<Polygon> simplified = new ArrayList<>();

        for (Polygon poly : polygons) {
            Polygon simplifiedPoly = douglasPeucker(poly, 1.0); // Tolerance of 1 pixel
            if (simplifiedPoly.npoints > 2) {
                simplified.add(simplifiedPoly);
            }
        }

        return simplified;
    }

    /**
     * Douglas-Peucker algorithm for polygon simplification
     */
    private static Polygon douglasPeucker(Polygon polygon, double tolerance) {
        if (polygon.npoints <= 2) return polygon;

        List<Point> points = new ArrayList<>();
        for (int i = 0; i < polygon.npoints; i++) {
            points.add(new Point(polygon.xpoints[i], polygon.ypoints[i]));
        }

        List<Point> simplified = douglasPeuckerRecursive(points, tolerance);

        Polygon result = new Polygon();
        for (Point p : simplified) {
            result.addPoint(p.x, p.y);
        }

        return result;
    }

    private static List<Point> douglasPeuckerRecursive(List<Point> points, double tolerance) {
        if (points.size() <= 2) return points;

        double maxDistance = 0;
        int maxIndex = 0;

        Point start = points.get(0);
        Point end = points.get(points.size() - 1);

        for (int i = 1; i < points.size() - 1; i++) {
            double distance = perpendicularDistance(points.get(i), start, end);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxIndex = i;
            }
        }

        if (maxDistance > tolerance) {
            List<Point> left = douglasPeuckerRecursive(points.subList(0, maxIndex + 1), tolerance);
            List<Point> right = douglasPeuckerRecursive(points.subList(maxIndex, points.size()), tolerance);

            List<Point> result = new ArrayList<>(left);
            result.addAll(right.subList(1, right.size())); // Remove duplicate point
            return result;
        } else {
            List<Point> result = new ArrayList<>();
            result.add(start);
            result.add(end);
            return result;
        }
    }

    private static double perpendicularDistance(Point point, Point lineStart, Point lineEnd) {
        double dx = lineEnd.x - lineStart.x;
        double dy = lineEnd.y - lineStart.y;

        if (dx == 0 && dy == 0) {
            return Math.sqrt(Math.pow(point.x - lineStart.x, 2) + Math.pow(point.y - lineStart.y, 2));
        }

        double t = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double projX = lineStart.x + t * dx;
        double projY = lineStart.y + t * dy;

        return Math.sqrt(Math.pow(point.x - projX, 2) + Math.pow(point.y - projY, 2));
    }

    /**
     * Save polygons to JSON file
     */
    private static void savePolygonsToJson(String textureId, List<Polygon> polygons) {
        try {
            JSONObject json = new JSONObject();
            JSONArray polygonArray = new JSONArray();

            for (Polygon poly : polygons) {
                JSONObject polygonJson = new JSONObject();
                JSONArray xpoints = new JSONArray();
                JSONArray ypoints = new JSONArray();

                for (int i = 0; i < poly.npoints; i++) {
                    xpoints.put(poly.xpoints[i]);
                    ypoints.put(poly.ypoints[i]);
                }

                polygonJson.put("xpoints", xpoints);
                polygonJson.put("ypoints", ypoints);
                polygonJson.put("npoints", poly.npoints);
                polygonArray.put(polygonJson);
            }

            json.put("polygons", polygonArray);
            json.put("textureId", textureId);

            Path path = Paths.get("polygon_cache_" + textureId + ".json");
            Files.write(path, json.toString(2).getBytes());

        } catch (Exception e) {
            System.err.println("Failed to save polygons to JSON: " + e.getMessage());
        }
    }

    /**
     * Load polygons from JSON file
     */
    private static List<Polygon> loadPolygonsFromJson(String textureId) {
        try {
            Path path = Paths.get("polygon_cache_" + textureId + ".json");
            if (!Files.exists(path)) return null;

            String content = new String(Files.readAllBytes(path));
            JSONObject json = new JSONObject(content);
            JSONArray polygonArray = json.getJSONArray("polygons");

            List<Polygon> polygons = new ArrayList<>();

            for (int i = 0; i < polygonArray.length(); i++) {
                JSONObject polygonJson = polygonArray.getJSONObject(i);
                JSONArray xpoints = polygonJson.getJSONArray("xpoints");
                JSONArray ypoints = polygonJson.getJSONArray("ypoints");
                int npoints = polygonJson.getInt("npoints");

                Polygon polygon = new Polygon();
                for (int j = 0; j < npoints; j++) {
                    polygon.addPoint(xpoints.getInt(j), ypoints.getInt(j));
                }
                polygons.add(polygon);
            }

            return polygons;

        } catch (Exception e) {
            System.err.println("Failed to load polygons from JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if a point is inside any of the polygons (for UI click detection)
     */
    public static boolean isPointInPolygons(List<Polygon> polygons, int x, int y) {
        for (Polygon polygon : polygons) {
            if (polygon.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Example UI component that uses polygon-based collision detection
     */
    public static class IrregularUIComponent extends Component {
        private List<Polygon> collisionPolygons;
        private String textureId;
        private boolean[] transparencyMap;
        private int textureWidth, textureHeight;

        public IrregularUIComponent(String textureId, boolean[] transparencyMap, int width, int height) {
            this.textureId = textureId;
            this.transparencyMap = transparencyMap;
            this.textureWidth = width;
            this.textureHeight = height;

            // Generate collision polygons
            this.collisionPolygons = generatePolygonsFromTransparencyMap(
                    transparencyMap, width, height, true, textureId
            );

            setSize(width, height);
        }

        /**
         * Handle mouse clicks with irregular collision detection
         */
        public boolean handleMouseClick(int mouseX, int mouseY) {
            // Convert screen coordinates to texture coordinates if needed
            int textureX = mouseX - getX();
            int textureY = mouseY - getY();

            // Check if click is within bounds
            if (textureX < 0 || textureX >= textureWidth || textureY < 0 || textureY >= textureHeight) {
                return false;
            }

            // Check if click is inside any collision polygon
            boolean hit = isPointInPolygons(collisionPolygons, textureX, textureY);

            if (hit) {
                onClicked(textureX, textureY);
                return true;
            }

            return false;
        }

        /**
         * Override this method to handle clicks on the irregular shape
         */
        protected void onClicked(int x, int y) {
            System.out.println("Irregular UI component clicked at: " + x + ", " + y);
        }

        public List<Polygon> getCollisionPolygons() {
            return collisionPolygons;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);

            // Optional: Draw collision polygons for debugging
            if (Boolean.getBoolean("debug.collision")) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(1));

                for (Polygon poly : collisionPolygons) {
                    g2d.draw(poly);
                }
            }
        }
    }

    // Usage example
    public static void main(String[] args) {
        // Example usage with your existing generateTransparencyMap method
        // Assuming you have a Texture object
        /*
        boolean[] transparencyMap = generateTransparencyMap(texture, true);

        // Generate collision polygons
        List<Polygon> polygons = generatePolygonsFromTransparencyMap(
            transparencyMap, texture.getWidth(), texture.getHeight(), true, "my_texture_id"
        );

        // Create UI component
        IrregularUIComponent uiComponent = new IrregularUIComponent(
            "my_texture_id", transparencyMap, texture.getWidth(), texture.getHeight()
        ) {
            @Override
            protected void onClicked(int x, int y) {
                System.out.println("My custom UI element was clicked at: " + x + ", " + y);
                // Handle the click event here
            }
        };

        // In your mouse event handler:
        // if (uiComponent.handleMouseClick(mouseX, mouseY)) {
        //     // Click was handled by the irregular UI component
        // }
        */
    }
}