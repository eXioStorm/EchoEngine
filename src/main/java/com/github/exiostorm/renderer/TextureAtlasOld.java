package com.github.exiostorm.renderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;

import static com.github.exiostorm.renderer.TextureGenerator.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.stbi_image_free;

public class TextureAtlasOld {
    private static int MAX_ATLAS_WIDTH = 0;
    private static int MAX_ATLAS_HEIGHT = 0;

    private final Map<Texture, UsageInfo> usageMap = new LinkedHashMap<>();
    private final List<Rectangle> freeSpaces = new ArrayList<>();
    private ByteBuffer atlasData;
    private int textureID;
    private int atlasWidth, atlasHeight;
    private final List<Texture> pendingTextures = new ArrayList<>(); // Textures to be packed
    private boolean finalized = false; // Flag to ensure finalizeAtlas() is called only once
    private boolean needsUpload = false; // Indicates if the atlas needs a GPU upload

    //TODO VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
    // Getting ready to implement atlas management setup?
    // We need to make it so we can read / write our atlas configuration to/from a file, then we can at least move forward even if our packing algorithm isn't perfect and continue making progress.
    // We also need something setup to hot reload textures, I think even minecraft lets you refresh textures.
    private Map<String, Map<String, Rectangle>> primaryAtlas; // Category -> Active SubAtlas (Texture Name -> Placement)
    private Map<String, Map<String, Map<String, Rectangle>>> subAtlases; // Category -> (SubAtlas Name -> (Texture Name -> Placement))
    //TODO ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

    public TextureAtlasOld() {
        atlasData = ByteBuffer.allocateDirect(MAX_ATLAS_WIDTH * MAX_ATLAS_HEIGHT * 4)
                .order(ByteOrder.nativeOrder());
        atlasWidth = 0;
        atlasHeight = 0;
        textureID = glGenTextures();
    }

    private static class UsageInfo {
        int offsetX, offsetY;
        int lastAccessFrame;
        boolean inMemory;

        public UsageInfo(int offsetX, int offsetY, int lastAccessFrame, boolean inMemory) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.lastAccessFrame = lastAccessFrame;
            this.inMemory = inMemory;
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void expandAtlas(int requiredWidth, int requiredHeight) {
        // Calculate the new dimensions proportionally
        int newWidth = Math.max(MAX_ATLAS_WIDTH, requiredWidth);
        int newHeight = Math.max(MAX_ATLAS_HEIGHT, requiredHeight);

        if (newWidth == MAX_ATLAS_WIDTH && newHeight == MAX_ATLAS_HEIGHT) {
            throw new RuntimeException("Unable to expand atlas: required dimensions exceed limits.");
        }

        System.out.println("Expanding atlas to: " + newWidth + "x" + newHeight);

        // Allocate a new buffer for the expanded atlas
        ByteBuffer newAtlasData = ByteBuffer.allocateDirect(newWidth * newHeight * 4)
                .order(ByteOrder.nativeOrder());

        // Copy existing atlas data row by row
        for (int y = 0; y < atlasHeight; y++) {
            int oldRowStart = y * MAX_ATLAS_WIDTH * 4;
            int newRowStart = y * newWidth * 4;

            for (int x = 0; x < atlasWidth * 4; x++) {
                newAtlasData.put(newRowStart + x, atlasData.get(oldRowStart + x));
            }
        }

        // Replace the old atlas with the new one
        atlasData = newAtlasData;
        MAX_ATLAS_WIDTH = newWidth;
        MAX_ATLAS_HEIGHT = newHeight;

        // Update the freeSpaces list to include the newly expanded areas
        if (newWidth > atlasWidth) {
            // Add new free space to the right of the current content
            freeSpaces.add(new Rectangle(atlasWidth, 0, newWidth - atlasWidth, atlasHeight));
        }
        if (newHeight > atlasHeight) {
            // Add new free space below the current content
            freeSpaces.add(new Rectangle(0, atlasHeight, newWidth, newHeight - atlasHeight));
        }

        System.out.println("Atlas successfully expanded.");
    }

    //TODO don't know about this "currentFrame" usage, we do want to track when created, and when used.. so when our BatchRenderer calls draw with a texture we need to update lastAccessFrame
    public void addTexture(Texture texture, int currentFrame, byte saveFlags) {
        if (usageMap.containsKey(texture)) {
            // Update usage info if the texture is already in the atlas
            UsageInfo info = usageMap.get(texture);
            info.lastAccessFrame = currentFrame;
            return;
        }

        //TODO issue here, pendingTextures will get cleared when we finalize the atlas.. we need to track which textures are in our atlas so we don't lose already added textures.
        // Store the texture for later packing
        pendingTextures.add(texture);

        // Optionally store additional data (transparency map, etc.)
        if ((saveFlags & 1) != 0) {
            generateTransparencyMap(texture, true);
        }
        if ((saveFlags & 2) != 0) {
            generateBufferedImage(texture, true);
        }
    }

    //TODO big latency issue with running this method, we need some way to use our old atlas while waiting for updates so that our 300ms+ latency isn't noticeable.
    public void finalizeAtlas() {
        if (finalized) {
            throw new IllegalStateException("Atlas has already been finalized!");
        }

        finalized = true;

        // Sort textures by size (largest to smallest) to optimize packing
        pendingTextures.sort(Comparator.comparingInt((Texture t) -> t.getWidth() * t.getHeight()).reversed());

        // Initialize atlas dimensions
        atlasWidth = 0;
        atlasHeight = 0;
        //TODO VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
        //TODO VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
        //TODO VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
        // This is where we need our bin packing logic.
        // Try to find a free space for the texture
        for (Texture texture : pendingTextures) {
            int startX = -1, startY = -1;
            for (Rectangle freeSpace : freeSpaces) {
                if (freeSpace.width >= texture.getWidth() && freeSpace.height >= texture.getHeight()) {
                    //TODO should put our logic to write data here somewhere as we only have this execution area if we've found free space to write to.
                    startX = freeSpace.x;
                    startY = freeSpace.y;
                    // Update free space
                    freeSpaces.remove(freeSpace);
                    usageMap.put(texture, new UsageInfo(startX, startY, 0, false));//remember to set this to true when we draw
                    if (freeSpace.width > texture.getWidth()) {
                        freeSpaces.add(new Rectangle(freeSpace.x + texture.getWidth(), freeSpace.y, freeSpace.width - texture.getWidth(), freeSpace.height));
                    }
                    if (freeSpace.height > texture.getHeight()) {
                        freeSpaces.add(new Rectangle(freeSpace.x, freeSpace.y + texture.getHeight(), texture.getWidth(), freeSpace.height - texture.getHeight()));
                    }
                    break;
                }
            }
            // If no free space is found, expand the atlas
            if (startX == -1 || startY == -1) {
                //TODO changed this to expand the atlas by 4x the texture dimensions to reduce how often we need this method
                // maybe think of something better than an arbitrary value of 4... could we perhaps save our largest width / height of any texture on the atlas? no..
                int newRequiredWidth = Math.max(atlasWidth + (texture.getWidth()*4), MAX_ATLAS_WIDTH);
                int newRequiredHeight = Math.max(atlasHeight + (texture.getHeight()*4), MAX_ATLAS_HEIGHT);

                expandAtlas(newRequiredWidth, newRequiredHeight);

                startX = atlasWidth;
                startY = atlasHeight;
                atlasWidth += texture.getWidth();
                atlasHeight = Math.max(atlasHeight, startY + texture.getHeight());//uhm
            }
            //TODO we need code to assign our designated spaces before we leave this loop for our pendingTextures
            // to do that I think we need to assign the coordinates in the above loop, then down below we implement those coordinates.
            // example : usageMap.put(texture, new UsageInfo(startX, startY, 0, false)); note : inMemory is used for managing unused textures, can set it true or false here.
            // but if you set it false you'll need to set it to true after we draw it.
        }
        //TODO ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //TODO ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //TODO ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        // Reset the atlas data buffer
        //TODO fucking GPT, we don't want to set our buffer size until our atlas dimensions have been settled.
        // edit : code above should settle our atlas dimensions
        //TODO VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV
        atlasData = ByteBuffer.allocateDirect(MAX_ATLAS_WIDTH * MAX_ATLAS_HEIGHT * 4).order(ByteOrder.nativeOrder());
        // Pack each pending texture into the atlas
        for (Texture texture : pendingTextures) {
            System.out.println("Processing texture: " + texture.getPath());
            //TODO originally we passed an arg for our saveFlags, but ChatGpt migrated our code to a different method that doesn't have that arg. we'll need to re-incorporate it later.
            ByteBuffer textureData = generateByteBuffer(texture, (byte) 0);

            int textureWidth = texture.getWidth();
            int textureHeight = texture.getHeight();

            // Place the texture in the atlas
            //TODO already defined because GPT forgot our damn code and reverted changes, we need to reimplement this logic above where we search our atlas for free space.
            int startX = atlasWidth;
            int startY = atlasHeight;

            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    int atlasIndex = ((startY + y) * MAX_ATLAS_WIDTH + (startX + x)) * 4;
                    int textureIndex = (y * textureWidth + x) * 4;

                    atlasData.put(atlasIndex, textureData.get(textureIndex));       // Red
                    atlasData.put(atlasIndex + 1, textureData.get(textureIndex + 1)); // Green
                    atlasData.put(atlasIndex + 2, textureData.get(textureIndex + 2)); // Blue
                    atlasData.put(atlasIndex + 3, textureData.get(textureIndex + 3)); // Alpha
                }
            }
            // Store texture placement info
            usageMap.put(texture, new UsageInfo(startX, startY, 0, true));
            // Update atlas dimensions
            atlasWidth += textureWidth;
            atlasHeight = Math.max(atlasHeight, startY + textureHeight);
            //TODO this might cause problems? idk... need this to prevent memory leaks
            //stbi_image_free(textureData);
        }
        //TODO ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        // Upload to GPU
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, MAX_ATLAS_WIDTH, MAX_ATLAS_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasData);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glBindTexture(GL_TEXTURE_2D, 0);
        needsUpload = false;
        System.out.println("Atlas uploaded to GPU.");

        // Clear pending textures
        pendingTextures.clear();

        //saveAtlasAsImage("src/main/resources/tests/" + Math.random() + ".png");
        System.out.println("Atlas finalized and uploaded. Dimensions: " + atlasWidth + "x" + atlasHeight);
    }

    private void saveAtlasAsImage(String filePath) {
        // Validate atlas dimensions
        if (atlasWidth <= 0 || atlasHeight <= 0) {
            throw new IllegalArgumentException("Atlas dimensions must be greater than zero. Width: "
                    + atlasWidth + ", Height: " + atlasHeight);
        }

        // Create BufferedImage with the proper atlas dimensions
        BufferedImage atlasImage = new BufferedImage(MAX_ATLAS_WIDTH, MAX_ATLAS_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        atlasData.rewind(); // Ensure the buffer is at the correct position for reading

        // Populate the BufferedImage with atlas data
        for (int y = 0; y < MAX_ATLAS_HEIGHT; y++) {
            for (int x = 0; x < MAX_ATLAS_WIDTH; x++) {
                int index = (y * MAX_ATLAS_WIDTH + x) * 4;
                if (index + 3 >= atlasData.capacity()) {
                    throw new IllegalStateException("Atlas data index out of bounds: " + index);
                }

                int r = atlasData.get(index) & 0xFF;
                int g = atlasData.get(index + 1) & 0xFF;
                int b = atlasData.get(index + 2) & 0xFF;
                int a = atlasData.get(index + 3) & 0xFF;

                int rgba = (a << 24) | (r << 16) | (g << 8) | b;
                atlasImage.setRGB(x, y, rgba);
            }
        }

        // Save the BufferedImage as a PNG file
        try {
            ImageIO.write(atlasImage, "png", new File(filePath));
            System.out.println("Atlas saved as: " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to save atlas image: " + e.getMessage());
            e.printStackTrace();
        }
        atlasData.rewind();
    }


    private void reloadTexture(Texture texture, UsageInfo info) {
        ByteBuffer textureData = generateByteBuffer(texture, (byte) 0);
        atlasData.position((info.offsetX + info.offsetY * MAX_ATLAS_WIDTH) * 4);
        atlasData.put(textureData);

        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexSubImage2D(GL_TEXTURE_2D, 0, info.offsetX, info.offsetY, texture.getWidth(), texture.getHeight(), GL_RGBA, GL_UNSIGNED_BYTE, textureData);
        glBindTexture(GL_TEXTURE_2D, 0);

        info.inMemory = true;
    }

    public float[] getUV(Texture texture) {
        UsageInfo info = usageMap.get(texture);
        if (info == null) {
            throw new RuntimeException("Texture not found in atlas: " + texture.getPath());
        }
        return new float[] {
                (float) info.offsetX / MAX_ATLAS_WIDTH,
                (float) info.offsetY / MAX_ATLAS_HEIGHT,
                (float) (info.offsetX + texture.getWidth()) / MAX_ATLAS_WIDTH,
                (float) (info.offsetY + texture.getHeight()) / MAX_ATLAS_HEIGHT,
        };
    }

}