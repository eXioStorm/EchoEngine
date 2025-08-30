package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

/**
 * A transformation from shape coordinates to pixel coordinates.
 */
public class Projection {

    private Vector2d scale;
    private Vector2d translate;

    public Projection() {
        this.scale = new Vector2d(1.0, 1.0);
        this.translate = new Vector2d(0.0, 0.0);
    }

    public Projection(Vector2d scale, Vector2d translate) {
        this.scale = new Vector2d(scale);
        this.translate = new Vector2d(translate);
    }

    /**
     * Converts the shape coordinate to pixel coordinate.
     */
    public Vector2d project(Vector2d coord) {
        return new Vector2d(coord).add(translate).mul(scale);
    }

    /**
     * Converts the pixel coordinate to shape coordinate.
     */
    public Vector2d unproject(Vector2d coord) {
        return new Vector2d(coord).div(scale).sub(translate);
    }

    /**
     * Converts the vector to pixel coordinate space.
     */
    public Vector2d projectVector(Vector2d vector) {
        return new Vector2d(vector).mul(scale);
    }

    /**
     * Converts the vector from pixel coordinate space.
     */
    public Vector2d unprojectVector(Vector2d vector) {
        return new Vector2d(vector).div(scale);
    }

    /**
     * Converts the X-coordinate from shape to pixel coordinate space.
     */
    public double projectX(double x) {
        return scale.x * (x + translate.x);
    }

    /**
     * Converts the Y-coordinate from shape to pixel coordinate space.
     */
    public double projectY(double y) {
        return scale.y * (y + translate.y);
    }

    /**
     * Converts the X-coordinate from pixel to shape coordinate space.
     */
    public double unprojectX(double x) {
        return x / scale.x - translate.x;
    }

    /**
     * Converts the Y-coordinate from pixel to shape coordinate space.
     */
    public double unprojectY(double y) {
        return y / scale.y - translate.y;
    }

    // Getters
    public Vector2d getScale() {
        return new Vector2d(scale);
    }

    public Vector2d getTranslate() {
        return new Vector2d(translate);
    }

    // Setters
    public void setScale(Vector2d scale) {
        this.scale = new Vector2d(scale);
    }

    public void setTranslate(Vector2d translate) {
        this.translate = new Vector2d(translate);
    }
}