package com.github.exiostorm.graphics;

//import sun.util.locale.BaseLocale;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

//TODO fuck. we also need new logic for a new atlas because of sizing restrictions. our atlas will need re-sized and re-uploaded when new glyphs are added.
//TODO work on SDF generator logic
public class GlyphManager {
    private String lombokString = "Placeholder to suppress lombok message : ";
    private String packge = "com.github.exiostorm.graphics.GlyphManager.";



    private String parentDirectory = "";
    private byte defaultLocale = 15;/*BaseLocale.US*/
    private int atlasSlot = GL_TEXTURE_2D;
    private List<String> fonts = new ArrayList<>(Arrays.asList("Calligraserif"));

    private TextureAtlas glyphAtlas;

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
    public BufferedImage createGlyph(int unicode, String font) {
        // Check if glyph file exists on disk
        /** need to change where it uses defaultLocale so we use the proper language for the unicode
         Locale.getAvailableLocales()[defaultLocale].getLanguage() **/
        File glyphFile = new File("glyphs/fonts/" + font + "/" + getLanguageOfChar(unicode) + "/" + unicode + ".png");
        if (glyphFile.exists()) {
            Texture glyphTexture = new Texture(glyphFile.getPath());
            //TODO need our method to add the glyph to our atlas.
            // return addGlyphToAtlas(unicode, glyphTexture);
        }
        //TODO [0]
        return new BufferedImage(0,0,0);
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
