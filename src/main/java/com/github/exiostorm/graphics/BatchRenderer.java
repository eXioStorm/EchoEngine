package com.github.exiostorm.graphics;

import com.github.exiostorm.main.EchoGame;
import com.github.exiostorm.main.GamePanel;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Consumer;

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

    private TextureAtlas atlas;
    private Shader defaultShader;
    private List<Quad> quads = new ArrayList<>();

    // Group quads first by shader, then by material
    private Map<Shader, Map<Material, List<Quad>>> renderGroups = new HashMap<>();

    public BatchRenderer(TextureAtlas atlas, Shader defaultShader) {
        //TODO for this.atlas = atlas we're creating a reference to the supplied atlas, so no worries about external modifications not being reflected.
        this.atlas = atlas;
        this.defaultShader = defaultShader;
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

        //TODO maybe something is wrong here with our VERTEX_SIZE setup?
        glVertexAttribPointer(0, 3, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 0); // Position
        glVertexAttribPointer(1, 2, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 3 * Float.BYTES); // UV
        glVertexAttribPointer(2, 4, GL_FLOAT, false, VERTEX_SIZE * Float.BYTES, 5 * Float.BYTES); // Color
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);


        //glBindBuffer(GL_ARRAY_BUFFER, 0);  // Unbind VBO (optional cleanup)
        glBindVertexArray(0);
    }
    public void begin() {
        renderGroups.clear();
        //quads.clear();

    }

    public void draw(Texture texture, float x, float y, Shader shader, Material material) {
        // Use defaults if not provided
        Shader shaderToUse = (shader != null) ? shader : defaultShader;
        Material materialToUse = (material != null) ? material : ShaderManager.getDefaultMaterial();

        float[] uv = AtlasManager.getUV(atlas, texture);
        Quad quad = new Quad(x, y, texture.getWidth(), texture.getHeight(), uv, shaderToUse, materialToUse);

        // Get or create the map for this shader
        Map<Material, List<Quad>> materialMap = renderGroups.computeIfAbsent(
                shaderToUse, k -> new HashMap<>());

        // Get or create the list for this material
        List<Quad> quadList = materialMap.computeIfAbsent(
                materialToUse, k -> new ArrayList<>());

        // Add the quad to the list
        quadList.add(quad);
    }
    /*
    //TODO going to have issues here with having multiple texture atlases...
    // we could fix this by adding quads to our atlas class instead.
    public void draw(Texture texture, float x, float y, Shader shader, Material material) {
        //TODO [0] need to figure out new atlas setup how to get converted coordinates for this part here
        float[] uv = AtlasManager.getUV(atlas, texture);
        quads.add(new Quad(x, y, texture.getWidth(), texture.getHeight(), uv, shader, material));
    }*/

    public void end() {
        renderBatch();
    }
    /*
    private void renderBatch() {
        // Set up OpenGL states
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        //TODO possible option here for multiple atlases, GL_TEXTURE# has a range from 0~31 which is more than plenty to have different atlases for things of different size.
        // e.g. backgrounds are really large and would give us massive atlases if we tried to keep them stored, and would cause lag if we constantly load / unload them along our other assets.
        glActiveTexture(GL_TEXTURE0); // Activate texture unit 0

        atlas.bind(); // Bind the texture atlas

        glBindVertexArray(vaoID); // Bind the VAO
        glBindBuffer(GL_ARRAY_BUFFER, vboID); // Bind the VBO

        defaultShader.enable(); // Enable the shader
        defaultShader.setUniform("textureSampler", 0); // Set the texture sampler uniform

        // Check for shader link errors (optional but useful for debugging)
        int[] linkStatus = new int[1];
        glGetProgramiv(defaultShader.getID(), GL_LINK_STATUS, linkStatus);
        if (linkStatus[0] == GL_FALSE) {
            String log = glGetProgramInfoLog(defaultShader.getID());
            System.err.println("Shader Program Link Error: " + log);
        }

        // Populate vertex buffer with quad data
        FloatBuffer data = BufferUtils.createFloatBuffer(quads.size() * 4 * VERTEX_SIZE);
        for (Quad quad : quads) {
            //TODO [0] 2025-04-08
            // We need to batch uniform changes together and include glDrawElements within that batch.
            // research confirmed we should be okay having multiple draw calls.
            // (future idea... maybe we could use a buffer atlas to save recent shader changes and use them in place of changing shader uniforms? though sounds like far too much extra effort...)
            // 2025-04-09 - I think we're going to need a shader manager that saves a List/Map of "Consumer<Shader>"?
            //
            if (quad.shaderMaterial != null) {
                quad.shaderMaterial.applyUniforms(defaultShader);
            } else {
                ShaderManager.getDefaultMaterial().applyUniforms(defaultShader);
            }
            quad.fillBuffer(data, gamePanel.WIDTH, gamePanel.HEIGHT);
        }
        data.flip(); // Prepare the buffer for reading

        // Upload vertex data to the GPU
        glBufferSubData(GL_ARRAY_BUFFER, 0, data);

        // Render the quads
        glDrawElements(GL_TRIANGLES, quads.size() * 6, GL_UNSIGNED_INT, 0);

        // Cleanup
        atlas.unbind();
        defaultShader.disable();
        glBindVertexArray(0);
        glDisable(GL_BLEND);
    }*/
    private void renderBatch() {
        // Standard GL setup
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        glActiveTexture(GL_TEXTURE0);
        atlas.bind();
        glBindVertexArray(vaoID);
        glBindBuffer(GL_ARRAY_BUFFER, vboID);

        // For each shader...
        for (Map.Entry<Shader, Map<Material, List<Quad>>> shaderEntry : renderGroups.entrySet()) {
            Shader shader = shaderEntry.getKey();
            Map<Material, List<Quad>> materialMap = shaderEntry.getValue();

            shader.enable();
            shader.setUniform("textureSampler", 0);

            // Check shader status for debugging
            checkShaderStatus(shader);

            // For each material with this shader...
            for (Map.Entry<Material, List<Quad>> materialEntry : materialMap.entrySet()) {
                Material material = materialEntry.getKey();
                List<Quad> quadList = materialEntry.getValue();

                if (quadList.isEmpty()) continue;

                // Apply material uniforms
                material.applyUniforms(shader);

                // Fill buffer
                FloatBuffer data = BufferUtils.createFloatBuffer(quadList.size() * 4 * VERTEX_SIZE);
                for (Quad quad : quadList) {
                    quad.fillBuffer(data, gamePanel.WIDTH, gamePanel.HEIGHT);
                }
                data.flip();

                // Upload and draw
                glBufferSubData(GL_ARRAY_BUFFER, 0, data);
                glDrawElements(GL_TRIANGLES, quadList.size() * 6, GL_UNSIGNED_INT, 0);
            }

            // Disable shader when done with all materials using it
            shader.disable();
        }

        // Cleanup
        atlas.unbind();
        glBindVertexArray(0);
        glDisable(GL_BLEND);
    }

    private void checkShaderStatus(Shader shader) {
        int[] linkStatus = new int[1];
        glGetProgramiv(shader.getID(), GL_LINK_STATUS, linkStatus);
        if (linkStatus[0] == GL_FALSE) {
            String log = glGetProgramInfoLog(shader.getID());
            System.err.println("Shader Program Link Error: " + log);
        }
    }

}
