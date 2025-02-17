package com.github.exiostorm.renderer;

import lombok.Getter;

import java.util.Arrays;

import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class Mesh {
    @Getter
    private int vaoID, vboID, eboID;
    private int vertexCount;

    public Mesh(float[] initialVertices, int[] initialIndices) {
        // Calculate initial sizes
        int vertexBufferSize = initialVertices.length * Float.BYTES;
        int indexBufferSize = initialIndices.length * Integer.BYTES;

        // Generate and bind VAO
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        // Generate and initialize VBO
        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertexBufferSize, GL_DYNAMIC_DRAW);

        // Generate and initialize EBO
        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferSize, GL_DYNAMIC_DRAW);

        // Attribute pointers
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0); // Position
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES); // Texture Coords
        glEnableVertexAttribArray(1);

        System.out.println("Vertex Buffer Size: " + vertexBufferSize);
        System.out.println("Index Buffer Size: " + indexBufferSize);

        // Unbind VAO
        glBindVertexArray(0);
    }

    public void updateData(float[] vertices, int[] indices) {
        System.out.println("Updating Mesh with Vertex Count: " + vertices.length / 4);
        System.out.println("Updating Mesh with Index Count: " + indices.length);

        glBindVertexArray(vaoID); // Bind VAO

        // Check buffer sizes and reallocate if necessary
        if (vertices.length * Float.BYTES > glGetBufferParameteri(GL_ARRAY_BUFFER, GL_BUFFER_SIZE) ||
                indices.length * Integer.BYTES > glGetBufferParameteri(GL_ELEMENT_ARRAY_BUFFER, GL_BUFFER_SIZE)) {
            System.err.println("Mesh buffers are too small. Reallocating...");
            recreateBuffers(vertices, indices);
        }

        // Update VBO
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        // Update EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices);

        vertexCount = indices.length;

        glBindVertexArray(0); // Unbind VAO
    }

    private void recreateBuffers(float[] vertices, int[] indices) {
        // Recreate VBO
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);

        // Recreate EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
    }
    public void render() {
        int currentVAO = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        System.out.println("Current VAO: " + currentVAO + ", Expected VAO: " + vaoID);

        glBindVertexArray(vaoID);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        checkGLError("glDrawElements");
        glBindVertexArray(0); // Unbind VAO after rendering
    }

    private void checkGLError(String location) {
        int error;
        while ((error = glGetError()) != GL_NO_ERROR) {
            System.err.println("OpenGL Error at " + location + ": " + error);
        }
    }

    public void cleanup() {
        glDeleteBuffers(vboID);
        glDeleteBuffers(eboID);
        glDeleteVertexArrays(vaoID);
    }
}