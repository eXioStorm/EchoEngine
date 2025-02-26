package com.github.exiostorm.renderer;

import lombok.Getter;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

public class TextureAtlasMaps {
    /**
     * List of free rectangles used for tracking available space in the atlas.
     * This list is updated and used within the calculatePlacement() method
     * to determine where textures can be placed efficiently.
     */
    private List<Rectangle> freeRectangles;
    /**
     * value is set inside our calculatePlacement() method, and used by our other methods calculatePrimaryPlacement() and calculateSubPlacement().
     */
    @Getter
    private Rectangle calculatedSize;
    //TODO maybe switch these back to using String instead of Texture... idk, might want to fix modularity?
    private Map<String, Map<Texture, Rectangle>> primaryAtlas; // Category -> Active SubAtlas (Texture Name -> Placement)
    private Map<String, Map<String, Map<Texture, Rectangle>>> subAtlases; // Category -> (SubAtlas Name -> (Texture Name -> Placement))
    //Separate from subAtlases map to avoid anymore complexity than we already have.
    private Map<String, Rectangle> subAtlasSizes; // SubAtlas Name -> [Used x, Used y, Used Width, Used Height]
    //Separate from primaryAtlas map to avoid anymore complexity than we already have.
    private int[] primaryAtlasSize;
    //Separate from subAtlases map to make coordinate retrieval quick.
    @Getter
    private Map<Texture, float[]> textureUV; // Texture Name -> Placement
    //To be used later with swapSubAtlas, so we can also update the atlas on the GPU.
    private int atlasID;
    private boolean inMemory = false;

    /**
     * initialized all of our mappings.
     */
    public TextureAtlasMaps() {
        this.freeRectangles = new ArrayList<>();
        this.subAtlases = new HashMap<>();
        this.primaryAtlas = new HashMap<>();
        this.subAtlasSizes = new HashMap<>();
        this.primaryAtlasSize = new int[2];
        this.textureUV = new HashMap<>();
        this.calculatedSize = new Rectangle(0,0,0,0);
        atlasID = glGenTextures();
    }
    public void bind() {
        glBindTexture(GL_TEXTURE_2D, atlasID);
    }
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
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
        glBindTexture(GL_TEXTURE_2D, atlasID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, primaryAtlasSize[0], primaryAtlasSize[1], 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);
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
        subAtlases.putIfAbsent(category, new HashMap<>());
        subAtlases.get(category).putIfAbsent(subAtlas, new HashMap<>());
        subAtlases.get(category).get(subAtlas).put(texture, new Rectangle(0, 0, texture.getWidth(), texture.getHeight()));
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
            categoryPositions.put(category, subAtlasSizes.get(primaryAtlas.get(category).keySet().iterator().next()));
        }

        rectanglePacker(categoryPositions);

        for (String category : primaryAtlas.keySet()) {
            for (Map<Texture, Rectangle> subAtlas : subAtlases.get(category).values()) {
                for (Texture textureName : subAtlas.keySet()) {
                    subAtlas.get(textureName).setLocation(subAtlas.get(textureName).x + categoryPositions.get(category).x, subAtlas.get(textureName).y + categoryPositions.get(category).y);
                    // This should set our UV coordinates for our textures.
                    textureUV.put(textureName, new float[]{
                            (float) subAtlas.get(textureName).x / calculatedSize.width,
                            (float) subAtlas.get(textureName).y / calculatedSize.height,
                            (float) (subAtlas.get(textureName).x + subAtlas.get(textureName).width) / calculatedSize.width,
                            (float) (subAtlas.get(textureName).y + subAtlas.get(textureName).height) / calculatedSize.height});
                }
            }
        }
        primaryAtlasSize[0] = calculatedSize.width; //X
        primaryAtlasSize[1] = calculatedSize.height; //Y
    }
    //TODO should probably make changes to this method for individual changes? idk...
    // Would prefer to do this step during initialization I think?
    private void calculateAllSubPlacements() {
        for (String category : subAtlases.keySet()) {
            for (String subAtlasName : subAtlases.get(category).keySet()){
                //TODO I think this will calculate specifically this section of our subAtlases / by subAtlasName?
                // NOPE. this didn't work as expected.?
                rectanglePacker(subAtlases.get(category).get(subAtlasName));
                subAtlasSizes.put(subAtlasName, new Rectangle(calculatedSize));
            }
        }
    }

    private void calculateSubPlacement(String category, String subAtlasName) {
        if (!subAtlases.get(category).get(subAtlasName).isEmpty()) {
            //TODO nor did this one.?? -_-
            rectanglePacker(subAtlases.get(category).get(subAtlasName));
            subAtlasSizes.put(subAtlasName, new Rectangle(calculatedSize));
        }
    }

    public void finalizeAtlasMaps() {
        calculateAllSubPlacements();
        calculatePrimaryPlacement();
    }

    /**
     * This method allows us to swap subAtlases, allowing us to only keep textures bound when necessary.
     * We should use inMemory boolean to swap this region of the atlas in our GPU after this method is called.
     * @param category name of the texture category
     * @param newSubAtlas the new subAtlas that will be swapped.
     * @return validity check.
     */
    public boolean swapSubAtlas(String category, String newSubAtlas) {
        if (!subAtlases.containsKey(category) || !subAtlases.get(category).containsKey(newSubAtlas)) {
            return false;
        }
        if (primaryAtlas.get(category) == subAtlases.get(category).get(newSubAtlas)) {
            return false; // Prevent unnecessary swap
        }
        primaryAtlas.put(category, subAtlases.get(category).get(newSubAtlas)); // Directly swap active sub-atlas
        if (inMemory) {
            Map<Texture, Rectangle> swappedSubAtlas = primaryAtlas.get(category);
            for (Map.Entry<Texture, Rectangle> entry : swappedSubAtlas.entrySet()) {
                Rectangle rect = entry.getValue();
                // Fetch the existing texture from the texture map
                ByteBuffer textureBuffer = entry.getKey().getByteBuffer((byte) 0);

                if (textureBuffer != null) {
                    glBindTexture(GL_TEXTURE_2D, atlasID);
                    glTexSubImage2D(
                            GL_TEXTURE_2D,  // Target texture type
                            0,              // Mipmap level
                            rect.x,         // X offset in the atlas
                            rect.y,         // Y offset in the atlas
                            rect.width,     // Width of the sub-image
                            rect.height,    // Height of the sub-image
                            GL_RGBA,        // Format
                            GL_UNSIGNED_BYTE, // Data type
                            textureBuffer   // ByteBuffer containing pixel data
                    );
                    glBindTexture(GL_TEXTURE_2D, 0); // Unbind texture
                }
            }
        }
        return true;
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