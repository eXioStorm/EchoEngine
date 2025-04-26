package com.github.exiostorm.graphics;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL30.*;

import static com.github.exiostorm.main.EchoGame.gamePanel;

//TODO [0] 2025-04-14 make our scene buffer retrievable somehow?
public class BatchRenderer {
    //TODO [0] not fond of a hard-coded imposed limit here... definitely needs re-written
    private static final int MAX_QUADS = 1000;
    private static final int VERTEX_SIZE = 9; // x, y, z, u, v, r, g, b, a
    private static final int VERTEX_COUNT = MAX_QUADS * 4;
    private static final int INDEX_COUNT = MAX_QUADS * 6;

    private FloatBuffer vertexBuffer;
    private IntBuffer indexBuffer;
    private int vaoID, vboID, eboID;

    //private TextureAtlas atlas;
    private Shader defaultShader;
    private List<Quad> quads = new ArrayList<>();
    // Counter for automatic ordering
    private int zPositionNow = 0;
    private int previousTexSlot = GL_TEXTURE_2D;

    private FrameBuffer fboUsedPreviously = null;
    private boolean fboUsed = false;

    public BatchRenderer() {
        //this.defaultShader = defaultShader;
        setupBuffers();
    }
    private void setupBuffers() {
        vaoID = glGenVertexArrays();
        vboID = glGenBuffers();
        eboID = glGenBuffers();

        glBindVertexArray(vaoID);

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, VERTEX_COUNT * VERTEX_SIZE * Float.BYTES, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        int[] indices = new int[INDEX_COUNT];
        int offset = 0;
        for (int i = 0; i < INDEX_COUNT; i += 6) {
            indices[i] = offset;
            indices[i + 1] = offset + 1;
            indices[i + 2] = offset + 2;
            indices[i + 3] = offset + 2;
            indices[i + 4] = offset + 3;
            indices[i + 5] = offset;
            offset += 4;
        }
        //System.out.println("Index buffer content: " + Arrays.toString(indices));
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 0); // Position
        glVertexAttribPointer(1, 2, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 3 * Float.BYTES); // UV
        glVertexAttribPointer(2, 4, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 5 * Float.BYTES); // Color
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);


        //glBindBuffer(GL_ARRAY_BUFFER, 0);  // Unbind VBO (optional cleanup)
        glBindVertexArray(0);
    }
    public void begin(FrameBuffer fbo) {
        if (fbo != null) {
            fboUsed = true;
            fboUsedPreviously = fbo;
            fboUsedPreviously.bind();
        }
        begin();
    }
    public void begin() {
        quads.clear();
        zPositionNow = 0;
    }
    public void draw(int textureID, int textureSlot, int width, int height, float[] uv, float x, float y, float z, Shader shader, Material material,
                     float rotation, float scaleX, float scaleY, boolean flipX, boolean flipY) {
        // Use defaults if not provided
        Shader shaderToUse = (shader != null) ? shader : defaultShader;
        int texSlotUsed = (textureSlot != -1) ? textureSlot : previousTexSlot;
        Material materialToUse = (material != null) ? material : ShaderManager.getDefaultMaterial();

        // If no z provided, use automatic incrementing value
        float zPosition = (z != -1.0f) ? z : zPositionNow++;

        //float[] uv = AtlasManager.getUV(atlas, texture);
        Quad quad = new Quad(x, y, z, width, height, uv, shaderToUse, materialToUse, textureID, texSlotUsed);
        //TODO huh? removing the next line seems to have no effect? OH we had logic for if z was null... lame
        quad.z = zPosition;

        // Apply transformations
        quad.rotation = rotation;
        quad.scaleX = scaleX;
        quad.scaleY = scaleY;
        quad.flipX = flipX;
        quad.flipY = flipY;

        quads.add(quad);
    }
    public void draw(FrameBuffer fbo, float x, float y, float z, Shader shader, Material material) {
        float[] uv = {0,0,1,1};
        draw(fbo.getTextureID(), fbo.getTextureSlot(), fbo.getWidth(), fbo.getHeight(), uv, x, y, z, shader, material, 0.0f, 1.0f, 1.0f, false, false);
    }
    public void draw(Texture texture, TextureAtlas atlas, float x, float y, float z, Shader shader, Material material) {
        //TODO don't know why, but the value to completely flip upside down is 9.424f when rotating
        // value actually 3.1399548 for flip, and 6.2827272 for full 360.
        draw(atlas.getAtlasID(), atlas.getAtlasSlot(), texture.getWidth(), texture.getHeight(), AtlasManager.getUV(atlas, texture), x, y, z, shader, material, 0.0f, 1.0f, 1.0f, false, false);
    }
    public void draw(Texture texture, float x, float y, float z, Shader shader, Material material) {
        draw(gamePanel.getAtlas().getAtlasID(), gamePanel.getAtlas().getAtlasSlot(), texture.getWidth(), texture.getHeight(), AtlasManager.getUV(gamePanel.getAtlas(), texture), x, y, z, shader, material, 0.0f, 1.0f, 1.0f, false, false);
    }

    public void end() {
        if (fboUsed) {
            fboUsed = false;
            fboUsedPreviously.unbind();
        }
        renderBatch();
    }
    private void renderBatch() {
        if (quads.isEmpty()) return;

        // Standard GL setup
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        //TODO possible option here for multiple atlases, GL_TEXTURE# has a range from 0~31 which is more than plenty to have different atlases for things of different size.
        //glActiveTexture(GL_TEXTURE_2D);
        //TODO [0] because our FBO needs to also use the texture slot and be bound with it, we need to move this part of our logic away.
        // we'll also need to make significant changes to support having multiple atlases for like text rendering & reduced texture sizes...
        // this might get complicated...
        //gamePanel.getAtlas().bind();
        glBindVertexArray(vaoID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);

        //TODO [0] need to research if this works alright for both 2d/3d
        // Sort ALL quads by order index first
        quads.sort(Comparator.comparing(q -> q.z));

        Shader currentShader = null;
        Material currentMaterial = null;
        int currentTextureID = -1;
        int currentTextureSlot = -1;
        List<Quad> currentBatch = new ArrayList<>();

        // Process quads in order
        for (Quad quad : quads) {
            // If shader or material changes, we need to flush the current batch
            boolean needFlush =
                    (currentShader != null && currentShader != quad.shader) ||
                            (currentTextureID != -1 && currentTextureID != quad.textureID) ||
                            (currentMaterial != null && currentMaterial != quad.shaderMaterial) ||
                            (currentTextureSlot != -1 && currentTextureSlot != quad.textureSlot);
            if (needFlush && !currentBatch.isEmpty()) {
                // Render current batch
                renderQuadBatch(currentShader, currentMaterial, currentTextureID, currentTextureSlot, currentBatch);
                currentBatch.clear();
            }

            // Update current shader/material
            currentShader = quad.shader;
            currentMaterial = quad.shaderMaterial;
            currentTextureID = quad.textureID;
            currentTextureSlot = quad.textureSlot;
            currentBatch.add(quad);
        }

        // Render final batch if exists
        if (!currentBatch.isEmpty()) {
            renderQuadBatch(currentShader, currentMaterial, currentTextureID, currentTextureSlot, currentBatch);
        }

        // Cleanup
        //gamePanel.getAtlas().unbind();
        glBindVertexArray(0);
        glDisable(GL_BLEND);
    }

    public void checkShaderStatus(Shader shader) {
        int[] linkStatus = new int[1];
        glGetProgramiv(shader.getID(), GL_LINK_STATUS, linkStatus);
        if (linkStatus[0] == GL_FALSE) {
            String log = glGetProgramInfoLog(shader.getID());
            System.err.println("Shader Program Link Error: " + log);
        } else {
            System.out.println(shader.getID() + " : " + linkStatus[0]);
        }
    }

    private void renderQuadBatch(Shader shader, Material material, int textureID, int textureSlot, List<Quad> quadBatch) {
        shader.enable();
        shader.setUniform("textureSampler", 0);
        material.applyUniforms(shader);

        //TODO [!] need to update things so we can select the texture slot (to allow us to have multiple atlases in memory at a time)
        // Bind the texture
        glActiveTexture(textureSlot);
        glBindTexture(GL_TEXTURE_2D, textureID);

        FloatBuffer data = BufferUtils.createFloatBuffer(quadBatch.size() * 4 * VERTEX_SIZE);
        for (Quad quad : quadBatch) {
            quad.fillBuffer(data, gamePanel.WIDTH, gamePanel.HEIGHT);
        }
        data.flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, data);
        glDrawElements(GL_TRIANGLES, quadBatch.size() * 6, GL_UNSIGNED_INT, 0);

        glBindTexture(textureSlot, 0);
        shader.disable();
    }
}