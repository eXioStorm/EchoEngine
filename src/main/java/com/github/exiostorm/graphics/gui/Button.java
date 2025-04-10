package com.github.exiostorm.graphics.gui;

import com.github.exiostorm.graphics.BatchRenderer;
import com.github.exiostorm.graphics.Shader;
import com.github.exiostorm.graphics.Texture;
import com.github.exiostorm.graphics.TextureAtlas;
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

    Consumer<Shader> shaderModifier;
    private Texture texture;
    private static final float EPSILON = 1e-6f;
    //TODO [0] this is why it always says 0x0 when we click
    private double[] mousePosition = {0,0};
    private boolean hovered = false;
    private boolean clicked = false;
    // Set hover action
    private Consumer<Button> onHoverAction;
    // Set hover action
    private Consumer<Button> unHoverAction;
    // Set click action
    private Consumer<Button> onClickAction;
    private Consumer<Button> onDragAction;

    public Button(float x, float y, Texture texture) {
        super(x, y);
        //TODO change code here? "new Texture(path)", will need texture to store original file path?
        //this.texture = new Texture(texture.getPath());
        this.texture = texture;
        //TODO these are just temporary while I try to figure out where I'm at
        //List<Texture> textures = List.of(this.texture);
        //atlas = new TextureAtlas();
        //TODO [0] I think... instead of creating a new renderer and atlas here we should have something setup to reuse / pass through our existing renderer and atlas. we might need a GUI handler? idk
        //renderer = new BatchRenderer(atlas, shader);
    }

    @Override
    public void update() {

    }

    @Override
    public void render() {
        if (!visible) return;
        //TODO [0] need to re-implement logic for shader behavior so we can highlight our buttons / do other rendering modifications.
        renderer.draw(texture, x, y, shader, shaderModifier);
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
        return !texture.getTransparencyMap(true)[pixelIndex];
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
    @Deprecated
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
    public Texture getTexture() {
        return texture;
    }

    public double[] getMousePosition() {
        return mousePosition;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isClicked() {
        return clicked;
    }
    public void setShaderModifier(Consumer<Shader> shaderModifier) {
        this.shaderModifier = shaderModifier;
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