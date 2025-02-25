package com.github.exiostorm.renderer;

import lombok.Getter;

import java.awt.Rectangle;
import java.util.*;

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
    private Map<String, Map<String, Rectangle>> primaryAtlas; // Category -> Active SubAtlas (Texture Name -> Placement)
    private Map<String, Map<String, Map<String, Rectangle>>> subAtlases; // Category -> (SubAtlas Name -> (Texture Name -> Placement))
    private Map<String, Rectangle> subAtlasSizes; // SubAtlas Name -> [Used x, Used y, Used Width, Used Height]
    private int[] primaryAtlasSize;
    @Getter
    private Map<String, float[]> textureUV; // Texture Name -> Placement
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
        subAtlases.get(category).get(subAtlas).put(texture.getPath(), new Rectangle(0, 0, texture.getWidth(), texture.getHeight()));
    }

    /**
     * This is our bin packer, use other methods for our texture Atlas...
     * @param rectangleMap Map<String, Rectangle> for packing Rectangles to an area.
     */
    //TODO I should probably move this to a different class? is now more useful than just for the texture atlas.
    public void rectanglePacker(Map<String, Rectangle> rectangleMap) {
        List<Rectangle> sortedTextures = new ArrayList<>(rectangleMap.values());
        sortedTextures.sort(Comparator.comparingInt(r -> -r.width * r.height)); // Sort by area descending

        freeRectangles.clear();
        freeRectangles.add(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)); // Initial large free area

        calculatedSize.setBounds(0,0,0,0);

        for (Rectangle texture : sortedTextures) {
            int bestAreaFit = Integer.MAX_VALUE;
            Rectangle bestRect = null;
            int bestIndex = -1;

            for (int i = 0; i < freeRectangles.size(); i++) {
                Rectangle freeRect = freeRectangles.get(i);
                if (freeRect.width >= texture.width && freeRect.height >= texture.height) {
                    int areaFit = freeRect.width * freeRect.height - texture.width * texture.height;
                    if (areaFit < bestAreaFit) {
                        bestRect = new Rectangle(freeRect.x, freeRect.y, texture.width, texture.height);
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
                rectangleMap.put(getTextureKey(rectangleMap, texture), bestRect);
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
    public void calculatePrimaryPlacement(){
        Map<String, Rectangle> categoryPositions = new HashMap<>();
        for (String category : primaryAtlas.keySet()) {
            categoryPositions.put(category, subAtlasSizes.get(primaryAtlas.get(category).keySet().iterator().next()));
        }

        rectanglePacker(categoryPositions);

        for (String category : primaryAtlas.keySet()) {
            for (Map<String, Rectangle> subAtlas : subAtlases.get(category).values()) {
                for (String textureName : subAtlas.keySet()) {
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
    public void calculateAllSubPlacements(){
        for (String category : subAtlases.keySet()) {
            for (String subAtlasName : subAtlases.get(category).keySet()){
                //TODO I think this will calculate specifically this section of our subAtlases / by subAtlasName?
                rectanglePacker(subAtlases.get(category).get(subAtlasName));
                subAtlasSizes.put(subAtlasName, new Rectangle(calculatedSize));
            }
        }
    }

    /**
     * This methods allows us to swap subAtlases, allowing us to only keep textures bound when necessary.
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
        return true;
    }

    /**
     * This method is used in our calculatePlacement() method, I'm not sure why we need a separate method. will merge?
     * @param textureMap
     * @param texture
     * @return validity check.
     */
    private String getTextureKey(Map<String, Rectangle> textureMap, Rectangle texture) {
        for (Map.Entry<String, Rectangle> entry : textureMap.entrySet()) {
            if (entry.getValue().equals(texture)) {
                return entry.getKey();
            }
        }
        return null;
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