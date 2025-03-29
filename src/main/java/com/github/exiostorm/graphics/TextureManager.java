package com.github.exiostorm.graphics;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.stb.STBImage.stbi_image_free;

public class TextureManager {
    static Map<String, Texture> Textures = new HashMap<>();

    public static Texture addTexture(String path) {
        Texture texture = new Texture(path);
        Textures.putIfAbsent(texture.getPath(), texture);
        return texture;
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
        ByteBuffer textureData = generateByteBuffer(texture, (byte) 0);
        boolean[] transparencyMap = new boolean[width * height];
        for (int i = 0; i < width * height; i++) {
            int alpha = textureData.get(i * 4 + 3) & 0xFF; // Extract alpha
            transparencyMap[i] = alpha == 0; // Fully transparent
        }
        if (save) {
            texture.setTransparencyMap(transparencyMap);
        }
        return transparencyMap;
    }
    //TODO maybe have another class handle the json logic so we can reduce imports used in this class? maybe not cause then we'd need our own import..
    public static void getOrGenerateDimensions(Texture texture) {
        String jsonPath = texture.getPath().substring(0, texture.getPath().lastIndexOf('.')) + ".json";
        File jsonFile = new File(jsonPath);
        if (jsonFile.exists()) {
            System.out.println("FOUND EXISTING DIMENSIONS JSON FILE! : "+jsonPath);
            try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                JSONObject jsonObject = new JSONObject(jsonContent.toString());
                texture.setWidth(jsonObject.getInt("width"));
                texture.setHeight(jsonObject.getInt("height"));
                System.out.println("JSON values read. Width: " + texture.getWidth() + ", Height: " + texture.getHeight());
            } catch (IOException e) {
                System.err.println("Error reading JSON file: " + e.getMessage());
                generateBufferedImage(texture, true);
                texture.setWidth(texture.getBufferedImage().getWidth());
                texture.setHeight(texture.getBufferedImage().getHeight());
            }
        } else {
            generateBufferedImage(texture, true);
            texture.setWidth(texture.getBufferedImage().getWidth());
            texture.setHeight(texture.getBufferedImage().getHeight());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("width", texture.getWidth());
            jsonObject.put("height", texture.getHeight());
            try (FileWriter fileWriter = new FileWriter(jsonPath)) {
                fileWriter.write(jsonObject.toString(4)); // Pretty print with 4-space indentation
                System.out.println("Dimensions saved to JSON: " + jsonPath);
            } catch (IOException e) {
                System.err.println("Error saving dimensions to JSON: " + e.getMessage());
            }
        }
    }
}