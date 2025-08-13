package com.github.exiostorm.graphics.gui;

import com.github.exiostorm.graphics.*;
import com.github.exiostorm.utils.*;

import java.awt.*;
import java.util.function.Consumer;

import static com.github.exiostorm.main.EchoGame.gamePanel;

//TODO [0] need logic for scale so our method "isMouseOver()" can scale up our transparency map as well.
public class Button extends GUIElement {
    Shader shader = gamePanel.getShader();
    TextureAtlas atlas = gamePanel.getAtlas();

    Material shaderMaterial;
    private Texture texture;
    private Polygon polygon; //TODO [!][!!][!!!][20250813@2:15pm]
    private int width;
    private int height;
    private boolean[] transparencyMap;
    private float rotation;    // Rotation in radians
    private float scaleX;      // Scale factor for X
    private float scaleY;      // Scale factor for Y
    private boolean flipX;    // Horizontal flip
    private boolean flipY;    // Vertical flip
    private long lastPressed = 0;
    public boolean hovered = false;
    public boolean clicked = false;
    private boolean usePolygon = false;
    private boolean hasTransforms = false;
    // Set hover action
    private Consumer<Button> onHoverAction;
    // Set hover action
    private Consumer<Button> unHoverAction;
    // Set click action
    private Consumer<Button> onClickAction;
    private Consumer<Button> onDragAction;

    public Button(float x, float y, float rotation, float scaleX, float scaleY, boolean flipX, boolean flipY, boolean usePolygon, Texture texture) {
        super(x, y);
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.flipX = flipX;
        this.flipY = flipY;
        this.usePolygon = usePolygon;
        //TODO [!][!!][!!!][20250813@2:58pm] need to also transform polygon if that gets used.
        MathTools.TransformResult transformResult = MathTools.transformMap(texture.getTransparencyMap(true), texture.getWidth(), texture.getHeight(), scaleX, scaleY, flipX, flipY, rotation);
        this.transparencyMap = transformResult.map;
        this.width = transformResult.width;
        this.height = transformResult.height;
        this.hasTransforms = true;
        this.texture = texture;
    }
    public Button(float x, float y, float rotation, float scaleX, float scaleY, boolean flipX, boolean flipY, Texture texture) {
        super(x, y);
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.flipX = flipX;
        this.flipY = flipY;
        //TODO [!][!!][!!!][20250813@2:58pm] need to also transform polygon if that gets used.
        MathTools.TransformResult transformResult = MathTools.transformMap(texture.getTransparencyMap(true), texture.getWidth(), texture.getHeight(), scaleX, scaleY, flipX, flipY, rotation);
        this.transparencyMap = transformResult.map;
        this.width = transformResult.width;
        this.height = transformResult.height;
        this.hasTransforms = true;
        this.texture = texture;
    }
    //TODO [!] idk about having usePolygon as part of the initialization, but will leave it in for now.
    public Button(float x, float y, boolean usePolygon, Texture texture) {
        super(x, y);
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        this.texture = texture;
        this.usePolygon = usePolygon;
        if (!usePolygon) {
            this.transparencyMap = texture.getTransparencyMap(true);
        } else {
            this.polygon = squareHole.generateSimplePolygon(this.width, this.height);
        }
    }
    public Button(float x, float y, Texture texture) {
        super(x,y);
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        this.texture = texture;
        this.transparencyMap = texture.getTransparencyMap(true);
    }

    @Override
    public void update() {

    }

    @Override
    public void render() {
        if (!visible) return;
        //TODO do something with the z coordinate for layering in order of objects last updated. e.g. when moving a button on top of another button.
        //gamePanel.getRenderer().draw(texture, atlas, x, y, 0.000000000000000000000000000000000000000000001f, shader, shaderMaterial);
        gamePanel.getRenderer().draw(gamePanel.getAtlas().getAtlasID(), gamePanel.getAtlas().getAtlasSlot(), texture.getWidth(), texture.getHeight(), AtlasManager.getUV(gamePanel.getAtlas(), texture), x, y, 0.000000000000000000000000000000000000000000001f, shader, shaderMaterial, 0.0f, 1.0f, 1.0f, flipX, flipY);
    }

    //TODO [0] our transformation logic will conflict with our button logic since nothing else gets transformed besides what's rendered.
    // will need to change logic to account for transformations.
    @Override
    public boolean isMouseOver(float mouseX, float mouseY) {
        // Adjust mouse coordinates relative to the button's position
        float localMouseX = mouseX - x;
        float localMouseY = mouseY - y;

        // Ensure the mouse is within the bounds of the button
        //TODO [!][!!][!!!][20250810]
        // Need to modify logic to use our button object instead of original texture width/height
        // because we need to support transformations, and we need simplified logic to
        // support bounding boxes instead for cases where we need more efficiency.
        if (localMouseX < 0 || localMouseX >= this.width ||
                localMouseY < 0 || localMouseY >= this.height) {
            return false;
        }
        //TODO [!] this won't work when transformed. need a bit more logic to make transformations work again.
        if (usePolygon) {
            // Use polygon detection if polygon is defined
            if (polygon != null) {
                return polygon.contains((int)localMouseX, (int)localMouseY);
            } else {
                // Fall back to rectangle detection
                return true;
            }
        }
        // Map mouse coordinates to pixel coordinates in the original texture
        int pixelX = (int) localMouseX;
        int pixelY = (int) localMouseY;

        // Get the pixel index in the 1D array
        int pixelIndex = pixelY * this.width + pixelX;

        // Consider the pixel under the mouse as valid if alpha > 0 (non-transparent)
        return !this.transparencyMap[pixelIndex];
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

    //TODO [1] figure out something to detect holding button down
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
    public long getLastPressed() {
        return lastPressed;
    }

    public void setLastPressed(long l) {
        this.lastPressed = l;
    }
    public void modifyTransforms (float rotation, float scaleX, float scaleY, boolean flipX, boolean flipY) {
        this.hasTransforms = true;
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.flipX = flipX;
        this.flipY = flipY;
    }
    public void useBoundaryBox(boolean value) {
        this.usePolygon = value;
    }
}