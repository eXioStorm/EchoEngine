package com.github.exiostorm.graphics;

import com.github.exiostorm.utils.MathTools;
import org.apache.commons.collections4.map.MultiValueMap;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class AtlasManager {


    public static TextureAtlas createAtlasFromFile(String path) {
        TextureAtlas existingAtlas = new TextureAtlas();
        //TODO [0] need to implement logic to load pre-made atlas from file.
        //TODO read the json provided for all the next values. will need something to iterate for all textures within the json.
        int placeholder = 0;
        existingAtlas.setWidth(placeholder);
        existingAtlas.setHeight(placeholder);
        AtlasManager.addToAtlas(existingAtlas, "", "", TextureManager.addTexture(""), 0, 0);
        return existingAtlas;
    }
    //TODO VV
    // make core method to reduce reused logic
    /**
     * This is our method for adding textures to both primary, and sub atlas. placement will be calculated later.
     * @param atlas Atlas to add to.
     * @param category Category this texture belongs to.
     * @param subAtlas SubAtlas this texture belongs to.
     * @param texture The texture itself.
     */
    public static void addToAtlas(TextureAtlas atlas, String category, String subAtlas, Texture texture) {
        addToAtlas(atlas, category, subAtlas, texture, 0, 0);
    }
    /**
     * Method for manual atlas texture placement.
     * @param x texture x coordinate
     * @param y texture y coordinate
     */
    public static void addToAtlas(TextureAtlas atlas, String category, String subAtlas, Texture texture, int x, int y) {
        atlas.getPrimaryAtlas().putIfAbsent(category, new HashMap<>());

        Map<String, Map<Texture, Rectangle>> categorySubAtlases = atlas.getSubAtlases().getCollection(category).iterator().next();
        if (categorySubAtlases == null) {
            categorySubAtlases = new HashMap<>();
            atlas.getSubAtlases().put(category, categorySubAtlases);
        }

        Map<Texture, Rectangle> subAtlasMap = categorySubAtlases.computeIfAbsent(subAtlas, k -> new HashMap<>());

        subAtlasMap.put(texture, new Rectangle(x, y, texture.getWidth(), texture.getHeight()));
    }
    //TODO ^^
    public static void newAddToAtlas(TextureAtlas atlas, String category, String subAtlas, Texture texture, int x, int y) {
        // Initialize the primary atlas entry for this category if it doesn't exist
        atlas.getNewPrimaryAtlas().putIfAbsent(category, subAtlas);

        // Check if the category exists in subAtlases
        if (!atlas.getNewSubAtlases().containsKey(category)) {
            // Create a new MultiValueMap for this category
            MultiValueMap<String, Map<Texture, Rectangle>> newCategoryMap = new MultiValueMap<>();
            atlas.getNewSubAtlases().put(category, newCategoryMap);
        }

        // Get the collection of MultiValueMaps for this category
        Collection<MultiValueMap<String, Map<Texture, Rectangle>>> categoryMaps = atlas.getNewSubAtlases().getCollection(category);

        // Try to find an existing MultiValueMap that contains the desired subAtlas
        MultiValueMap<String, Map<Texture, Rectangle>> targetMap = null;
        for (MultiValueMap<String, Map<Texture, Rectangle>> map : categoryMaps) {
            if (map.containsKey(subAtlas)) {
                targetMap = map;
                break;
            }
        }

        // If no existing map contains this subAtlas, create a new one
        if (targetMap == null) {
            targetMap = new MultiValueMap<>();
            atlas.getNewSubAtlases().put(category, targetMap);
        }

        // Get or create the collection of texture maps for this subAtlas
        Collection<Map<Texture, Rectangle>> textureMaps = targetMap.getCollection(subAtlas);
        Map<Texture, Rectangle> textureMap;

        if (textureMaps.isEmpty()) {
            // Create a new map for textures
            textureMap = new HashMap<>();
            targetMap.put(subAtlas, textureMap);
        } else {
            // Use the first map in the collection
            textureMap = textureMaps.iterator().next();
        }

        // Add the texture to the map
        textureMap.put(texture, new Rectangle(x, y, texture.getWidth(), texture.getHeight()));
    }
    public void newAtlasSwapQueue(TextureAtlas atlas, String category, String newSubAtlas) {
        if (!atlas.getNewPrimaryAtlas().containsKey(category)) {
            System.err.println("Invalid swap request: " + category + " -> " + newSubAtlas);
            return;
        }
        atlas.getSwapQueue().put(category, newSubAtlas);
    }
    /**
     * This method allows us to swap subAtlases, allowing us to only keep textures bound when necessary.
     * @param category name of the texture category
     * @param newSubAtlas the new subAtlas that will be swapped.
     */
    public void atlasSwapQueue(TextureAtlas atlas, String category, String newSubAtlas) {
        if (!atlas.getPrimaryAtlas().containsKey(category)) {
            System.err.println("Invalid swap request: " + category + " -> " + newSubAtlas);
            return;
        }
        atlas.getSwapQueue().put(category, newSubAtlas); // Store the latest swap request for this category
    }
    public boolean newAtlasSwapBatch(TextureAtlas atlas) {
        if (atlas.getSwapQueue().isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : atlas.getSwapQueue().entrySet()) {
            String category = entry.getKey();
            String newSubAtlas = entry.getValue();

            // Find the specified subAtlas in the new structure
            Map<Texture, Rectangle> foundSubAtlas = null;
            for (MultiValueMap<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getNewSubAtlases().getCollection(category)) {
                if (subAtlasEntry.containsKey(newSubAtlas)) {
                    // Fix: Use proper method to get the value from MultiValueMap
                    // This assumes that get() returns a Map<Texture, Rectangle>
                    foundSubAtlas = subAtlasEntry.getCollection(newSubAtlas).iterator().next();
                    break;
                }
            }

            // Skip if subAtlas not found or already the primary
            String currentPrimaryKey = atlas.getNewPrimaryAtlas().get(category);
            if (foundSubAtlas == null || newSubAtlas.equals(currentPrimaryKey)) {
                continue;
            }

            // Update the primary atlas with the new subAtlas key
            atlas.getNewPrimaryAtlas().put(category, newSubAtlas);
        }

        if (atlas.getInMemory()) {
            for (Map.Entry<String, String> entry : atlas.getSwapQueue().entrySet()) {
                String category = entry.getKey();
                String subAtlasKey = atlas.getNewPrimaryAtlas().get(category);

                // Find the actual texture map for the new primary atlas key
                Map<Texture, Rectangle> swappedSubAtlas = null;
                for (MultiValueMap<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getNewSubAtlases().getCollection(category)) {
                    if (subAtlasEntry.containsKey(subAtlasKey)) {
                        // Fix: Use proper method to get the value from MultiValueMap
                        swappedSubAtlas = subAtlasEntry.getCollection(subAtlasKey).iterator().next();
                        break;
                    }
                }

                if (swappedSubAtlas != null) {
                    for (Map.Entry<Texture, Rectangle> textureEntry : swappedSubAtlas.entrySet()) {
                        Rectangle rect = textureEntry.getValue();
                        ByteBuffer textureBuffer = textureEntry.getKey().getByteBuffer((byte) 0);
                        if (textureBuffer != null) {
                            glBindTexture(atlas.getAtlasSlot(), atlas.getAtlasID());
                            glTexSubImage2D(
                                    atlas.getAtlasSlot(), 0, rect.x, rect.y, rect.width, rect.height,
                                    GL_RGBA, GL_UNSIGNED_BYTE, textureBuffer
                            );
                        }
                    }
                }
            }
            glBindTexture(atlas.getAtlasSlot(), 0);
        }

        atlas.getSwapQueue().clear();
        return true;
    }
    /**
     * This method processes the queue made from our swapQueue method.
     * @return validity check
     */
    public boolean atlasSwapBatch(TextureAtlas atlas) {
        if (atlas.getSwapQueue().isEmpty()) {
            return false;
        }

        for (Map.Entry<String, String> entry : atlas.getSwapQueue().entrySet()) {
            String category = entry.getKey();
            String newSubAtlas = entry.getValue();

            // Fetch the subAtlas
            Map<Texture, Rectangle> foundSubAtlas = null;
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getSubAtlases().getCollection(category)) {
                if (subAtlasEntry.containsKey(newSubAtlas)) {
                    foundSubAtlas = subAtlasEntry.get(newSubAtlas);
                    break;
                }
            }
            if (foundSubAtlas == null || atlas.getPrimaryAtlas().get(category) == foundSubAtlas) {
                continue; // Skip unnecessary swaps
            }
            atlas.getPrimaryAtlas().put(category, foundSubAtlas); // Swap subAtlas
        }
        if (atlas.getInMemory()) {
            for (Map.Entry<String, String> entry : atlas.getSwapQueue().entrySet()) {
                String category = entry.getKey();
                Map<Texture, Rectangle> swappedSubAtlas = atlas.getPrimaryAtlas().get(category);

                // Batch OpenGL updates
                for (Map.Entry<Texture, Rectangle> textureEntry : swappedSubAtlas.entrySet()) {
                    Rectangle rect = textureEntry.getValue();
                    ByteBuffer textureBuffer = textureEntry.getKey().getByteBuffer((byte) 0);

                    if (textureBuffer != null) {
                        glBindTexture(atlas.getAtlasSlot(), atlas.getAtlasID()); // Bind once
                        glTexSubImage2D(
                                atlas.getAtlasSlot(), 0, rect.x, rect.y, rect.width, rect.height,
                                GL_RGBA, GL_UNSIGNED_BYTE, textureBuffer
                        );
                    }
                }
            }
            glBindTexture(atlas.getAtlasSlot(), 0); // Unbind after batch update
        }
        atlas.getSwapQueue().clear(); // Clear after processing
        return true;
    }
    public void newSaveAtlasToGPU(TextureAtlas atlas) {
        ByteBuffer atlasBuffer = ByteBuffer.allocateDirect(atlas.getWidth() * atlas.getHeight() * 4).order(ByteOrder.nativeOrder());

        // Initialize the buffer with zeros (transparent background)
        for (int i = 0; i < atlas.getWidth() * atlas.getHeight() * 4; i++) {
            atlasBuffer.put((byte) 0);
        }

        // Iterate over all categories in the primary atlas
        for (String category : atlas.getNewPrimaryAtlas().keySet()) {
            if (!atlas.getNewSubAtlases().containsKey(category)) continue;

            // Retrieve all subAtlas collections for the given category
            for (MultiValueMap<String, Map<Texture, Rectangle>> subAtlasMap : atlas.getNewSubAtlases().getCollection(category)) {
                // Iterate through each key in the subAtlasMap
                for (String subAtlasKey : subAtlasMap.keySet()) {
                    // Get the collection of maps for this key
                    Collection<Map<Texture, Rectangle>> mapCollection = subAtlasMap.getCollection(subAtlasKey);

                    // Process each map in the collection
                    for (Map<Texture, Rectangle> subAtlas : mapCollection) {
                        // Now, safely iterate over subAtlas entries with proper typing
                        for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
                            Rectangle rect = entry.getValue();
                            ByteBuffer textureBuffer = entry.getKey().getByteBuffer((byte) 0);

                            if (textureBuffer != null) {
                                for (int row = 0; row < rect.height; row++) {
                                    for (int col = 0; col < rect.width; col++) {
                                        int atlasIndex = ((rect.y + row) * atlas.getWidth() + (rect.x + col)) * 4;
                                        int textureIndex = (row * rect.width + col) * 4;

                                        atlasBuffer.put(atlasIndex, textureBuffer.get(textureIndex));
                                        atlasBuffer.put(atlasIndex + 1, textureBuffer.get(textureIndex + 1));
                                        atlasBuffer.put(atlasIndex + 2, textureBuffer.get(textureIndex + 2));
                                        atlasBuffer.put(atlasIndex + 3, textureBuffer.get(textureIndex + 3));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Upload to GPU
        atlasBuffer.flip();
        glBindTexture(atlas.getAtlasSlot(), atlas.getAtlasID());
        glTexImage2D(atlas.getAtlasSlot(), 0, GL_RGBA, atlas.getWidth(), atlas.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glTexParameteri(atlas.getAtlasSlot(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(atlas.getAtlasSlot(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(atlas.getAtlasSlot(), 0);

        // Mark atlas as loaded in memory
        atlas.setInMemory(true);
        System.out.println("Atlas finalized and uploaded. Dimensions: " + atlas.getWidth() + "x" + atlas.getHeight());
    }

    public void saveAtlasToGPU(TextureAtlas atlas) {
        ByteBuffer atlasBuffer = ByteBuffer.allocateDirect(atlas.getWidth() * atlas.getHeight() * 4).order(ByteOrder.nativeOrder());
        // Fill the buffer with transparent pixels
        for (int i = 0; i < atlas.getWidth() * atlas.getHeight() * 4; i++) {
            atlasBuffer.put((byte) 0);
        }

        // Process each texture in the atlas
        for (Map<Texture, Rectangle> subAtlas : atlas.getPrimaryAtlas().values()) {
            for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
                Rectangle rect = entry.getValue();

                ByteBuffer textureBuffer = entry.getKey().getByteBuffer((byte) 0);
                if (textureBuffer != null) {
                    for (int row = 0; row < rect.height; row++) {
                        for (int col = 0; col < rect.width; col++) {
                            int atlasIndex = ((rect.y + row) * atlas.getWidth() + (rect.x + col)) * 4;
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
        glBindTexture(atlas.getAtlasSlot(), atlas.getAtlasID());
        glTexImage2D(atlas.getAtlasSlot(), 0, GL_RGBA, atlas.getWidth(), atlas.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glTexParameteri(atlas.getAtlasSlot(), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(atlas.getAtlasSlot(), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(atlas.getAtlasSlot(), 0);
        atlas.setInMemory(true);
        System.out.println("Atlas finalized and uploaded. Dimensions: " + atlas.getWidth() + "x" + atlas.getHeight());
    }
    //TODO should move this to the TextureManager
    private void calculateAtlasPrimaryPlacement(TextureAtlas atlas) {
        Map<String, Rectangle> categoryPositions = new HashMap<>();

        for (String category : atlas.getPrimaryAtlas().keySet()) {
            //TODO might have issue with multivaluemap, after "subAtlases.getCollection(category)" add ".iterator().next()"
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getSubAtlases().getCollection(category)) {
                String subAtlasName = subAtlasEntry.keySet().iterator().next();
                categoryPositions.put(category, atlas.getSubAtlasSizes().get(subAtlasName));
            }
        }
        //TODO need a temporary Rectangle to hold width/height values
        Rectangle temp = new Rectangle(MathTools.rectanglePacker2D(categoryPositions));

        for (String category : atlas.getPrimaryAtlas().keySet()) {
            //TODO might have issue with multivaluemap, after "subAtlases.getCollection(category)" add ".iterator().next()"
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getSubAtlases().getCollection(category)) {
                for (Map<Texture, Rectangle> subAtlas : subAtlasEntry.values()) {
                    for (Texture textureName : subAtlas.keySet()) {
                        subAtlas.get(textureName).setLocation(
                                subAtlas.get(textureName).x + categoryPositions.get(category).x,
                                subAtlas.get(textureName).y + categoryPositions.get(category).y
                        );
                        //TODO check if this is redundant
                        //atlas.getTextureUV().put(textureName, toUV(atlas, subAtlas.get(textureName)));
                    }
                }
            }
        }
        atlas.setWidth(temp.width);
        atlas.setHeight(temp.height);
        //TODO still need to learn proper handling for avoiding the garbage collector.
        temp = null;
    }

    //TODO [0] had to use AI to generate new methods, not certain if they work.
    private void newCalculateAtlasPrimaryPlacement(TextureAtlas atlas) {
        Map<String, Rectangle> categoryPositions = new HashMap<>();

        // Iterate over primaryAtlas categories
        for (String category : atlas.getPrimaryAtlas().keySet()) {
            // Retrieve the MultiValueMap of subAtlases
            Collection<MultiValueMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getNewSubAtlases().getCollection(category);

            for (MultiValueMap<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlasCollections) {
                for (String subAtlasName : subAtlasEntry.keySet()) {
                    categoryPositions.put(category, atlas.getSubAtlasSizes().get(subAtlasName));
                }
            }
        }

        // Compute packed atlas size
        Rectangle temp = new Rectangle(MathTools.rectanglePacker2D(categoryPositions));

        // Update subAtlas texture positions based on category offsets
        for (String category : atlas.getPrimaryAtlas().keySet()) {
            Collection<MultiValueMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getNewSubAtlases().getCollection(category);

            for (MultiValueMap<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlasCollections) {
                for (String subAtlasName : subAtlasEntry.keySet()) {
                    MultiValueMap<String, Map<Texture, Rectangle>> innerMap = subAtlasEntry;

                    for (Map<Texture, Rectangle> subAtlas : innerMap.getCollection(subAtlasName)) {
                        for (Texture texture : subAtlas.keySet()) {
                            Rectangle rect = subAtlas.get(texture);
                            Rectangle offset = categoryPositions.get(category);

                            rect.setLocation(rect.x + offset.x, rect.y + offset.y);
                        }
                    }
                }
            }
        }
        // Update atlas dimensions
        atlas.setWidth(temp.width);
        atlas.setHeight(temp.height);
    }



    //TODO should probably make changes to this method for individual changes? idk...
    // Would prefer to do this step during initialization I think?
    public void calculateAtlasAllSubPlacements(TextureAtlas atlas) {
        for (String category : atlas.getSubAtlases().keySet()) {
            for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getSubAtlases().getCollection(category)) {
                for (String subAtlasName : subAtlasEntry.keySet()) {
                    Map<Texture, Rectangle> subAtlas = subAtlasEntry.get(subAtlasName);
                    //TODO moved method call inside of our put() since we have a return value now.
                    atlas.getSubAtlasSizes().put(subAtlasName, new Rectangle(
                            MathTools.rectanglePacker2D(subAtlas)
                    ));
                    /*
                    // Update UV mapping for all textures in the subAtlas
                    for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
                        //TODO check if this is redundant
                        atlas.getTextureUV().put(entry.getKey(), toUV(atlas, entry.getValue()));
                    }
                    */
                }
            }
        }
    }
    //TODO [0] had to use AI to generate new methods, not certain if they work.
    public void newCalculateAtlasAllSubPlacements(TextureAtlas atlas) {
        for (String category : atlas.getNewSubAtlases().keySet()) {
            // Retrieve all sub-atlas collections for the given category
            Collection<MultiValueMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getNewSubAtlases().getCollection(category);

            for (MultiValueMap<String, Map<Texture, Rectangle>> subAtlasCollection : subAtlasCollections) {
                for (String subAtlasName : subAtlasCollection.keySet()) {
                    // Merge all maps into a single map for processing
                    Map<Texture, Rectangle> mergedSubAtlas = new HashMap<>();

                    for (Map<Texture, Rectangle> subAtlas : subAtlasCollection.getCollection(subAtlasName)) {
                        mergedSubAtlas.putAll(subAtlas);
                    }

                    // Compute packed rectangle and store it in subAtlasSizes
                    atlas.getSubAtlasSizes().put(subAtlasName, new Rectangle(
                            MathTools.rectanglePacker2D(mergedSubAtlas)
                    ));
                }
            }
        }
    }

    public void newCalculateAtlasSubPlacement(TextureAtlas atlas, String category, String subAtlasName) {
        if (!atlas.getSubAtlases().containsKey(category)) {
            System.err.println("Category '" + category + "' not found.");
            return;
        }

        Map<Texture, Rectangle> subAtlas = null;

        // Retrieve collection of MultiValueMaps for the category
        Collection<MultiValueMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getNewSubAtlases().getCollection(category);

        for (MultiValueMap<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlasCollections) {
            if (subAtlasEntry.containsKey(subAtlasName)) {
                for (Map<Texture, Rectangle> subAtlasMap : subAtlasEntry.getCollection(subAtlasName)) {
                    subAtlas = subAtlasMap;
                    break;
                }
            }
            if (subAtlas != null) break;
        }

        if (subAtlas == null) {
            System.err.println("SubAtlas '" + subAtlasName + "' not found in category '" + category + "'.");
            return;
        }

        // Calculate and store packed size for the given subAtlas
        atlas.getSubAtlasSizes().put(subAtlasName, new Rectangle(
                MathTools.rectanglePacker2D(subAtlas)
        ));
    }

    public void calculateAtlasSubPlacement(TextureAtlas atlas, String category, String subAtlasName) {
        if (!atlas.getSubAtlases().containsKey(category)) {
            System.err.println("Category '" + category + "' not found.");
            return;
        }

        Map<Texture, Rectangle> subAtlas = null;

        for (Map<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getSubAtlases().getCollection(category)) {
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
        atlas.getSubAtlasSizes().put(subAtlasName, new Rectangle(
                MathTools.rectanglePacker2D(subAtlas)
        ));
        /*
        // Update UV mapping for all textures in the subAtlas
        for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
            //TODO check if this is redundant
            atlas.getTextureUV().put(entry.getKey(), toUV(atlas, entry.getValue()));
        }
        */
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
    public void finalizeAtlasMaps(TextureAtlas atlas) {
        calculateAtlasAllSubPlacements(atlas);
        calculateAtlasPrimaryPlacement(atlas);
    }
    private float[] toUV(TextureAtlas atlas, Rectangle rect) {
        return new float[]{
                (float) rect.x / atlas.getWidth(),
                (float) rect.y / atlas.getHeight(),
                (float) (rect.x + rect.width) / atlas.getWidth(),
                (float) (rect.y + rect.height) / atlas.getHeight()};
    }
}
