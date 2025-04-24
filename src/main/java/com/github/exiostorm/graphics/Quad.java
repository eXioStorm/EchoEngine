package com.github.exiostorm.graphics;

import java.nio.FloatBuffer;

class Quad {
    float x, y, z, width, height;
    float[] uv;
    Shader shader;
    Material shaderMaterial;
    int textureID;
    int textureSlot;

    public float rotation = 0.0f;    // Rotation in radians
    public float scaleX = 1.0f;      // Scale factor for X
    public float scaleY = 1.0f;      // Scale factor for Y
    public boolean flipX = false;    // Horizontal flip
    public boolean flipY = false;    // Vertical flip

    public Quad(float x, float y, float z, float width, float height, float[] uv, Shader shader, Material material, int textureID, int textureSlot) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = width;
        this.height = height;
        this.uv = uv;
        this.shader = shader;
        this.shaderMaterial = material;
        this.textureID = textureID;
        this.textureSlot = textureSlot;
        // Default transformations
        this.rotation = 0.0f;
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        this.flipX = false;
        this.flipY = false;
    }
    // Update fillBuffer to apply transformations
    public void fillBuffer(FloatBuffer buffer, int screenWidth, int screenHeight) {
        // Calculate center for rotation
        float centerX = x + width / 2;
        float centerY = y + height / 2;

        // Apply scale (potentially from center)
        float halfWidth = (width * scaleX) / 2;
        float halfHeight = (height * scaleY) / 2;

        // Calculate corner positions relative to center
        float[] xCorners = {-halfWidth, halfWidth, halfWidth, -halfWidth};
        float[] yCorners = {-halfHeight, -halfHeight, halfHeight, halfHeight};

        // Apply flipping if needed
        if (flipX) {
            xCorners[0] = halfWidth;
            xCorners[1] = -halfWidth;
            xCorners[2] = -halfWidth;
            xCorners[3] = halfWidth;
        }

        if (flipY) {
            yCorners[0] = halfHeight;
            yCorners[1] = halfHeight;
            yCorners[2] = -halfHeight;
            yCorners[3] = -halfHeight;
        }

        // Apply rotation
        if (rotation != 0) {
            float cos = (float) Math.cos(rotation);
            float sin = (float) Math.sin(rotation);

            for (int i = 0; i < 4; i++) {
                float rotatedX = xCorners[i] * cos - yCorners[i] * sin;
                float rotatedY = xCorners[i] * sin + yCorners[i] * cos;
                xCorners[i] = rotatedX;
                yCorners[i] = rotatedY;
            }
        }

        // Final positions (translated back to world space)
        float[] finalX = new float[4];
        float[] finalY = new float[4];

        for (int i = 0; i < 4; i++) {
            finalX[i] = centerX + xCorners[i];
            finalY[i] = centerY + yCorners[i];

            // Convert to normalized device coordinates
            finalX[i] = (finalX[i] / screenWidth) * 2 - 1;
            finalY[i] = 1 - (finalY[i] / screenHeight) * 2;
        }

        // Add vertices to buffer
        // Bottom-left
        buffer.put(finalX[0]).put(finalY[0]).put(z);  // Use the object's z
        buffer.put(uv[0]).put(uv[1]);
        buffer.put(1.0f).put(1.0f).put(1.0f).put(1.0f);
        // RGBA ^
        // Bottom-right
        buffer.put(finalX[1]).put(finalY[1]).put(z);  // Use the object's z
        buffer.put(uv[2]).put(uv[1]);
        buffer.put(1.0f).put(1.0f).put(1.0f).put(1.0f);

        // Top-right
        buffer.put(finalX[2]).put(finalY[2]).put(z);  // Use the object's z
        buffer.put(uv[2]).put(uv[3]);
        buffer.put(1.0f).put(1.0f).put(1.0f).put(1.0f);

        // Top-left
        buffer.put(finalX[3]).put(finalY[3]).put(z);  // Use the object's z
        buffer.put(uv[0]).put(uv[3]);
        buffer.put(1.0f).put(1.0f).put(1.0f).put(1.0f);
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