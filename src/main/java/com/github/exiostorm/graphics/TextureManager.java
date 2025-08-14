package com.github.exiostorm.graphics;

import com.github.exiostorm.utils.ShapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

public class TextureManager {
    static Map<String, Texture> Textures = new HashMap<>();

    public static Texture addTexture(String path) {
        if (!Textures.containsKey(path)) {
            Texture texture = new Texture(path);
            Textures.putIfAbsent(texture.getPath(), texture);
            return texture;
        } else {
            return Textures.get(path);
        }
    }
    public static ByteBuffer generateByteBuffer(Texture texture, byte saveFlag) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        // Use generateBufferedImage() to fetch the image
        BufferedImage image = generateBufferedImage(texture, false);

        // Extract pixel data from the BufferedImage
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        // Allocate ByteBuffer for texture data
        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
        boolean[] transparencyMap = new boolean[width * height];
        for (int i = 0; i < width * height; i++) {
            int pixel = pixels[i];
            int alpha = (pixel >> 24) & 0xFF; // Extract alpha
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
            buffer.put((byte) (pixel & 0xFF));         // Blue
            buffer.put((byte) alpha);                 // Alpha
            if (saveFlag != 0) {
                if (saveFlag == 7 || saveFlag == 6 || saveFlag == 5 || saveFlag == 4) {
                    transparencyMap[i] = alpha == 0;
                }
            }
        }
        buffer.flip();
        /*  000
            001
            010
            011
            100
            101
            110
            111 */
        if (saveFlag == 7 || saveFlag == 6 || saveFlag == 5 || saveFlag == 4) {
            texture.setTransparencyMap(transparencyMap); // Populate transparency map if requested
        }
        if (saveFlag == 7 || saveFlag == 6 || saveFlag == 3 || saveFlag == 2) {
            texture.setBufferedImage(image); // Populate buffered image if requested
        }
        if (saveFlag == 7 || saveFlag == 5 || saveFlag == 3 || saveFlag == 1) {
            texture.setByteBuffer(buffer); // Populate ByteBuffer if requested
        }
        return buffer;
    }
    public static BufferedImage generateBufferedImage(Texture texture, boolean save) {
        if (texture.getBufferedImage() != null) {
            return texture.getBufferedImage();
        }
        try {
            BufferedImage image = ImageIO.read(new FileInputStream(texture.getPath()));
            if (save) {
                texture.setBufferedImage(image);
            }
            return image;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load BufferedImage: " + texture.getPath(), e);
        }
    }
    public static boolean[] generateTransparencyMap(Texture texture, boolean save) {
        if (texture.getTransparencyMap() != null) {
            return texture.getTransparencyMap();
        }

        int width = texture.getWidth();
        int height = texture.getHeight();

        // Generate buffer without saving to avoid circular reference
        BufferedImage image = generateBufferedImage(texture, false);

        // Extract pixel data directly
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        boolean[] transparencyMap = new boolean[width * height];
        for (int i = 0; i < width * height; i++) {
            int alpha = (pixels[i] >> 24) & 0xFF; // Extract alpha
            transparencyMap[i] = alpha == 0; // Fully transparent
        }

        if (save) {
            texture.setTransparencyMap(transparencyMap);
            // Save to JSON without causing a circular reference
            saveTextureToJson(texture, transparencyMap);
        }

        return transparencyMap;
    }

    public static boolean[] getOrGenerateTransparencyMap(Texture texture, boolean save) {
        // Check if transparency map is already generated
        if (texture.getTransparencyMap() != null) {
            return texture.getTransparencyMap();
        }

        String jsonPath = getJsonPath(texture);
        File jsonFile = new File(jsonPath);

        // Try loading from JSON first
        if (jsonFile.exists()) {
            boolean[] loadedMap = loadTextureFromJson(texture);
            if (loadedMap != null) {
                if (save) {
                    texture.setTransparencyMap(loadedMap);
                }
                return loadedMap;
            }
        }

        // If we reach here, we need to generate the map
        return generateTransparencyMap(texture, save);
    }

    private static String getJsonPath(Texture texture) {
        return texture.getPath().substring(0, texture.getPath().lastIndexOf('.')) + ".json";
    }
    //TODO [0] bug here, our transparency map is saving as all opaque for some reason.
    // This method is now separate to avoid the circular reference
    public static void saveTextureData(Texture texture) {
        // Only save if we don't already have the transparency map
        if (texture.getTransparencyMap() == null) {
            boolean[] transparencyMap = generateTransparencyMap(texture, true);
            // The map is already saved by generateTransparencyMap
        } else {
            // Just save what we already have
            saveTextureToJson(texture, texture.getTransparencyMap());
        }
    }

    public static void getOrGenerateDimensions(Texture texture) {
        String jsonPath = getJsonPath(texture);
        File jsonFile = new File(jsonPath);

        if (jsonFile.exists()) {
            // Only load dimensions from JSON
            loadDimensionsFromJson(texture, jsonFile);
        } else {
            // Generate and save only dimensions
            BufferedImage image = generateBufferedImage(texture, true);
            texture.setWidth(image.getWidth());
            texture.setHeight(image.getHeight());

            // Save dimensions without causing circular reference
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("width", texture.getWidth());
            jsonObject.put("height", texture.getHeight());

            try (FileWriter fileWriter = new FileWriter(jsonPath)) {
                fileWriter.write(jsonObject.toString(0));
            } catch (IOException e) {
                System.err.println("Error saving dimensions to JSON: " + e.getMessage());
            }
        }
    }
    //TODO [!][!!][!!!][20250814@2:04am]
    // methods for polygon json data need to be made in this class~

    private static void loadDimensionsFromJson(Texture texture, File jsonFile) {
        //System.out.println("FOUND EXISTING DIMENSIONS JSON FILE! : " + jsonFile.getPath());

        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonContent.toString());

            // Only load dimensions
            texture.setWidth(jsonObject.getInt("width"));
            texture.setHeight(jsonObject.getInt("height"));
            //System.out.println("JSON values read. Width: " + texture.getWidth() + ", Height: " + texture.getHeight());
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
            BufferedImage image = generateBufferedImage(texture, true);
            texture.setWidth(image.getWidth());
            texture.setHeight(image.getHeight());
        }
    }
    /**
     * Load polygon data from JSON file if it exists, otherwise return null
     */
    public static Polygon loadPolygonFromJson(Texture texture) {
        String jsonPath = getJsonPath(texture);
        File jsonFile = new File(jsonPath);

        if (!jsonFile.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonContent.toString());

            // Check if polygon data exists
            if (jsonObject.has("polygon")) {
                JSONObject polygonObject = jsonObject.getJSONObject("polygon");

                if (polygonObject.has("xpoints") && polygonObject.has("ypoints") && polygonObject.has("npoints")) {
                    JSONArray xpointsArray = polygonObject.getJSONArray("xpoints");
                    JSONArray ypointsArray = polygonObject.getJSONArray("ypoints");
                    int npoints = polygonObject.getInt("npoints");

                    // Validate data consistency
                    if (xpointsArray.length() != ypointsArray.length() || xpointsArray.length() != npoints) {
                        System.err.println("Inconsistent polygon data in JSON file");
                        return null;
                    }

                    // Convert JSONArrays to int arrays
                    int[] xpoints = new int[npoints];
                    int[] ypoints = new int[npoints];

                    for (int i = 0; i < npoints; i++) {
                        xpoints[i] = xpointsArray.getInt(i);
                        ypoints[i] = ypointsArray.getInt(i);
                    }

                    Polygon polygon = new Polygon(xpoints, ypoints, npoints);
                    System.out.println("Loaded polygon from JSON with " + npoints + " points");
                    return polygon;
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading polygon from JSON: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing polygon data from JSON: " + e.getMessage());
        }

        return null;
    }

    /**
     * Save polygon data to JSON file (merges with existing data)
     */
    public static void savePolygonToJson(Texture texture, Polygon polygon) {
        if (polygon == null) {
            return;
        }

        String jsonPath = getJsonPath(texture);
        JSONObject jsonObject = new JSONObject();

        // Load existing JSON data if file exists
        File jsonFile = new File(jsonPath);
        if (jsonFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                jsonObject = new JSONObject(jsonContent.toString());
            } catch (IOException e) {
                System.err.println("Error reading existing JSON file: " + e.getMessage());
                // Continue with empty JSON object
            }
        }

        // Add or update polygon data
        JSONObject polygonObject = new JSONObject();

        JSONArray xpointsArray = new JSONArray();
        JSONArray ypointsArray = new JSONArray();

        for (int i = 0; i < polygon.npoints; i++) {
            xpointsArray.put(polygon.xpoints[i]);
            ypointsArray.put(polygon.ypoints[i]);
        }

        polygonObject.put("xpoints", xpointsArray);
        polygonObject.put("ypoints", ypointsArray);
        polygonObject.put("npoints", polygon.npoints);

        jsonObject.put("polygon", polygonObject);

        // Save to file
        try (FileWriter fileWriter = new FileWriter(jsonPath)) {
            fileWriter.write(jsonObject.toString(0));
            System.out.println("Polygon saved to JSON: " + jsonPath);
        } catch (IOException e) {
            System.err.println("Error saving polygon to JSON: " + e.getMessage());
        }
    }

    /**
     * Get or generate polygon data for texture
     */
    public static Polygon getOrGeneratePolygon(Texture texture) {
        // First try to load from JSON
        Polygon polygon = loadPolygonFromJson(texture);

        if (polygon != null) {
            return polygon;
        }

        // If not found, generate polygon (you'll need to implement this based on your logic)
        polygon = ShapeUtils.generateSimplePolygon(texture.getWidth(), texture.getHeight());;

        if (polygon != null) {
            // Save the generated polygon for future use
            savePolygonToJson(texture, polygon);
        }

        return polygon;
    }
    private static boolean[] loadTextureFromJson(Texture texture) {
        String jsonPath = getJsonPath(texture);
        File jsonFile = new File(jsonPath);

        if (!jsonFile.exists()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonContent.toString());

            // Load transparency map from hex string
            if (jsonObject.has("transparencyMapHex") && jsonObject.has("transparencyMapLength")) {
                String hexString = jsonObject.getString("transparencyMapHex");
                int length = jsonObject.getInt("transparencyMapLength");

                boolean[] transparencyMap = hexStringToBooleanArray(hexString, length);

                System.out.println("Loaded transparency map from JSON with " + transparencyMap.length + " entries");
                return transparencyMap;
            }
        } catch (IOException e) {
            System.err.println("Error reading transparency map from JSON: " + e.getMessage());
        }
        return null;
    }

    private static void saveTextureToJson(Texture texture, boolean[] transparencyMap) {
        String jsonPath = getJsonPath(texture);
        JSONObject jsonObject = new JSONObject();

        // Save dimensions
        jsonObject.put("width", texture.getWidth());
        jsonObject.put("height", texture.getHeight());

        // Save transparency map as hex string
        if (transparencyMap != null) {
            String hexString = booleanArrayToHexString(transparencyMap);
            jsonObject.put("transparencyMapHex", hexString);
            jsonObject.put("transparencyMapLength", transparencyMap.length);
        }
        //TODO [!][!!][!!!][20250815@12:06am]
        // add FileWriter to Texture so that all these methods can just access it without having to create it every time.
        try (FileWriter fileWriter = new FileWriter(jsonPath)) {
            fileWriter.write(jsonObject.toString(0));
            System.out.println("Transparency map saved to JSON: " + jsonPath);
        } catch (IOException e) {
            System.err.println("Error saving transparency map to JSON: " + e.getMessage());
        }
    }

    /**
     * Convert boolean array to hexadecimal string representation
     * Each bit represents one boolean value, packed into bytes
     */
    private static String booleanArrayToHexString(boolean[] booleans) {
        if (booleans == null || booleans.length == 0) {
            return "";
        }

        // Calculate number of bytes needed (round up)
        int numBytes = (booleans.length + 7) / 8;
        byte[] bytes = new byte[numBytes];

        // Pack booleans into bits
        for (int i = 0; i < booleans.length; i++) {
            if (booleans[i]) {
                int byteIndex = i / 8;
                int bitIndex = i % 8;
                bytes[byteIndex] |= (1 << bitIndex);
            }
        }

        // Convert bytes to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b & 0xFF));
        }

        return hexString.toString();
    }

    /**
     * Convert hexadecimal string back to boolean array
     * @param hexString the hex representation
     * @param length the original array length (needed because of potential padding)
     */
    private static boolean[] hexStringToBooleanArray(String hexString, int length) {
        // I sure hope that I never have to read/debug this...
        if (hexString == null || hexString.isEmpty() || length <= 0) {
            return new boolean[0];
        }

        boolean[] booleans = new boolean[length];

        // Convert hex string to bytes
        int numBytes = hexString.length() / 2;
        byte[] bytes = new byte[numBytes];

        for (int i = 0; i < numBytes; i++) {
            String byteString = hexString.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(byteString, 16);
        }

        // Extract bits back to booleans
        for (int i = 0; i < length; i++) {
            int byteIndex = i / 8;
            int bitIndex = i % 8;

            if (byteIndex < bytes.length) {
                booleans[i] = (bytes[byteIndex] & (1 << bitIndex)) != 0;
            }
        }

        return booleans;
    }
    public static Map<String, Texture> getTextures() {
        return Textures;
    }
}