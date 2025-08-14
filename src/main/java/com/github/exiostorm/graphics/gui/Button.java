package com.github.exiostorm.graphics.gui;

import com.github.exiostorm.graphics.*;
import com.github.exiostorm.utils.*;

import java.awt.*;
import java.util.function.Consumer;

import static com.github.exiostorm.main.EchoGame.gamePanel;
import static com.github.exiostorm.utils.ShapeUtils.transformPolygon;

//TODO [0] need logic for scale so our method "isMouseOver()" can scale up our transparency map as well.
//TODO [!][!!][!!!][20250814@2:42pm]
// Maybe use suppliers to use polygon/transMap instead so we don't have if checks that pile up? reduce number of if checks that are always true/false?
public class Button extends GUIElement {
    Shader shader = gamePanel.getShader();
    TextureAtlas atlas = gamePanel.getAtlas();

    Material shaderMaterial;
    private Texture texture;
    private Polygon polygon;
    private int width;
    private int height;
    private boolean[] transparencyMap;
    private float rotation = 0;    // Rotation in radians
    private float scaleX = 1.0f;      // Scale factor for X
    private float scaleY = 1.0f;      // Scale factor for Y
    private boolean flipX = false;    // Horizontal flip
    private boolean flipY = false;    // Vertical flip
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
        this.texture = texture;
        this.hasTransforms = true;
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.flipX = flipX;
        this.flipY = flipY;
        this.usePolygon = usePolygon;
        if (usePolygon) {
            this.width = texture.getWidth();
            this.height = texture.getHeight();
            this.polygon = transformPolygon(texture.getPolygon(), scaleX, scaleY, flipX, flipY, rotation);
        } else {
            ShapeUtils.TransformResult transformResult = ShapeUtils.transformMap(texture.getTransparencyMap(true), texture.getWidth(), texture.getHeight(), scaleX, scaleY, flipX, flipY, rotation);
            this.transparencyMap = transformResult.map;
            this.width = transformResult.width;
            this.height = transformResult.height;
        }
    }
    public Button(float x, float y, float rotation, float scaleX, float scaleY, boolean flipX, boolean flipY, Texture texture) {
        super(x, y);
        this.texture = texture;
        this.hasTransforms = true;
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.flipX = flipX;
        this.flipY = flipY;
        ShapeUtils.TransformResult transformResult = ShapeUtils.transformMap(texture.getTransparencyMap(true), texture.getWidth(), texture.getHeight(), scaleX, scaleY, flipX, flipY, rotation);
        this.transparencyMap = transformResult.map;
        this.width = transformResult.width;
        this.height = transformResult.height;
    }
    public Button(float x, float y, boolean usePolygon, Texture texture) {
        super(x, y);
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        this.texture = texture;
        this.usePolygon = usePolygon;
        if (!usePolygon) {
            this.transparencyMap = texture.getTransparencyMap(true);
        } else {
            this.polygon = texture.getPolygon();
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
        //TODO [!][!!][!!!][20250814@2:46pm]
        // instead of having "0.000000000000000000000000000000000000000000001f", we need to implement a layer system...
        gamePanel.getRenderer().draw(
        this.atlas.getAtlasID(), this.atlas.getAtlasSlot(), this.texture.getWidth(), this.texture.getHeight(), AtlasManager.getUV(this.atlas, texture), x, y, 0.000000000000000000000000000000000000000000001f, this.shader, this.shaderMaterial, this.rotation, this.scaleX, this.scaleY, this.flipX, this.flipY
        );
    }

    //TODO [0] our transformation logic will conflict with our button logic since nothing else gets transformed besides what's rendered.
    // will need to change logic to account for transformations.
    @Override
    public boolean isMouseOver(float mouseX, float mouseY) {
        // Adjust mouse coordinates relative to the button's position
        float localMouseX = mouseX - x;
        float localMouseY = mouseY - y;

        // Ensure the mouse is within the bounds of the button
        if (localMouseX < 0 || localMouseX >= this.width ||
                localMouseY < 0 || localMouseY >= this.height) {
            return false;
        }

        // Use polygon detection if polygon is defined
        if (usePolygon) {
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
        //TODO [!][!!][!!!][20250814@1:53pm]
        // need to include logic to update transformed shapes
        this.hasTransforms = true;
        this.rotation = rotation;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.flipX = flipX;
        this.flipY = flipY;
    }
    public void usePolygon(boolean value) {
        this.usePolygon = value;
    }
}