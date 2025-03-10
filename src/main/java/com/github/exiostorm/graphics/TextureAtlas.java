package com.github.exiostorm.graphics;

import org.apache.commons.collections4.map.MultiValueMap;

import java.awt.Rectangle;
import java.util.*;

import static org.lwjgl.opengl.GL30.*;
@SuppressWarnings("deprecation")//MultiValueMap was replaced by MultiValuedMap, however I couldn't quickly figure out how to iterate through MultiValuedMap...
//TODO maybe add a method to merge subAtlases? looking into glyph rendering, and I could see it being useful to pack multiple languages together so we don't have super wide or tall atlases because of different subAtlases for each language.
// Think I won't do this, instead we'll just add onto our atlas when we need new characters.
//TODO ANOTHER idea for glyph/text rendering is to have a small set of default characters we load, and then dynamically generate new text characters as they get used.(this would work by rendering a default character when unrecognized,
// and queuing the unrecognized character to be generated to the glyph atlas.)
//TODO Another issue with our current setup is the usage of Texture objects, because when we use text we don't reference a file on the computer we generate the data for the text.
//TODO I'm wondering if I should add a method to remove textures / fonts based on their use? then we add them dynamically, and remove when they haven't been used for awhile.?
// if we're adding new characters dynamically as they get used then this probably isn't necessary.
public class TextureAtlas {
    /**
     * Going to use this for having multiple texture atlases.
     * see BatchRenderer.draw();
     */
    private List<Quad> quads = new ArrayList<>();
    //TODO maybe switch these back to using String instead of Texture... idk, might want to fix modularity?
    // Scratch this idea, this is our TEXTURE atlas, we're getting too obsessive about modularity. we can have this dependency here because of just how intertwined the classes actually are.
    //TODO Reminder here that we use a normal Map and not a MultiValueMap because in this case we actually only want one subAtlas per category.?
    private Map<String, Map<Texture, Rectangle>> primaryAtlas; // Category -> Active SubAtlas (Texture Name -> Placement)
    private int width;
    private int height;
    private MultiValueMap<String, Map<String, Map<Texture, Rectangle>>> subAtlases; // Category -> (SubAtlas Name -> (Texture Name -> Placement))
    private Map<String, Rectangle> subAtlasSizes; // SubAtlas Name -> [Used x, Used y, Used Width, Used Height]
    private Map<String, String> swapQueue;
    //TODO this may be redundant because of our Rectangles in subAtlases
    //private Map<Texture, float[]> textureUV; // Texture Name -> Placement
    //TODO Need to separate management logic so we can manage multiple TextureAtlases. Not certain WHY we'd need multiple, but I suspect it would either have something to do with Fonts, or community content.
    private int atlasID;
    /**
     * Use this to set which GPU texture to bind the atlas to. this way we can save multiple textures to the GPU.
     */
    private int atlasSlot = GL_TEXTURE_2D;
    /**
     * use isPacked for reduced disk space / faster load times.
     * This will be necessary logic for unpacking textures before we do things like swap atlases.(We wouldn't want to swap a subatlas if our original no longer exists on the disk.)
     * For normal usage, myself I'd probably just use unpacked textures and deal with the longer loading times. Except the singular instance it wouldn't matter which is glyphs / fonts...
     */
    private boolean isPacked = false;
    private boolean inMemory = false;

    /**
     * initialized all of our mappings.
     */
    public TextureAtlas() {
        this.subAtlases = new MultiValueMap<>();
        this.primaryAtlas = new HashMap<>();
        this.subAtlasSizes = new HashMap<>();
        //this.textureUV = new HashMap<>();
        swapQueue = new HashMap<>();
        atlasID = glGenTextures();
    }
    public void bind() {
        glBindTexture(this.atlasSlot, this.atlasID);
    }
    public void unbind() {
        glBindTexture(this.atlasSlot, 0);
    }

    //TODO might delete this
    public void addToAtlas(TextureAtlas atlas, String category, String subAtlas, Texture texture) {
        AtlasManager.addToAtlas(atlas, category, subAtlas, texture);
    }
    public Map<String, Map<Texture, Rectangle>> getPrimaryAtlas(){
        return this.primaryAtlas;
    }
    public MultiValueMap<String, Map<String, Map<Texture, Rectangle>>> getSubAtlases(){
        return this.subAtlases;
    }
    Map<String, Rectangle> getSubAtlasSizes() {
        return this.subAtlasSizes;
    }
    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Map<String, String> getSwapQueue() {
        return this.swapQueue;
    }

    public boolean getInMemory() {
        return this.inMemory;
    }
    void setInMemory(boolean saved) {
        this.inMemory = saved;
    }

    public int getAtlasSlot() {
        return this.atlasSlot;
    }

    public int getAtlasID() {
        return this.atlasID;
    }
    /* to delete later~
    public Map<Texture, float[]> getTextureUV() {
        return this.textureUV;
    }
     */
}