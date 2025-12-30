package com.github.exiostorm.graphics;

import com.github.exiostorm.utils.MathTools;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import javax.imageio.ImageIO;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.*;

public class AtlasManager {
    static Map<String, TextureAtlas> atlases = new HashMap<>();

    public static TextureAtlas atlas(String path) {
        TextureAtlas atlas = new TextureAtlas();
        atlases.putIfAbsent(path, atlas);
        return atlas;
    }
    /**
     * Creates a TextureAtlas object from a JSON file.
     * @param path The path to the JSON file
     * @return The reconstructed TextureAtlas
     * @throws JSONException If there's an error parsing the JSON
     * @throws IOException If there's an error reading the file
     */
    public static TextureAtlas createAtlasFromFile(String path) throws JSONException, IOException {
        TextureAtlas existingAtlas = new TextureAtlas();

        // Read the JSON file
        String jsonContent = new String(Files.readAllBytes(Paths.get(path)));
        JSONObject jsonObject = new JSONObject(jsonContent);

        // Load primary atlas
        JSONObject primaryAtlasJson = jsonObject.getJSONObject("primaryAtlas");
        Map<String, String> primaryAtlas = new HashMap<>();
        Iterator<String> primaryKeys = primaryAtlasJson.keys();
        while (primaryKeys.hasNext()) {
            String key = primaryKeys.next();
            primaryAtlas.put(key, primaryAtlasJson.getString(key));
        }
        existingAtlas.setPrimaryAtlas(primaryAtlas);

        // Load width, height, and atlasSlot
        existingAtlas.setWidth(jsonObject.getInt("width"));
        existingAtlas.setHeight(jsonObject.getInt("height"));
        existingAtlas.setAtlasSlot(jsonObject.getInt("atlasSlot"));

        // Load subAtlasSizes
        JSONObject subAtlasSizesJson = jsonObject.getJSONObject("subAtlasSizes");
        Map<String, Rectangle> subAtlasSizes = new HashMap<>();
        Iterator<String> sizeKeys = subAtlasSizesJson.keys();
        while (sizeKeys.hasNext()) {
            String key = sizeKeys.next();
            JSONObject rectJson = subAtlasSizesJson.getJSONObject(key);
            Rectangle rect = new Rectangle(
                    rectJson.getInt("x"),
                    rectJson.getInt("y"),
                    rectJson.getInt("width"),
                    rectJson.getInt("height")
            );
            subAtlasSizes.put(key, rect);
        }
        existingAtlas.setSubAtlasSizes(subAtlasSizes);

        // Load texturePositions first
        JSONObject texturePositionsJson = jsonObject.getJSONObject("texturePositions");
        Map<Texture, Rectangle> texturePositions = new HashMap<>();
        Map<String, Rectangle> pathToRectMap = new HashMap<>(); // Helper map for lookups

        Iterator<String> textureKeys = texturePositionsJson.keys();
        while (textureKeys.hasNext()) {
            String texturePath = textureKeys.next();
            JSONObject rectJson = texturePositionsJson.getJSONObject(texturePath);

            Rectangle rect = new Rectangle(
                    rectJson.getInt("x"),
                    rectJson.getInt("y"),
                    rectJson.getInt("width"),
                    rectJson.getInt("height")
            );

            // Create texture from path
            Texture texture = TextureManager.addTexture(texturePath);

            // Store in the texture positions map
            texturePositions.put(texture, rect);

            // Also store in our helper map for quick lookups
            pathToRectMap.put(texturePath, rect);
        }
        existingAtlas.setTexturePositions(texturePositions);

        // Now load subAtlases, using references to the already created textures and rectangles
        JSONObject subAtlasesJson = jsonObject.getJSONObject("subAtlases");
        MultiValuedMap<String, MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlases = new ArrayListValuedHashMap<>();

        Iterator<String> mainKeys = subAtlasesJson.keys();
        while (mainKeys.hasNext()) {
            String mainKey = mainKeys.next();
            JSONArray subAtlasArray = subAtlasesJson.getJSONArray(mainKey);

            for (int i = 0; i < subAtlasArray.length(); i++) {
                JSONObject subMapJson = subAtlasArray.getJSONObject(i);
                MultiValuedMap<String, Map<Texture, Rectangle>> subMap = new ArrayListValuedHashMap<>();

                Iterator<String> subKeys = subMapJson.keys();
                while (subKeys.hasNext()) {
                    String subKey = subKeys.next();
                    JSONArray textureArray = subMapJson.getJSONArray(subKey);

                    for (int j = 0; j < textureArray.length(); j++) {
                        JSONObject textureMapJson = textureArray.getJSONObject(j);
                        Map<Texture, Rectangle> textureMap = new HashMap<>();

                        String texturePath = textureMapJson.getString("texturePath");

                        // Get the texture and rectangle from our already loaded data
                        Texture texture = TextureManager.addTexture(texturePath);
                        Rectangle rect = pathToRectMap.get(texturePath);

                        textureMap.put(texture, rect);
                        subMap.put(subKey, textureMap);
                    }
                }

                subAtlases.put(mainKey, subMap);
            }
        }
        existingAtlas.setSubAtlases(subAtlases);
        atlases.putIfAbsent(path, existingAtlas);
        return existingAtlas;
    }

    /**
     * Saves the atlas data to a JSON file.
     * @param filePath The path where the JSON file will be saved
     * @throws JSONException If there's an error creating the JSON
     * @throws IOException If there's an error writing to the file
     */
    public static void saveToJson(TextureAtlas atlas, String filePath) throws JSONException, IOException {
        // Create the main JSON object
        JSONObject jsonObject = new JSONObject();

        // Add primary atlas
        JSONObject primaryAtlasJson = new JSONObject();
        for (Map.Entry<String, String> entry : atlas.getPrimaryAtlas().entrySet()) {
            primaryAtlasJson.put(entry.getKey(), entry.getValue());
        }
        jsonObject.put("primaryAtlas", primaryAtlasJson);

        // Add width, height, and atlas slot
        jsonObject.put("width", atlas.getWidth());
        jsonObject.put("height", atlas.getHeight());
        jsonObject.put("atlasSlot", atlas.getAtlasSlot());

        // Add subAtlasSizes
        JSONObject subAtlasSizesJson = new JSONObject();
        for (Map.Entry<String, Rectangle> entry : atlas.getSubAtlasSizes().entrySet()) {
            String key = entry.getKey();
            Rectangle rect = entry.getValue();

            JSONObject rectJson = new JSONObject();
            rectJson.put("x", rect.x);
            rectJson.put("y", rect.y);
            rectJson.put("width", rect.width);
            rectJson.put("height", rect.height);

            subAtlasSizesJson.put(key, rectJson);
        }
        jsonObject.put("subAtlasSizes", subAtlasSizesJson);

        // First, save all texture positions
        JSONObject texturePositionsJson = new JSONObject();
        for (Map.Entry<Texture, Rectangle> entry : atlas.getTexturePositions().entrySet()) {
            Texture texture = entry.getKey();
            Rectangle rectangle = entry.getValue();

            JSONObject rectJson = new JSONObject();
            rectJson.put("x", rectangle.x);
            rectJson.put("y", rectangle.y);
            rectJson.put("width", rectangle.width);
            rectJson.put("height", rectangle.height);

            texturePositionsJson.put(texture.getPath(), rectJson);
        }
        jsonObject.put("texturePositions", texturePositionsJson);

        // Now process subAtlases with references to texturePositions instead of duplicating the data
        JSONObject subAtlasesJson = new JSONObject();

        for (String mainKey : atlas.getSubAtlases().keySet()) {
            JSONArray subAtlasArray = new JSONArray();

            Collection<MultiValuedMap<String, Map<Texture, Rectangle>>> mainValues = atlas.getSubAtlases().get(mainKey);
            if (mainValues != null) {
                for (MultiValuedMap<String, Map<Texture, Rectangle>> subMap : mainValues) {
                    JSONObject subMapJson = new JSONObject();

                    for (String subKey : subMap.keySet()) {
                        JSONArray textureArray = new JSONArray();

                        Collection<Map<Texture, Rectangle>> subValues = subMap.get(subKey);
                        if (subValues != null) {
                            for (Map<Texture, Rectangle> textureMap : subValues) {
                                JSONObject textureMapJson = new JSONObject();

                                for (Map.Entry<Texture, Rectangle> entry : textureMap.entrySet()) {
                                    Texture texture = entry.getKey();

                                    // Only store the path reference, not the full Rectangle data again
                                    textureMapJson.put("texturePath", texture.getPath());
                                }

                                textureArray.put(textureMapJson);
                            }
                        }

                        subMapJson.put(subKey, textureArray);
                    }

                    subAtlasArray.put(subMapJson);
                }
            }

            subAtlasesJson.put(mainKey, subAtlasArray);
        }

        jsonObject.put("subAtlases", subAtlasesJson);

        // Write to file
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(jsonObject.toString(2)); // The '2' parameter adds indentation for readability
        }
        atlases.put(filePath, atlas);
    }
    /**
     * This is our method for adding textures to both primary, and sub atlas. placement will be calculated later.
     * @param atlas Atlas to add to.
     * @param category Category this texture belongs to.
     * @param subAtlas SubAtlas this texture belongs to.
     * @param texture The texture itself.
     */
    public static boolean addToAtlas(TextureAtlas atlas, String category, String subAtlas, Texture texture) {
        return addToAtlas(atlas, category, subAtlas, texture, 0, 0);
    }
    /**
     * Method for manual atlas texture placement.
     * @param x texture x coordinate
     * @param y texture y coordinate
     */
    public static boolean addToAtlas(TextureAtlas atlas, String category, String subAtlas, Texture texture, int x, int y) {
        // Check if this texture is already mapped - exit early if it is
        if (atlas.getTexturePositions().containsKey(texture)) {
            return true; // Texture already exists in the atlas
        }

        if (!atlas.getPrimaryAtlas().containsKey(category)) {
            atlas.getPrimaryAtlas().put(category, subAtlas); // Store the first texture path for this category
        }

        // Ensure the category exists in newSubAtlases
        MultiValuedMap<String, Map<Texture, Rectangle>> subAtlasEntry;
        Collection<MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getSubAtlases().get(category);

        if (subAtlasCollections == null || subAtlasCollections.isEmpty()) {
            subAtlasEntry = new ArrayListValuedHashMap<>();
            atlas.getSubAtlases().put(category, subAtlasEntry);
        } else {
            subAtlasEntry = subAtlasCollections.iterator().next(); // Get first entry (assuming one per category)
        }

        // Retrieve or create the subAtlas map
        Map<Texture, Rectangle> subAtlasMap = null;
        for (Map<Texture, Rectangle> existingMap : subAtlasEntry.get(subAtlas)) {
            subAtlasMap = existingMap;
            break;
        }

        if (subAtlasMap == null) {
            subAtlasMap = new HashMap<>();
            subAtlasEntry.put(subAtlas, subAtlasMap);
        }

        // Add the new texture rectangle
        Rectangle rect = new Rectangle(x, y, texture.getWidth(), texture.getHeight());
        subAtlasMap.put(texture, rect);
        atlas.getTexturePositions().put(texture, rect); // Store in direct lookup map
        return false;
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
        atlas.getSwapQueue().put(category, newSubAtlas);
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

            // Find the specified subAtlas in the new structure
            Map<Texture, Rectangle> foundSubAtlas = null;
            for (MultiValuedMap<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getSubAtlases().get(category)) {
                if (subAtlasEntry.containsKey(newSubAtlas)) {
                    // Fix: Use proper method to get the value from MultiValuedMap
                    // This assumes that get() returns a Map<Texture, Rectangle>
                    foundSubAtlas = subAtlasEntry.get(newSubAtlas).iterator().next();
                    break;
                }
            }

            // Skip if subAtlas not found or already the primary
            String currentPrimaryKey = atlas.getPrimaryAtlas().get(category);
            if (foundSubAtlas == null || newSubAtlas.equals(currentPrimaryKey)) {
                continue;
            }

            // Update the primary atlas with the new subAtlas key
            atlas.getPrimaryAtlas().put(category, newSubAtlas);
        }

        if (atlas.getInMemory()) {
            for (Map.Entry<String, String> entry : atlas.getSwapQueue().entrySet()) {
                String category = entry.getKey();
                String subAtlasKey = atlas.getPrimaryAtlas().get(category);

                // Find the actual texture map for the new primary atlas key
                Map<Texture, Rectangle> swappedSubAtlas = null;
                for (MultiValuedMap<String, Map<Texture, Rectangle>> subAtlasEntry : atlas.getSubAtlases().get(category)) {
                    if (subAtlasEntry.containsKey(subAtlasKey)) {
                        // Fix: Use proper method to get the value from MultiValuedMap
                        swappedSubAtlas = subAtlasEntry.get(subAtlasKey).iterator().next();
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
    public static void saveAtlasToGPU(TextureAtlas atlas) {
        ByteBuffer atlasBuffer = ByteBuffer.allocateDirect(atlas.getWidth() * atlas.getHeight() * 4).order(ByteOrder.nativeOrder());

        // Initialize the buffer with zeros (transparent background)
        for (int i = 0; i < atlas.getWidth() * atlas.getHeight() * 4; i++) {
            atlasBuffer.put((byte) 0);
        }

        // Create a BufferedImage to visualize the atlas
        BufferedImage atlasImage = new BufferedImage(atlas.getWidth(), atlas.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Iterate over all categories in the primary atlas
        for (String category : atlas.getPrimaryAtlas().keySet()) {
            if (!atlas.getSubAtlases().containsKey(category)) continue;

            // Retrieve all subAtlas collections for the given category
            for (MultiValuedMap<String, Map<Texture, Rectangle>> subAtlasMap : atlas.getSubAtlases().get(category)) {
                for (String subAtlasKey : subAtlasMap.keySet()) {
                    for (Map<Texture, Rectangle> subAtlas : subAtlasMap.get(subAtlasKey)) {
                        for (Map.Entry<Texture, Rectangle> entry : subAtlas.entrySet()) {
                            Texture texture = entry.getKey();
                            Rectangle rect = entry.getValue();
                            ByteBuffer textureBuffer = texture.getByteBuffer((byte) 0);

                            if (textureBuffer != null) {
                                for (int row = 0; row < rect.height; row++) {
                                    for (int col = 0; col < rect.width; col++) {
                                        int atlasIndex = ((rect.y + row) * atlas.getWidth() + (rect.x + col)) * 4;
                                        int textureIndex = (row * rect.width + col) * 4;

                                        byte r = textureBuffer.get(textureIndex);
                                        byte g = textureBuffer.get(textureIndex + 1);
                                        byte b = textureBuffer.get(textureIndex + 2);
                                        byte a = textureBuffer.get(textureIndex + 3);

                                        atlasBuffer.put(atlasIndex, r);
                                        atlasBuffer.put(atlasIndex + 1, g);
                                        atlasBuffer.put(atlasIndex + 2, b);
                                        atlasBuffer.put(atlasIndex + 3, a);

                                        // Save pixels to BufferedImage for visualization
                                        int argb = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
                                        atlasImage.setRGB(rect.x + col, rect.y + row, argb);
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
        glTexParameteri(atlas.getAtlasSlot(), GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(atlas.getAtlasSlot(), GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(atlas.getAtlasSlot(), 0);

        // Save debug image
        String debugPath = "src/main/resources/tests/atlas.png";
        try {
            ImageIO.write(atlasImage, "PNG", new File(debugPath));
            System.out.println("Atlas saved as PNG to: " + debugPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Mark atlas as loaded in memory
        atlas.setInMemory(true);
        System.out.println("Atlas finalized and uploaded. Dimensions: " + atlas.getWidth() + "x" + atlas.getHeight());
    }

    //TODO [0] had to use AI to generate new methods, not certain if they work.
    private static void calculateAtlasPrimaryPlacement(TextureAtlas atlas) {
        // We only want to pack the active subAtlases (those in newPrimaryAtlas)
        MultiValuedMap<String, Map<Texture, Rectangle>> activeSubAtlases = new ArrayListValuedHashMap<>();

        // Collect only the active subAtlases based on newPrimaryAtlas mapping
        for (String category : atlas.getPrimaryAtlas().keySet()) {
            String activeSubAtlasName = atlas.getPrimaryAtlas().get(category);
            Collection<MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getSubAtlases().get(category);

            for (MultiValuedMap<String, Map<Texture, Rectangle>> subAtlasCollection : subAtlasCollections) {
                if (subAtlasCollection.containsKey(activeSubAtlasName)) {
                    // Add only the active subAtlas to our packing collection
                    activeSubAtlases.putAll(activeSubAtlasName, subAtlasCollection.get(activeSubAtlasName));
                }
            }
        }

        // Now pack only the active subAtlases
        Rectangle packedSize = MathTools.rectanglePacker2D(activeSubAtlases);

        // Set the final atlas dimensions
        atlas.setWidth(packedSize.width);
        atlas.setHeight(packedSize.height);

        // No need to translate by packedSize.x and packedSize.y as the rectanglePacker2D
        // already positions the rectangles correctly
    }
    //TODO [0] had to use AI to generate new methods, not certain if they work.
    public static void calculateAtlasAllSubPlacements(TextureAtlas atlas) {
        // Process each subAtlas separately
        for (String category : atlas.getSubAtlases().keySet()) {
            Collection<MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getSubAtlases().get(category);

            for (MultiValuedMap<String, Map<Texture, Rectangle>> subAtlasCollection : subAtlasCollections) {
                for (String subAtlasName : subAtlasCollection.keySet()) {
                    // Create a temporary MultiValuedMap containing only this subAtlas
                    MultiValuedMap<String, Map<Texture, Rectangle>> singleSubAtlas = new ArrayListValuedHashMap<>();
                    singleSubAtlas.putAll(subAtlasName, subAtlasCollection.get(subAtlasName));

                    // Pack just this subAtlas
                    Rectangle packedSize = MathTools.rectanglePacker2D(singleSubAtlas);

                    // Store the computed size
                    atlas.getSubAtlasSizes().put(subAtlasName, packedSize);
                }
            }
        }
    }


    public void calculateAtlasSubPlacement(TextureAtlas atlas, String category, String subAtlasName) {
        if (!atlas.getSubAtlases().containsKey(category)) {
            System.err.println("Category '" + category + "' not found.");
            return;
        }

        // Retrieve all sub-atlas collections for the given category
        Collection<MultiValuedMap<String, Map<Texture, Rectangle>>> subAtlasCollections = atlas.getSubAtlases().get(category);

        for (MultiValuedMap<String, Map<Texture, Rectangle>> subAtlasEntry : subAtlasCollections) {
            if (subAtlasEntry.containsKey(subAtlasName)) {
                // Directly compute packed size using rectanglePacker2D
                Rectangle packedSize = MathTools.rectanglePacker2D(subAtlasEntry);

                // Store computed packed size
                atlas.getSubAtlasSizes().put(subAtlasName, packedSize);
                return;
            }
        }

        System.err.println("SubAtlas '" + subAtlasName + "' not found in category '" + category + "'.");
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
    public static void finalizeAtlasMaps(TextureAtlas atlas) {
        calculateAtlasAllSubPlacements(atlas);
        calculateAtlasPrimaryPlacement(atlas);
    }
    public static float[] getUV(TextureAtlas atlas, Texture texture) {
        Rectangle rect = atlas.getTexturePositions().get(texture);
        if (rect == null) return null; // Texture not found

        return new float[]{
                (float) rect.x / atlas.getWidth(),
                (float) rect.y / atlas.getHeight(),
                (float) (rect.x + rect.width) / atlas.getWidth(),
                (float) (rect.y + rect.height) / atlas.getHeight()
        };
    }
}
