package com.github.exiostorm.renderer;

import java.awt.Rectangle;
import java.util.*;

public class TextureAtlasMaps {
    /**
     * List of free rectangles used for tracking available space in the atlas.
     * This list is updated and used within the calculatePlacement() method
     * to determine where textures can be placed efficiently.
     */
    private List<Rectangle> freeRectangles;
    private Map<String, Map<String, Rectangle>> primaryAtlas; // Category -> Active SubAtlas (Texture Name -> Placement)
    private Map<String, Map<String, Map<String, Rectangle>>> subAtlases; // Category -> (SubAtlas Name -> (Texture Name -> Placement))
    private Map<String, Rectangle> subAtlasSizes; // SubAtlas Name -> [Used x, Used y, Used Width, Used Height]
    private int[] primaryAtlasSize;
    private Map<String, float[]> textureUV;
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
    //TODO figure out a way to make this compatible with our mainAtlas setup..
    private void calculatePlacement(Map<String, Rectangle> textureMap) {
        List<Rectangle> sortedTextures = new ArrayList<>(textureMap.values());
        sortedTextures.sort(Comparator.comparingInt(r -> -r.width * r.height)); // Sort by area descending

        freeRectangles.clear();
        freeRectangles.add(new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE)); // Initial large free area

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

                textureMap.put(getTextureKey(textureMap, texture), bestRect);
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
        //TODO need to iterate through each Category and calculate positions, then calculate each category for overall atlas, the atlas size, and our texture uv positions in our texture/UV map.
        // Category -> Active SubAtlas (Texture Name -> Placement)
        // SubAtlas Name -> [Used x, Used y, Used Width, Used Height]
        Map<String, Rectangle> categorySizes = new HashMap<>();
        for (String category : primaryAtlas.keySet()) {
            categorySizes.put(category, subAtlasSizes.get(primaryAtlas.get(category).keySet().iterator().next()));
        }
        calculatePlacement(categorySizes);
        primaryAtlasSize[0] = 1; //X
        primaryAtlasSize[1] = 2; //Y
    }
    public void calculateSubPlacement(){
        //TODO need to iterate through each subAtlas and calculate positions and sizes.
        // Category -> (SubAtlas Name -> (Texture Name -> Placement))

        // get our categories for our subAtlases
        for (String category : subAtlases.keySet()) {
            for (String subAtlasName : subAtlases.get(category).keySet()){
                //TODO I think this will calculate specifically this section of our subAtlases / by subAtlasName?
                calculatePlacement(subAtlases.get(category).get(subAtlasName));
                //subAtlasSizes.put(subAtlasName, ); //We're missing a way to save the size of the calculated atlas. we need to define a Rectangle inside our calculatePlacement() method, and then immediate after using the method we need to retrieve the Rectangle for our size.
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
    //TODO this method doesn't make sense. probably because we're missing logic for the overall atlas placement.
    public Map<String, Map<String, Rectangle>> getCategoryPlacement() {
        return primaryAtlas;
    }

    /**
    * This method should be used to populate our Texture UV map so we can quickly retrieve locations based on textures instead of having extra parameters for retrieving positions through checking our subatlas maps.
    */
    public float[] getUV(Texture texture) {
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