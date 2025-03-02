package com.github.exiostorm.renderer;

import lombok.Getter;
import org.apache.commons.collections4.map.MultiValueMap;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
     */
    private List<Quad> quads = new ArrayList<>();
    /**
     * List of free rectangles used for tracking available space in the atlas.
     * This list is updated and used within the rectanglePacker() method
     * to determine where textures can be placed efficiently.
     */
    private List<Rectangle> freeRectangles;
    //TODO This will be moved with our bin packer / REMOVED since we won't need it.
    /**
     * value is set inside our calculatePlacement() method, and used by our other methods calculatePrimaryPlacement() and calculateSubPlacement().
     * When we move our rectanglePacker to another class we should give it a return value so we can set this. if a return value already exists, then perhaps feed it as a parameter.
     */
    @Getter
    private Rectangle calculatedSize;
    //TODO maybe switch these back to using String instead of Texture... idk, might want to fix modularity?
    // Scratch this idea, this is our TEXTURE atlas, we're getting too obsessive about modularity. we can have this dependency here because of just how intertwined the classes actually are.
    //TODO Reminder here that we use a normal Map and not a MultiValueMap because in this case we actually only want one subAtlas per category.
    private Map<String, Map<Texture, Rectangle>> primaryAtlas; // Category -> Active SubAtlas (Texture Name -> Placement)
    //TODO early mistake made, had to change this from Map to MultiValueMap after configuring everything else and that broke a lot of things. we need a MultiValueMap for handling categories with multiple entries.
    private MultiValueMap<String, Map<String, Map<Texture, Rectangle>>> subAtlases; // Category -> (SubAtlas Name -> (Texture Name -> Placement))
    //Separate from subAtlases map to avoid anymore complexity than we already have.
    private Map<String, Rectangle> subAtlasSizes; // SubAtlas Name -> [Used x, Used y, Used Width, Used Height]
    //Separate from primaryAtlas map to avoid anymore complexity than we already have.
    private int[] primaryAtlasSize;
    // Queue for batching subAtlas swaps
    private Map<String, String> swapQueue;
    //Separate from subAtlases map to make coordinate retrieval quick.
    @Getter
    private Map<Texture, float[]> textureUV; // Texture Name -> Placement
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
        this.freeRectangles = new ArrayList<>();
        this.subAtlases = new MultiValueMap<>();
        this.primaryAtlas = new HashMap<>();
        this.subAtlasSizes = new HashMap<>();
        this.primaryAtlasSize = new int[2];
        this.textureUV = new HashMap<>();
        this.calculatedSize = new Rectangle(0,0,0,0);
        swapQueue = new HashMap<>();
        atlasID = glGenTextures();
    }

    /**
     * Might need to remove this? unless we store which GL_TEXTURE0... to use.
     */
    public void bind() {
        glBindTexture(atlasSlot, atlasID);
    }

    /**
     * Might need to remove this? unless we store which GL_TEXTURE0... to use.
     */
    public void unbind() {
        glBindTexture(atlasSlot, 0);
    }
    //TODO could move this method to the manager? parameter to accept TextureAtlas for the ID n stuff.
    //TODO Using GL13 we could provide things like GL_TEXTURE0, GL_TEXTURE1, etc. to assign to different slots. could even use a map to be certain what goes where.
    public void initializeGPUAtlas() {
        ByteBuffer atlasBuffer = ByteBuffer.allocateDirect(primaryAtlasSize[0] * primaryAtlasSize[1] * 4).order(ByteOrder.nativeOrder());
        // Fill the buffer with transparent pixels
        for (int i = 0; i < primaryAtlasSize[0] * primaryAtlasSize[1] * 4; i++) {
            atlasBuffer.put((byte) 0);
        }

        // Process each texture in the atlas
        for (Map<Texture, Rectangle> subAtlas : primaryAtlas.values()) {
            for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
                Rectangle rect = entry.getValue();

                ByteBuffer textureBuffer = entry.getKey().getByteBuffer((byte) 0);
                if (textureBuffer != null) {
                    for (int row = 0; row < rect.height; row++) {
                        for (int col = 0; col < rect.width; col++) {
                            int atlasIndex = ((rect.y + row) * primaryAtlasSize[0] + (rect.x + col)) * 4;
                            int textureIndex = (row * rect.width + col) * 4;

                            atlasBuffer.put(atlasIndex, textureBuffer.get(textureIndex));     // Red
                            atlasBuffer.put(atlasIndex + 1, textureBuffer.get(textureIndex + 1)); // Green
                            atlasBuffer.put(atlasIndex + 2, textureBuffer.get(textureIndex + 2)); // Blue
                            atlasBuffer.put(atlasIndex + 3, textureBuffer.get(textureIndex + 3)); // Alpha
                        }
                    }
                }
            }
        }
        atlasBuffer.flip();
        glBindTexture(atlasSlot, atlasID);
        glTexImage2D(atlasSlot, 0, GL_RGBA, primaryAtlasSize[0], primaryAtlasSize[1], 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glTexParameteri(atlasSlot, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(atlasSlot, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(atlasSlot, 0);
        inMemory = true;
        System.out.println("Atlas finalized and uploaded. Dimensions: " + primaryAtlasSize[0] + "x" + primaryAtlasSize[1]);
    }
    /**
     * Hmmm... some issues with this method perhaps... maybe with our overall setup? Maybe not. Thinking about our JukeBox and category modifications.
     * This is our method for adding textures, all textures belong to a subAtlas.
     * @param category Category this texture belongs to.
     * @param subAtlas SubAtlas this texture belongs to.
     * @param texture The texture itself.
     */
    public void addTexture(String category, String subAtlas, Texture texture) {
        primaryAtlas.putIfAbsent(category, new HashMap<>());

        Map<String, Map<Texture, Rectangle>> categorySubAtlases = subAtlases.getCollection(category).iterator().next();
        if (categorySubAtlases == null) {
            categorySubAtlases = new HashMap<>();
            subAtlases.put(category, categorySubAtlases);
        }

        Map<Texture, Rectangle> subAtlasMap = categorySubAtlases.computeIfAbsent(subAtlas, k -> new HashMap<>());

        subAtlasMap.put(texture, new Rectangle(0, 0, texture.getWidth(), texture.getHeight()));
    }

    /**
     * This is our bin packer, use other methods for our texture Atlas...
     * @param rectangleMap Map<String, Rectangle> for packing Rectangles to an area.
     */
    //TODO I should probably move this to a different class? is now more useful than just for the texture atlas.
    public <K> void rectanglePacker(Map<K, Rectangle> rectangleMap) {
        List<Map.Entry<K, Rectangle>> sortedTextures = new ArrayList<>(rectangleMap.entrySet());
        sortedTextures.sort(Comparator.comparingInt(e -> -e.getValue().width * e.getValue().height));

        freeRectangles.clear();
        freeRectangles.add(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)); // Initial large free area

        calculatedSize.setBounds(0,0,0,0);

        for (Map.Entry<K, Rectangle> entry : sortedTextures) {
            Rectangle rectangle = entry.getValue();

            int bestAreaFit = Integer.MAX_VALUE;
            Rectangle bestRect = null;
            int bestIndex = -1;

            for (int i = 0; i < freeRectangles.size(); i++) {
                Rectangle freeRect = freeRectangles.get(i);
                if (freeRect.width >= rectangle.width && freeRect.height >= rectangle.height) {
                    int areaFit = freeRect.width * freeRect.height - rectangle.width * rectangle.height;
                    if (areaFit < bestAreaFit) {
                        bestRect = new Rectangle(freeRect.x, freeRect.y, rectangle.width, rectangle.height);
                        bestAreaFit = areaFit;
                        bestIndex = i;
                    }
                }
            }

            if (bestRect != null) {
                Rectangle freeRect = freeRectangles.remove(bestIndex);

                if (freeRect.width > bestRect.width) {
                    freeRectangles.add(new Rectangle(bestRect.x + bestRect.width, bestRect.y,
                            freeRect.width - bestRect.width, freeRect.height));
                }
                if (freeRect.height > bestRect.height) {
                    freeRectangles.add(new Rectangle(bestRect.x, bestRect.y + bestRect.height,
                            bestRect.width, freeRect.height - bestRect.height));
                }
                rectangle.setLocation(bestRect.x, bestRect.y);
                rectangleMap.put(entry.getKey(), bestRect);
                calculatedSize.x = bestRect.x;
                calculatedSize.y = bestRect.y;
                calculatedSize.width = Math.max(calculatedSize.width, bestRect.x + bestRect.width);
                calculatedSize.height = Math.max(calculatedSize.height, bestRect.y + bestRect.height);
            }
        }
    }

    /*
    //TODO a lil' confused, we should only have one subAtlas in each category for our primaryAtlas, but I suppose this makes sense?...
            for (String subAtlas : primaryAtlas.get(category).keySet()) {
                categorySizes.put(category, subAtlasSizes.get(subAtlas));
            }
     */
    private void calculatePrimaryPlacement() {
        Map<String, Rectangle> categoryPositions = new HashMap<>();

        for (String category : primaryAtlas.keySet()) {
            //TODO might have issue with multivaluemap, after "subAtlases.getCollection(category)" add ".iterator().next()"
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlases.getCollection(category)) {
                String subAtlasName = subAtlasEntry.keySet().iterator().next();
                categoryPositions.put(category, subAtlasSizes.get(subAtlasName));
            }
        }

        rectanglePacker(categoryPositions);

        for (String category : primaryAtlas.keySet()) {
            //TODO might have issue with multivaluemap, after "subAtlases.getCollection(category)" add ".iterator().next()"
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlases.getCollection(category)) {
                for (Map<Texture, Rectangle> subAtlas : subAtlasEntry.values()) {
                    for (Texture textureName : subAtlas.keySet()) {
                        subAtlas.get(textureName).setLocation(
                                subAtlas.get(textureName).x + categoryPositions.get(category).x,
                                subAtlas.get(textureName).y + categoryPositions.get(category).y
                        );
                        textureUV.put(textureName, toUV(subAtlas.get(textureName)));
                    }
                }
            }
        }
        primaryAtlasSize[0] = calculatedSize.width;
        primaryAtlasSize[1] = calculatedSize.height;
    }

    //TODO should probably make changes to this method for individual changes? idk...
    // Would prefer to do this step during initialization I think?
    public void calculateAllSubPlacements() {
        for (String category : subAtlases.keySet()) {
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlases.getCollection(category)) {
                for (String subAtlasName : subAtlasEntry.keySet()) {
                    Map<Texture, Rectangle> subAtlas = subAtlasEntry.get(subAtlasName);

                    rectanglePacker(subAtlas);
                    subAtlasSizes.put(subAtlasName, new Rectangle(calculatedSize));

                    // Update UV mapping for all textures in the subAtlas
                    for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
                        textureUV.put(entry.getKey(), toUV(entry.getValue()));
                    }
                }
            }
        }
    }

    public void calculateSubPlacement(String category, String subAtlasName) {
        if (!subAtlases.containsKey(category)) {
            System.err.println("Category '" + category + "' not found.");
            return;
        }

        Map<Texture, Rectangle> subAtlas = null;

        for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlases.getCollection(category)) {
            if (subAtlasEntry.containsKey(subAtlasName)) {
                subAtlas = subAtlasEntry.get(subAtlasName);
                break;
            }
        }

        if (subAtlas == null) {
            System.err.println("SubAtlas '" + subAtlasName + "' not found in category '" + category + "'.");
            return;
        }

        // Calculate placements for this specific subAtlas
        rectanglePacker(subAtlas);
        subAtlasSizes.put(subAtlasName, new Rectangle(calculatedSize));

        // Update UV mapping for all textures in the subAtlas
        for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
            textureUV.put(entry.getKey(), toUV(entry.getValue()));
        }
    }

    public void finalizeAtlasMaps() {
        calculateAllSubPlacements();
        calculatePrimaryPlacement();
    }
    /**
     * This method allows us to swap subAtlases, allowing us to only keep textures bound when necessary.
     * @param category name of the texture category
     * @param newSubAtlas the new subAtlas that will be swapped.
     */
    public void swapQueue(String category, String newSubAtlas) {
        if (!primaryAtlas.containsKey(category)) {
            System.err.println("Invalid swap request: " + category + " -> " + newSubAtlas);
            return;
        }
        swapQueue.put(category, newSubAtlas); // Store the latest swap request for this category
    }

    /**
     * This method processes the queue made from our swapQueue method.
     * @return validity check
     */
    public boolean swapBatch() {
        if (swapQueue.isEmpty()) {
            return false;
        }

        for (Map.Entry<String, String> entry : swapQueue.entrySet()) {
            String category = entry.getKey();
            String newSubAtlas = entry.getValue();

            // Fetch the subAtlas
            Map<Texture, Rectangle> foundSubAtlas = null;
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlases.getCollection(category)) {
                if (subAtlasEntry.containsKey(newSubAtlas)) {
                    foundSubAtlas = subAtlasEntry.get(newSubAtlas);
                    break;
                }
            }
            if (foundSubAtlas == null || primaryAtlas.get(category) == foundSubAtlas) {
                continue; // Skip unnecessary swaps
            }
            primaryAtlas.put(category, foundSubAtlas); // Swap subAtlas
        }
        if (inMemory) {
            for (Map.Entry<String, String> entry : swapQueue.entrySet()) {
                String category = entry.getKey();
                Map<Texture, Rectangle> swappedSubAtlas = primaryAtlas.get(category);

                // Batch OpenGL updates
                for (Map.Entry<Texture, Rectangle> textureEntry : swappedSubAtlas.entrySet()) {
                    Rectangle rect = textureEntry.getValue();
                    ByteBuffer textureBuffer = textureEntry.getKey().getByteBuffer((byte) 0);

                    if (textureBuffer != null) {
                        glBindTexture(atlasSlot, atlasID); // Bind once
                        glTexSubImage2D(
                                atlasSlot, 0, rect.x, rect.y, rect.width, rect.height,
                                GL_RGBA, GL_UNSIGNED_BYTE, textureBuffer
                        );
                    }
                }
            }
            glBindTexture(atlasSlot, 0); // Unbind after batch update
        }
        swapQueue.clear(); // Clear after processing
        return true;
    }

    private float[] toUV(Rectangle rect) {
        return new float[]{
        (float) rect.x / primaryAtlasSize[0],
        (float) rect.y / primaryAtlasSize[1],
        (float) (rect.x + rect.width) / primaryAtlasSize[0],
        (float) (rect.y + rect.height) / primaryAtlasSize[1]};
    }
    /**
     * This method should run only during our first initialization of each sub-atlas / application setup when a save is absent.
     */
    public void saveAtlasConfiguration() {
        // Implement serialization logic here
    }

    /**
     * This method is used to load default atlas configurations so our sequential load times are quick.
     */
    public void loadAtlasConfiguration() {
        // Implement deserialization logic here
    }
}