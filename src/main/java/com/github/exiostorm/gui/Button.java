package com.github.exiostorm.gui;

import com.github.exiostorm.renderer.BatchRenderer;
import com.github.exiostorm.renderer.Shader;
import com.github.exiostorm.renderer.Texture;
import com.github.exiostorm.renderer.TextureAtlasOld;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

import java.util.List;
import java.util.function.Consumer;

public class Button extends GUIElement {
    BatchRenderer renderer;
    TextureAtlasOld atlas;
    @Setter
    boolean useShader = false;
    @Setter
    private Shader shader = null;
    @Getter
    private Texture texture;
    private static final float EPSILON = 1e-6f;
    @Getter
    private double[] mousePosition = {0,0};
    @Getter
    private boolean hovered = false;
    @Getter
    private boolean clicked = false;
    // Set hover action
    @Setter
    private Consumer<Button> onHoverAction;
    // Set hover action
    @Setter
    private Consumer<Button> unHoverAction;
    // Set click action
    @Setter
    private Consumer<Button> onClickAction;
    @Setter
    private Consumer<Button> onDragAction;

    public Button(float x, float y, Texture texture) {
        super(x, y);
        //TODO change code here? "new Texture(path)", will need texture to store original file path?
        //this.texture = new Texture(texture.getPath());
        this.texture = texture;
        //TODO these are just temporary while I try to figure out where I'm at
        //List<Texture> textures = List.of(this.texture);
        atlas = new TextureAtlasOld();
        renderer = new BatchRenderer(atlas, shader);
    }

    @Override
    public void update() {

    }

    @Override
    public void render() {
        if (!visible) return;
        //TODO might be an issue here because of other class dependency... probably no other way.
        //texture.drawImmediate(x, y, texture.getWidth(), texture.getHeight(), this.useShader);
        renderer.draw(texture, x, y, shader, this.useShader);
    }


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
        return texture.getTransparencyMap(true)[pixelIndex];
    }

    private boolean isPointInPolygon(float x, float y, List<Vector2f> vertices) {
        int intersectCount = 0;
        for (int i = 0; i < vertices.size(); i++) {
            Vector2f v1 = vertices.get(i);
            Vector2f v2 = vertices.get((i + 1) % vertices.size());
            if (rayIntersectsSegment(x, y, v1, v2)) {
                intersectCount++;
            }
        }
        return intersectCount % 2 == 1; // Odd = inside, even = outside
    }

    private boolean rayIntersectsSegment(float x, float y, Vector2f v1, Vector2f v2) {
        if (v1.y > v2.y) {
            Vector2f temp = v1;
            v1 = v2;
            v2 = temp;
        }
        if (Math.abs(y - v1.y) < EPSILON || Math.abs(y - v2.y) < EPSILON) {
            y += EPSILON; // Avoid edge cases
        }

        if (y < v1.y || y > v2.y || x > Math.max(v1.x, v2.x)) return false;

        if (x < Math.min(v1.x, v2.x)) return true;

        float slope = (v2.x - v1.x) / (v2.y - v1.y);
        float intersectionX = v1.x + (y - v1.y) * slope;
        return x < intersectionX;
    }



    // Trigger hover action
    public void triggerHoverAction() {
        if (onHoverAction != null) {
            onHoverAction.accept(this);
        }
        hovered = true;
    }
    // Trigger hover action
    public void stopHoverAction() {
        if (unHoverAction != null) {
            unHoverAction.accept(this);
        }
        hovered = false;
    }

    // Trigger click action
    public void triggerClickAction() {
        clicked = true;
        if (onClickAction != null) {
            onClickAction.accept(this);
        }
    }
}