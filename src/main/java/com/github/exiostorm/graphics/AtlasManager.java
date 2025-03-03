package com.github.exiostorm.graphics;

import com.github.exiostorm.utils.MathTools;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class AtlasManager {
    public static TextureAtlas createAtlasFromJson(String jsonPath) {
        //TODO need to implement logic to create a new atlas from a json file.
        return new TextureAtlas();
    }
    /**
     * This is our method for adding textures, all textures belong to a subAtlas.
     * @param category Category this texture belongs to.
     * @param subAtlas SubAtlas this texture belongs to.
     * @param texture The texture itself.
     */
    public static void addToAtlas(TextureAtlas atlas, String category, String subAtlas, Texture texture){
        atlas.getPrimaryAtlas().putIfAbsent(category, new HashMap<>());

        Map<String, Map<Texture, Rectangle>> categorySubAtlases = atlas.getSubAtlases().getCollection(category).iterator().next();
        if (categorySubAtlases == null) {
            categorySubAtlases = new HashMap<>();
            atlas.getSubAtlases().put(category, categorySubAtlases);
        }

        Map<Texture, Rectangle> subAtlasMap = categorySubAtlases.computeIfAbsent(subAtlas, k -> new HashMap<>());

        subAtlasMap.put(texture, new Rectangle(0, 0, texture.getWidth(), texture.getHeight()));
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
                        atlas.getTextureUV().put(textureName, toUV(atlas, subAtlas.get(textureName)));
                    }
                }
            }
        }
        atlas.setWidth(temp.width);
        atlas.setHeight(temp.height);
        //TODO still need to learn proper handling for avoiding the garbage collector.
        temp = null;
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
                    // Update UV mapping for all textures in the subAtlas
                    for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
                        atlas.getTextureUV().put(entry.getKey(), toUV(atlas, entry.getValue()));
                    }
                }
            }
        }
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

        // Update UV mapping for all textures in the subAtlas
        for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
            atlas.getTextureUV().put(entry.getKey(), toUV(atlas, entry.getValue()));
        }
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
