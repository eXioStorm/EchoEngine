package com.github.exiostorm.graphics.archive;

import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.stb.STBTTAlignedQuad;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class TextRenderer {
    private static final Map<String, FontData> fonts = new LinkedHashMap<>(); // Store fonts by name in insertion order
    private static String defaultFont; // Variable to hold the default font name
    private static int defaultColorHex=0xFFFFFF;

    // Inner class to hold font data (texture ID and baked character data)
    private static class FontData {
        STBTTBakedChar.Buffer charData;
        int textureId;
        FontData(STBTTBakedChar.Buffer charData, int textureId) {
            this.charData = charData;
            this.textureId = textureId;
        }
    }

    public static void renderText(String text, String fontName, int colorHex, float colorAlpha, float x, float y, float scale) {
        renderTextCore(text, fontName, colorHex, colorAlpha, x, y, scale);
    }
    public static void renderText(String text, String fontName, float colorAlpha, float x, float y, float scale) {
        renderTextCore(text, fontName, defaultColorHex, colorAlpha, x, y, scale);
    }
    public static void renderText(String text, String fontName, float x, float y, float scale) {
        renderTextCore(text, fontName, defaultColorHex, 1.0f, x, y, scale);
    }
    public static void renderText(String text, int colorHex, float colorAlpha, float x, float y, float scale) {
        if(defaultFont==null) {
            if (!fonts.isEmpty()) {
                defaultFont = fonts.keySet().iterator().next(); // Get the first key from the HashMap
            }
        }
        renderTextCore(text, defaultFont, colorHex, colorAlpha, x, y, scale);
    }
    public static void renderText(String text, float x, float y, float scale) {
        if(defaultFont==null) {
            if (!fonts.isEmpty()) {
                defaultFont = fonts.keySet().iterator().next(); // Get the first key from the HashMap
            }
        }
        renderTextCore(text, defaultFont, defaultColorHex, 1.0f, x, y, scale);
    }
    public static void renderText(String text, float colorAlpha, float x, float y, float scale) {
        if(defaultFont==null) {
            if (!fonts.isEmpty()) {
                defaultFont = fonts.keySet().iterator().next(); // Get the first key from the HashMap
            }
        }
        renderTextCore(text, defaultFont, defaultColorHex, colorAlpha, x, y, scale);
    }

    // Load all fonts from the specified directory
    public static void loadFontsFromDirectory(String fontDir) {
        try {
            Files.list(Path.of(fontDir))
                    .filter(path -> path.toString().toLowerCase().endsWith(".ttf"))
                    .forEach(path -> {
                        String fontName = path.getFileName().toString().replace(".ttf", "");
                        System.out.println("Loading font: " + fontName + " from " + path);
                        try {
                            ByteBuffer fontBuffer = loadFontFile(path.toString());
                            FontData fontData = initFont(fontBuffer);
                            fonts.put(fontName, fontData); // Load and store font
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load the .ttf file into a ByteBuffer
    private static ByteBuffer loadFontFile(String fontFilePath) throws Exception {
        Path path = Path.of(fontFilePath);
        byte[] fontBytes = Files.readAllBytes(path);
        ByteBuffer fontBuffer = MemoryUtil.memAlloc(fontBytes.length);
        fontBuffer.put(fontBytes).flip();
        return fontBuffer;
    }

    //TODO sweet. so basically this works by creating an atlas that's 512x512? I think we should be able to re-work the code a bit...
    // though there may be quite a few parts that need large over-hauls to get working correctly.
    // (good news because a texture atlas is also exactly what we plan on using, meaning we'll have a bit of the logic ready already.)
    // Initialize font and create texture, returning the FontData
    private static FontData initFont(ByteBuffer fontBuffer) {
        STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(96); // ASCII printable characters
        // Create a texture to store the font bitmap
        int textureSize = 512;
        ByteBuffer bitmap = MemoryUtil.memAlloc(textureSize * textureSize);
        // Bake the font bitmap (render characters to a bitmap)
        STBTruetype.stbtt_BakeFontBitmap(fontBuffer, 32, bitmap, textureSize, textureSize, 32, charData);
        // Generate OpenGL texture for the baked bitmap
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, textureSize, textureSize, 0, GL_ALPHA, GL_UNSIGNED_BYTE, bitmap);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Free memory
        MemoryUtil.memFree(bitmap);
        MemoryUtil.memFree(fontBuffer);
        return new FontData(charData, textureId); // Return the FontData containing charData and texture ID
    }


    private static void renderTextCore(String text, String fontName, int colorHex, float alpha, float x, float y, float scale) {
        FontData fontData = fonts.get(fontName);
        if (fontData == null) {
            System.err.println("Font not found: " + fontName);
            return; // Exit if the font is not found
        }
        // Extract RGB values from the hex color code
        float red = ((colorHex >> 16) & 0xFF) / 255.0f;
        float green = ((colorHex >> 8) & 0xFF) / 255.0f;
        float blue = (colorHex & 0xFF) / 255.0f;

        glBindTexture(GL_TEXTURE_2D, fontData.textureId);
        glEnable(GL_TEXTURE_2D);
        // Set the color for the text (RGBA)
        glColor4f(red, green, blue, alpha);
        glBegin(GL_QUADS);

        // Create an instance of STBTTAlignedQuad to hold quad information
        STBTTAlignedQuad quad = STBTTAlignedQuad.malloc();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Start xposBuffer with the x coordinate, not 0
            FloatBuffer xposBuffer = stack.floats(x);
            FloatBuffer yposBuffer = stack.floats(y);

            for (int i = 0; i < text.length(); i++) {
                //TODO something here, unicode limitation? char limitation, might want int?
                char c = text.charAt(i);
                if (c >= 32 && c < 128) {
                    // Get the baked quad for the character 'c'
                    STBTruetype.stbtt_GetBakedQuad(fontData.charData, 512, 512, c - 32, xposBuffer, yposBuffer, quad, true);

                    // Render the quad with scaled positions
                    float x0 = quad.x0() * scale;
                    float y0 = quad.y0() * scale;
                    float x1 = quad.x1() * scale;
                    float y1 = quad.y1() * scale;

                    glTexCoord2f(quad.s0(), quad.t0());
                    glVertex2f(x0, y0);

                    glTexCoord2f(quad.s1(), quad.t0());
                    glVertex2f(x1, y0);

                    glTexCoord2f(quad.s1(), quad.t1());
                    glVertex2f(x1, y1);

                    glTexCoord2f(quad.s0(), quad.t1());
                    glVertex2f(x0, y1);
                }
            }
        }

        glEnd();
        glDisable(GL_TEXTURE_2D);

        // Free the quad after rendering
        quad.free();
    }

    // Method to set the default font
    public static void setDefaultFont(String fontName) {
        if (fonts.containsKey(fontName)) {
            defaultFont = fontName;
        } else {
            System.out.println("Font not found: " + fontName);
        }
    }
    // Method to set the default color hexidecimal code
    public static void setDefaultColorHex(int colorHex){
        defaultColorHex = colorHex;
    }
    // Retrieve a font's baked character data by its name
    public static STBTTBakedChar.Buffer getFont(String fontName) {
        FontData fontData = fonts.get(fontName);
        return fontData != null ? fontData.charData : null;
    }
}
