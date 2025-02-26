package com.github.exiostorm.renderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.stb.STBImage.stbi_image_free;

//TODO will rename this later when I come up with a better naming system.
public class TextureGenerator {
    static Map<String, Texture> Textures;

    public TextureGenerator() {
        Textures = new HashMap<>();
    }
    public static void addTexture(Texture texture) {
        Textures.putIfAbsent(texture.getPath(), texture);
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
}
