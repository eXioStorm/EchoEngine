package com.github.exiostorm.graphics.gui;

import com.github.exiostorm.graphics.Material;
import com.github.exiostorm.graphics.Shader;
import com.github.exiostorm.graphics.Texture;

import java.util.List;
import java.util.function.Consumer;

import static com.github.exiostorm.main.EchoGame.gamePanel;

public class Cursor {
    private List<Texture> textures;
    Material shaderMaterial;
    public int currentFrame;
    private float minFrameTime;
    private float frameTime;
    private float accumulator;
    private boolean isBehindSchedule;

    public Cursor(List<Texture> textures, float frameTime) {
        this.textures = textures;
        this.currentFrame = 0;
        this.minFrameTime = 0.002f;
        this.accumulator = 0.0005f;
        this.frameTime = frameTime;
    }

    public void update(float deltaTime) {
        if (textures.isEmpty()) return;

        // Cap extremely large delta times to prevent huge jumps
        float cappedDelta = Math.min(deltaTime, frameTime * 2);

        accumulator += cappedDelta;

        // Check if we're behind schedule
        isBehindSchedule = accumulator > frameTime * 1.5f;

        // Only advance if we've reached at least the minimum frame time
        if (accumulator >= minFrameTime) {
            // Only advance one frame at a time for smoother animations
            currentFrame = (currentFrame + 1) % textures.size();

            // If we're behind schedule, reduce accumulator by just the minimum
            // to catch up faster, otherwise use the standard frameTime
            if (isBehindSchedule) {
                accumulator -= minFrameTime;
            } else {
                accumulator -= frameTime;
            }
        }
    }

    public void render(float x, float y) {
        gamePanel.getRenderer().draw(textures.get(currentFrame), gamePanel.getAtlas(), x, y, 1.0f, gamePanel.getShader(), shaderMaterial);
    }
    public void setShaderMaterial(Material material) {
        this.shaderMaterial = material;
    }
}