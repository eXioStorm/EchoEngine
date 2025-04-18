package com.github.exiostorm.graphics;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.awt.Rectangle;
import java.util.*;

import static org.lwjgl.opengl.GL30.*;
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
    private Map<String, String> primaryAtlas; // Category  -> Active SubAtlas
    //TODO Looking at our bin packer it seems we need to convert our setup so that primaryAtlas does not use Texture but contains the subAtlas only? and all of our position data is purely saved to the subAtlases while primaryAtlas purely
    // describes the used subAtlas.? will need to make changes to ;
    // AtlasManager : addToAtlas(), saveAtlasToGPU(), calculateAtlasPrimaryPlacement(),
    private int width;
    private int height;
    private MultiValuedMap<String, MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlases; // Category -> (SubAtlas Name -> (Texture -> Placement)) // multiple subAtlas, and then multiple textures. //"()" indicates multiple entries
    // Because of this change we'll need to change our bin packer.
    private Map<Texture, Rectangle> texturePositions;
    //TODO is this redundant??
    private Map<String, Rectangle> subAtlasSizes; // SubAtlas Name -> [Used x, Used y, Used Width, Used Height]
    private Map<String, String> swapQueue;
    //TODO Need to separate management logic so we can manage multiple TextureAtlases. Not certain WHY we'd need multiple, but I suspect it would either have something to do with Fonts, or community content.
    private int atlasID;
    /**
     * Use this to set which GPU texture to bind the atlas to. this way we can save multiple textures to the GPU.
     */
    private int atlasSlot = GL_TEXTURE_2D;
    private boolean inMemory = false;

    /**
     * initialized all of our mappings.
     */
    public TextureAtlas() {
        this.primaryAtlas = new HashMap<>();
        this.subAtlases = new ArrayListValuedHashMap<>();

        this.texturePositions = new HashMap<>();
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

    public MultiValuedMap<String, MultiValuedMap<String, Map<Texture, Rectangle>>> getSubAtlases(){
        return this.subAtlases;
    }
    public void setSubAtlases(MultiValuedMap<String, MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlases){
        this.subAtlases = subAtlases;
    }

    public Map<String, String> getPrimaryAtlas(){
        return this.primaryAtlas;
    }
    public void setPrimaryAtlas(Map<String, String> primaryAtlas){
        this.primaryAtlas = primaryAtlas;
    }

    public Map<String, Rectangle> getSubAtlasSizes() {
        return this.subAtlasSizes;
    }
    public void setSubAtlasSizes(Map<String, Rectangle> subAtlasSizes) {
        this.subAtlasSizes = subAtlasSizes;
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
    public void setAtlasSlot(int atlasSlot){
        this.atlasSlot = atlasSlot;
    }

    public int getAtlasID() {
        return this.atlasID;
    }
    public Map<Texture, Rectangle> getTexturePositions() {
        return texturePositions;
    }

    public void setTexturePositions(Map<Texture, Rectangle> texturePositions) {
        this.texturePositions = texturePositions;
    }
}