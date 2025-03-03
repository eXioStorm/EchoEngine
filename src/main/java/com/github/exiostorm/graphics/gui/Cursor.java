package com.github.exiostorm.graphics.gui;

import com.github.exiostorm.graphics.Texture;

public class Cursor {
    private Texture texture;
    private int frameWidth;
    private int frameHeight;
    private int frameCount;
    private float currentFrame;
    private float frameRate;

    public Cursor(Texture texture, int frameWidth, int frameHeight, int frameCount, float frameRate) {
        this.texture = texture;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;
        this.frameRate = frameRate;
    }

    public void update(float deltaTime) {
        currentFrame += deltaTime * frameRate;
        if (currentFrame >= frameCount) {
            currentFrame -= frameCount;
        }
    }

    public void render(float x, float y) {
        /*
        int frameIndex = (int) currentFrame;
        float textureX = (frameIndex * frameWidth) / (float) texture.getWidth();
        float textureY = 0f;
        float textureWidth = frameWidth / (float) texture.getWidth();
        float textureHeight = frameHeight / (float) texture.getHeight();

        texture.bind();
        glBegin(GL_QUADS);
        glTexCoord2f(textureX, textureY); glVertex2f(x, y);
        glTexCoord2f(textureX + textureWidth, textureY); glVertex2f(x + frameWidth, y);
        glTexCoord2f(textureX + textureWidth, textureY + textureHeight); glVertex2f(x + frameWidth, y + frameHeight);
        glTexCoord2f(textureX, textureY + textureHeight); glVertex2f(x, y + frameHeight);
        glEnd();
        texture.unbind();
        */
    }
}