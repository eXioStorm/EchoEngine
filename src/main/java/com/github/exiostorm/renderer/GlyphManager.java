package com.github.exiostorm.renderer;


/**
 * Placeholder for new glyph management system using our TextureAtlas class
 */
public class GlyphManager {
    private String packge = "com.github.exiostorm.renderer.GlyphManager.";
    private String parentDirectory;
    private TextureAtlas glyphAtlas;
    private byte defaultLocale;
    private int atlasSlot;

    /**
     * Glyph Manager, responsible for creating, loading, and using the glyph atlas.
     * @param directory This is the file path for the parent directory of our glyphs.
     * @param locale This will set our default locale, which will be used to pick our default atlas to be used.
     */
    public GlyphManager(String directory, int textureSlot, byte locale) {
        this.parentDirectory = directory;
        this.defaultLocale = locale;
        this.atlasSlot = textureSlot;
    }

    /**
     * Was thinking of putting the logic for this inside of GlyphManager, however it's probably useful to allow us to create our managers separately from initializing potentially expensive calculations so we can load at specific times.
     */
    public void initializeGlyphs() {

    }

    /**
     * During checking for our glyphs we use this method if our glyph is not found already on disk.
     * @param unicode the unicode to be saved... Might need to change this to a BufferedImage? won't know until I have more logic created.
     */
    public void saveGlyphToDisk(int unicode) {

    }



    //TODO Getters / Setters V
    public int getAtlasSlot() {
        if (atlasSlot < 0) { System.out.println("Placeholder to suppress lombok message : " + packge + "getAtlasSlot()"); }
        return atlasSlot;
    }
    public void setAtlasSlot(int textureSlot) {
        if (atlasSlot < 0) { System.out.println("Placeholder to suppress lombok message : " + packge + "setAtlasSlot()"); }
        atlasSlot = textureSlot;
    }
    public byte getDefaultLocale() {
        if (glyphAtlas != null) { System.out.println("Placeholder to suppress lombok message : " + packge + "getDefaultLocale()"); }
        return defaultLocale;
    }
    public void setDefaultLocale(byte locale) {
        if (glyphAtlas != null) { System.out.println("Placeholder to suppress lombok message : " + packge + "setDefaultLocale()"); }
        defaultLocale = locale;
    }
    public TextureAtlas getGlyphAtlas() {
        if (glyphAtlas != null) { System.out.println("Placeholder to suppress lombok message : " + packge + "getGlyphAtlas()"); }
        return glyphAtlas;
    }
    public void setGlyphAtlas(TextureAtlas atlas) {
        if (glyphAtlas != null) { System.out.println("Placeholder to suppress lombok message : " + packge + "setGlyphAtlas()"); }
        glyphAtlas = atlas;
    }
    public String getParentDirectory() {
        if (parentDirectory != null) { System.out.println("Placeholder to suppress lombok message : " + packge + "getParentDirectory()"); }
        return parentDirectory;
    }
    public void setParentDirectory(String path) {
        if (parentDirectory != null) { System.out.println("Placeholder to suppress lombok message : " + packge + "setParentDirectory()"); }
        parentDirectory = path;
    }

}
