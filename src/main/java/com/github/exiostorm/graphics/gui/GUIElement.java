package com.github.exiostorm.graphics.gui;

public abstract class GUIElement {
    protected float x, y; // Position and size
    protected boolean visible = true; // Visibility flag

    public GUIElement(float x, float y) {
        this.x = x;
        this.y = y;
        this.visible = true;
    }

    public abstract void update(); // Each element can handle updates (e.g., hover effects)
    public abstract void render(); // Render logic
    public abstract boolean isMouseOver(float mouseX, float mouseY);

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }
}