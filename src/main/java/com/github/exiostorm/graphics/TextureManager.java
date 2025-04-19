package com.github.exiostorm.graphics;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
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
            saveTransparencyMapToJson(texture, transparencyMap);
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
            boolean[] loadedMap = loadTransparencyMapFromJson(texture);
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
            saveTransparencyMapToJson(texture, texture.getTransparencyMap());
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
    private static void loadDimensionsFromJson(Texture texture, File jsonFile) {
        System.out.println("FOUND EXISTING DIMENSIONS JSON FILE! : " + jsonFile.getPath());

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
            System.out.println("JSON values read. Width: " + texture.getWidth() + ", Height: " + texture.getHeight());
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
            BufferedImage image = generateBufferedImage(texture, true);
            texture.setWidth(image.getWidth());
            texture.setHeight(image.getHeight());
        }
    }
    private static boolean[] loadTransparencyMapFromJson(Texture texture) {
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

            // Only try to load transparency map
            if (jsonObject.has("transparencyMap")) {
                JSONArray transparencyArray = jsonObject.getJSONArray("transparencyMap");
                boolean[] transparencyMap = new boolean[transparencyArray.length()];

                for (int i = 0; i < transparencyArray.length(); i++) {
                    transparencyMap[i] = transparencyArray.getInt(i) == 1;
                }

                System.out.println("Loaded transparency map from JSON with " + transparencyMap.length + " entries");
                return transparencyMap;
            }
        } catch (IOException e) {
            System.err.println("Error reading transparency map from JSON: " + e.getMessage());
        }
        return null;
    }
    // Fixed method to avoid circular dependency
    private static void saveTransparencyMapToJson(Texture texture, boolean[] transparencyMap) {
        String jsonPath = getJsonPath(texture);
        JSONObject jsonObject = new JSONObject();

        // Save dimensions
        jsonObject.put("width", texture.getWidth());
        jsonObject.put("height", texture.getHeight());

        // Save transparency map
        if (transparencyMap != null) {
            JSONArray transparencyArray = new JSONArray();
            for (boolean isTransparent : transparencyMap) {
                transparencyArray.put(isTransparent ? 1 : 0);
            }
            jsonObject.put("transparencyMap", transparencyArray);
        }

        try (FileWriter fileWriter = new FileWriter(jsonPath)) {
            fileWriter.write(jsonObject.toString(0));
            System.out.println("Transparency map saved to JSON: " + jsonPath);
        } catch (IOException e) {
            System.err.println("Error saving transparency map to JSON: " + e.getMessage());
        }
    }
    public static Map<String, Texture> getTextures() {
        return Textures;
    }
}