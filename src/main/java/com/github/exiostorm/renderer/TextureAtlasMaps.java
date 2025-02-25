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
    private Map<String, int[]> subAtlasSizes; // SubAtlas Name -> [Used Width, Used Height]
    private int[] primaryAtlasSize;
    private Map<String, float[]> textureUV;

    public TextureAtlasMaps() {
        this.freeRectangles = new ArrayList<>();
        this.subAtlases = new HashMap<>();
        this.primaryAtlas = new HashMap<>();
        this.textureUV = new HashMap<>();
    }

    /**
     * This method should be removed, the logic should be handled inside of our method "addTexture()"
     */
    private boolean addCategory(String category) {
        if (primaryAtlas.containsKey(category)) return false;
        primaryAtlas.put(category, new HashMap<>());
        return true;
    }

    public boolean addTexture(String category, String subAtlas, Texture texture) {
        if (!primaryAtlas.containsKey(category)) return false;

        subAtlases.putIfAbsent(category, new HashMap<>());
        subAtlases.get(category).putIfAbsent(subAtlas, new HashMap<>());
        subAtlases.get(category).get(subAtlas).put(texture.getPath(), new Rectangle(0, 0, texture.getWidth(), texture.getHeight()));
        return true;
    }

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

    public void calculatePrimaryPlacement(){
        //TODO need to iterate through each Category and calculate positions, then calculate each category for overall atlas, the atlas size, and our texture uv positions in our texture/UV map.
        //calculatePlacement(primaryAtlas);
    }
    public void calculateSubPlacement(){
        //TODO need to iterate through each subAtlas and calculate positions and sizes.
    }

    /**
     * This methods allows us to swap subAtlases, allowing us to only keep textures bound when necessary.
     * @param category name of the texture category
     * @param newSubAtlas the new subAtlas that will be swapped.
     * @return return method for valid action confirmation.
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

    public String getActiveSubAtlas(String category) {
        if (!subAtlases.containsKey(category)) return null;
        for (Map.Entry<String, Map<String, Rectangle>> entry : subAtlases.get(category).entrySet()) {
            if (entry.getValue() == primaryAtlas.get(category)) {
                return entry.getKey();
            }
        }
        return null;
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