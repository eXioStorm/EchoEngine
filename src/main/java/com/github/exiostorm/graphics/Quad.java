package com.github.exiostorm.graphics;

import java.nio.FloatBuffer;

class Quad {
    float x, y, width, height;
    float[] uv;
    Shader shader;
    boolean useShader;

    public Quad(float x, float y, float width, float height, float[] uv, Shader shader, boolean useShader) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.uv = uv;
        this.shader = shader;
        this.useShader = useShader;
    }
    public void fillBuffer(FloatBuffer buffer, int screenWidth, int screenHeight) {
        float xNDC = (2.0f * x / screenWidth) - 1.0f;
        float yNDC = 1.0f - (2.0f * y / screenHeight);
        float widthNDC = (2.0f * width / screenWidth);
        float heightNDC = (2.0f * height / screenHeight);

        float[] vertices = {
                // Vertex 1 (Top-left)
                xNDC, yNDC, 0.0f,
                uv[0], uv[1],
                1.0f, 1.0f, 1.0f, 1.0f,

                // Vertex 2 (Top-right)
                xNDC + widthNDC, yNDC, 0.0f,
                uv[2], uv[1],
                1.0f, 1.0f, 1.0f, 1.0f,

                // Vertex 3 (Bottom-right)
                xNDC + widthNDC, yNDC - heightNDC, 0.0f,
                uv[2], uv[3],
                1.0f, 1.0f, 1.0f, 1.0f,

                // Vertex 4 (Bottom-left)
                xNDC, yNDC - heightNDC, 0.0f,
                uv[0], uv[3],
                1.0f, 1.0f, 1.0f, 1.0f,
        };

        buffer.put(vertices);
    }


    public int addToVertexBuffer(float[] buffer, int index) {
        buffer[index++] = x;           buffer[index++] = y;           buffer[index++] = uv[0]; buffer[index++] = uv[1];
        buffer[index++] = x + width;   buffer[index++] = y;           buffer[index++] = uv[2]; buffer[index++] = uv[1];
        buffer[index++] = x + width;   buffer[index++] = y + height;  buffer[index++] = uv[2]; buffer[index++] = uv[3];
        buffer[index++] = x;           buffer[index++] = y + height;  buffer[index++] = uv[0]; buffer[index++] = uv[3];
        return index;
    }

    public int addToIndexBuffer(int[] buffer, int index) {
        int offset = index / 6 * 4; // 4 vertices per quad
        buffer[index++] = offset;       buffer[index++] = offset + 1; buffer[index++] = offset + 2;
        buffer[index++] = offset + 2;   buffer[index++] = offset + 3; buffer[index++] = offset;
        return index;
    }
}