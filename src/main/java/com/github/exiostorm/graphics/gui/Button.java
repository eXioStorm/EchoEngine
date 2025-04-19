package com.github.exiostorm.graphics.gui;

import com.github.exiostorm.graphics.*;
import com.github.exiostorm.main.EchoGame;
import com.github.exiostorm.main.GamePanel;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.github.exiostorm.main.EchoGame.gamePanel;

public class Button extends GUIElement {
    BatchRenderer renderer = gamePanel.getRenderer();
    Shader shader = gamePanel.getShader();
    TextureAtlas atlas = gamePanel.getAtlas();

    Material shaderMaterial;
    private Texture texture;
    public boolean hovered = false;
    public boolean clicked = false;
    // Set hover action
    private Consumer<Button> onHoverAction;
    // Set hover action
    private Consumer<Button> unHoverAction;
    // Set click action
    private Consumer<Button> onClickAction;
    private Consumer<Button> onDragAction;

    public Button(float x, float y, Texture texture) {
        super(x, y);
        this.texture = texture;
    }

    @Override
    public void update() {

    }

    @Override
    public void render() {
        if (!visible) return;
        renderer.draw(texture, x, y, 1, shader, shaderMaterial);
    }

    //TODO [0] our transformation logic will conflict with our button logic since nothing else gets transformed besides what's rendered.
    // will need to change logic to account for transformations.
    @Override
    public boolean isMouseOver(float mouseX, float mouseY) {
        // Adjust mouse coordinates relative to the button's position
        float localMouseX = mouseX - x;
        float localMouseY = mouseY - y;

        // Ensure the mouse is within the bounds of the button
        if (localMouseX < 0 || localMouseX >= texture.getWidth() ||
                localMouseY < 0 || localMouseY >= texture.getHeight()) {
            return false;
        }

        // Map mouse coordinates to pixel coordinates in the original texture
        int pixelX = (int) localMouseX;
        int pixelY = (int) localMouseY;

        // Get the pixel index in the 1D array
        int pixelIndex = pixelY * texture.getWidth() + pixelX;

        // Consider the pixel under the mouse as valid if alpha > 0 (non-transparent)
        return !texture.getTransparencyMap(true)[pixelIndex];
    }

    // Trigger hover action
    public void triggerHoverAction() {
        if (onHoverAction != null) {
            onHoverAction.accept(this);
        }
        this.hovered = true;
    }
    // Trigger hover action
    public void stopHoverAction() {
        if (unHoverAction != null) {
            unHoverAction.accept(this);
        }
        this.hovered = false;
    }

    // Trigger click action
    public void triggerClickAction() {
        if (onClickAction != null) {
            onClickAction.accept(this);
        }
        this.clicked = true;
    }
    public Texture getTexture() {
        return texture;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public boolean isClicked() {
        return this.clicked;
    }
    public void setShaderMaterial(Material material) {
        this.shaderMaterial = material;
    }

    public void setShader(Shader shader) {
        this.shader = shader;
    }

    public void setOnHoverAction(Consumer<Button> onHoverAction) {
        this.onHoverAction = onHoverAction;
    }

    public void setUnHoverAction(Consumer<Button> unHoverAction) {
        this.unHoverAction = unHoverAction;
    }

    public void setOnClickAction(Consumer<Button> onClickAction) {
        this.onClickAction = onClickAction;
    }

    public void setOnDragAction(Consumer<Button> onDragAction) {
        this.onDragAction = onDragAction;
    }
}