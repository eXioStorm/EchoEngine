package com.github.exiostorm.graphics;

//import sun.util.locale.BaseLocale;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.exiostorm.utils.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.util.msdfgen.*;
import org.lwjgl.system.MemoryStack;

import javax.naming.Context;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

//TODO fuck. we also need new logic for a new atlas because of sizing restrictions. our atlas will need re-sized and re-uploaded when new glyphs are added.
// Should have something to toggle the ability to generate new glyphs so that people can prevent performance hits. then new glyphs will just render with a default character.
//TODO work on SDF generator logic
public class GlyphManager {
    private String lombokString = "Placeholder to suppress lombok message : ";
    private String packge = "com.github.exiostorm.graphics.GlyphManager.";
    private String glyphs = "glyphs";
    private String GLYPH_DIR = "glyphs/fonts/";

    private static final int DEFAULT_RESOLUTION = 64;
    private static final double DEFAULT_RANGE = 4.0;
    private static final double DEFAULT_ANGLE_THRESHOLD = Math.PI / 3.0; // 60 degrees

    private String parentDirectory = "";
    private byte defaultLocale = 15;/*BaseLocale.US*/
    private int atlasSlot = GL_TEXTURE_2D;
    //TODO [0] need to set something up to detect OS and load a font file from the OS directory.
    // Actually, probably create our own ttf and load it with our application?
    private List<String> fonts = new ArrayList<>(Arrays.asList("Calligraserif"));

    private TextureAtlas glyphAtlas = AtlasManager.newAtlas(GLYPH_DIR);

    //TODO [1] need a List / enum List of unicode characters to be loaded categorized by language

    /* removed since getters/setters make it redundant.
    public GlyphManager(String directory, int textureSlot, byte locale) {
        this.parentDirectory = directory;
        this.defaultLocale = locale;
        this.atlasSlot = textureSlot;
    }*/

    /**
     * Was thinking of putting the logic for this inside of "public GlyphManager", however it's probably useful to allow us to create our managers separately
     * from initializing potentially expensive calculations, so we can load at specific times.
     */
    public void initializeGlyphs() {
        //TODO will have logic here to check for a json configuration in our parentDirectory that contains our defaultLocale(PLUS manual glyph additions?),
        // and then from that setting will check for a folder structure under the name of our defaultLocale to find our glyph configuration json.
        // from reading our glyph configuration we will use the method createGlyph to go through and fetch the data for our glyphs to populate the atlas.
        //TODO When we create our atlases I wonder if we should have some way of directly loading an already generated atlas and deleting our individual files...
        // we could then have logic to split the atlas back into separate files?... Reason to do so would be because of application start-up time.
        // the problem when we do that with our atlas is that we lose the ability to have subatlases for texture swapping. in this particular instance we don't need it.
    }

    /**
     * During checking for our glyphs we use this method if our glyph is not found already on disk.
     * @param unicode the unicode to be saved... Might need to change this to a BufferedImage? won't know until I have more logic created.
     */
    public void getGlyph(int unicode, String font) {
        // Check if glyph file exists on disk
        /** need to change where it uses defaultLocale so we use the proper language for the unicode
         Locale.getAvailableLocales()[defaultLocale].getLanguage() **/
        File glyphFile = new File(GLYPH_DIR + font + "/" + getLanguageOfChar(unicode) + "/" + unicode + ".png");
        if (!glyphFile.exists()) {
            //TODO [0] need logic to generate the glyph
            //STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc(96??); // ASCII printable characters??
            //STBTruetype.stbtt_BakeFontBitmap(fontBuffer, 32, bitmap, textureSize, textureSize, 32, charData);
            TextureManager.addTexture(createGlyph(unicode, font, glyphFile));

        }
        Texture glyphTexture = TextureManager.addTexture(glyphFile.getPath());
        // The category and subAtlas are the same so that our bin packer packs them all equally so our atlas doesn't become misshapen.
        AtlasManager.addToAtlas(glyphAtlas, glyphs, glyphs, glyphTexture);
    }
    //TODO 2025-03-28
    //TODO this is the last thing we were working on, and we need the logic for generating our glyphs. for this we were porting MSDFGen from C++ to Java so we can generate our glyphs DURING runtime.
    // When porting MSDFGen we created our own class MSDFGenExt to replicate LWJGL's own MSDFGenExt class that's lacking the features we need to actually generate glyphs with.
    // "Our" MSDFGenExt has math logic for the generation of glyphs, things like contour, color, direction, point, etc. I have no understanding of how it works.
    //TODO Previously we were using ChatGpt trying to generate the glyphs and it was trying to use C++ references that we didn't have any identical references for in Java, currently we have no reference for actually generating anything and
    // we left off porting the C++ code to Java. We need to confirm we've ported the code we need and then generate our code / references with our new ported code.
    public String createGlyph(int unicode, String font, File file) {
        //TODO [0] need to generate the glyph, save it to our directory, and then create a new texture.[New Texture is created on line 84 with TextureManager.addTexture(createGlyph())]
        //TODO [0] might need to set it up to batch process our glyphs as we'll be using multiple glyphs from any one ttf font file.
        // Load the TTF font into a ByteBuffer

        return "null";
    }
    /**
     * Loads a TTF font file into a direct ByteBuffer.
     *
     * @param fontFile The TTF font file.
     * @return A ByteBuffer containing the font data.
     */
    private static ByteBuffer loadFontFile(File fontFile) {
        try {
            byte[] data = new byte[(int) fontFile.length()];
            try (FileInputStream fis = new FileInputStream(fontFile)) {
                fis.read(data);
            }
            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length).order(ByteOrder.nativeOrder());
            buffer.put(data);
            buffer.flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font file: " + fontFile.getAbsolutePath(), e);
        }
    }


    private String getLanguageOfChar(int unicode) {
        Character.UnicodeScript script = Character.UnicodeScript.of(unicode);
        return switch (script) {
            case LATIN -> "Latin";
            case CYRILLIC -> "Cyrillic";
            case GREEK -> "Greek";
            case ARABIC -> "Arabic";
            case HEBREW -> "Hebrew";
            case DEVANAGARI -> "Devanagari";
            case HAN -> "Chinese";  // Covers Chinese, Japanese Kanji
            case HANGUL -> "Korean";
            case HIRAGANA, KATAKANA -> "Japanese";
            case THAI -> "Thai";
            case ARMENIAN -> "Armenian";
            case GEORGIAN -> "Georgian";
            default -> "Unknown";  // If script isn't recognized
        };
    }
    //TODO Getters / Setters V
    public int getAtlasSlot() {
        if (atlasSlot < 0) { System.out.println(lombokString + packge + "getAtlasSlot()"); }
        return atlasSlot;
    }
    public void setAtlasSlot(int textureSlot) {
        if (atlasSlot < 0) { System.out.println(lombokString + packge + "setAtlasSlot()"); }
        atlasSlot = textureSlot;
    }
    public byte getDefaultLocale() {
        if (glyphAtlas != null) { System.out.println(lombokString + packge + "getDefaultLocale()"); }
        return defaultLocale;
    }
    public void setDefaultLocale(byte locale) {
        if (glyphAtlas != null) { System.out.println(lombokString + packge + "setDefaultLocale()"); }
        defaultLocale = locale;
    }
    public TextureAtlas getGlyphAtlas() {
        if (glyphAtlas != null) { System.out.println(lombokString + packge + "getGlyphAtlas()"); }
        return glyphAtlas;
    }
    public void setGlyphAtlas(TextureAtlas atlas) {
        if (glyphAtlas != null) { System.out.println(lombokString + packge + "setGlyphAtlas()"); }
        glyphAtlas = atlas;
    }
    public String getParentDirectory() {
        if (parentDirectory != null) { System.out.println(lombokString + packge + "getParentDirectory()"); }
        return parentDirectory;
    }
    public void setParentDirectory(String path) {
        if (parentDirectory != null) { System.out.println(lombokString + packge + "setParentDirectory()"); }
        parentDirectory = path;
    }
    /****/
    public List<String> getFonts() {
        if (parentDirectory != null) { System.out.println(lombokString + packge + "getFonts()"); }
        return fonts;
    }
    public void setFonts(List<String> fonts) {
        if (parentDirectory != null) { System.out.println(lombokString + packge + "setFonts()"); }
        this.fonts = fonts;
    }
    public void addFont(String fonts) { this.fonts.add(fonts); }
    public void removeFont(String fonts) { this.fonts.add(fonts); }
    /**/
}
