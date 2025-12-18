package com.github.exiostorm.utils.msdf;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
//TODO 20251218 This class doesn't match the C++ implementation, changes are needed for image flipping (why our generated glyph is upside down)
/**
 * Reference to a 2D image bitmap or buffer acting as one.
 * Pixel storage not owned or managed by the object.
 */
public class BitmapRef<T> {
    protected T pixels;
    protected int width;
    protected int height;
    protected int channels;

    public BitmapRef() {
        this.pixels = null;
        this.width = 0;
        this.height = 0;
        this.channels = 1;
    }

    public BitmapRef(T pixels, int width, int height) {
        this(pixels, width, height, 1);
    }

    public BitmapRef(T pixels, int width, int height, int channels) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.channels = channels;
    }

    /**
     * Get pixel data at coordinates (x, y).
     * Returns array starting position for multi-channel data.
     */
    public int getPixelIndex(int x, int y) {
        return channels * (width * y + x);
    }

    /**
     * Get single channel value at coordinates (x, y, channel).
     */
    public Object getPixel(int x, int y, int channel) {
        int index = getPixelIndex(x, y) + channel;

        if (pixels instanceof float[]) {
            return ((float[]) pixels)[index];
        } else if (pixels instanceof int[]) {
            return ((int[]) pixels)[index];
        } else if (pixels instanceof byte[]) {
            return ((byte[]) pixels)[index];
        } else if (pixels instanceof FloatBuffer) {
            return ((FloatBuffer) pixels).get(index);
        } else if (pixels instanceof IntBuffer) {
            return ((IntBuffer) pixels).get(index);
        } else if (pixels instanceof ByteBuffer) {
            return ((ByteBuffer) pixels).get(index);
        }

        throw new UnsupportedOperationException("Unsupported pixel type");
    }

    /**
     * Set single channel value at coordinates (x, y, channel).
     */
    public void setPixel(int x, int y, int channel, Object value) {
        int index = getPixelIndex(x, y) + channel;

        if (pixels instanceof float[]) {
            ((float[]) pixels)[index] = (Float) value;
        } else if (pixels instanceof int[]) {
            ((int[]) pixels)[index] = (Integer) value;
        } else if (pixels instanceof byte[]) {
            ((byte[]) pixels)[index] = (Byte) value;
        } else if (pixels instanceof FloatBuffer) {
            ((FloatBuffer) pixels).put(index, (Float) value);
        } else if (pixels instanceof IntBuffer) {
            ((IntBuffer) pixels).put(index, (Integer) value);
        } else if (pixels instanceof ByteBuffer) {
            ((ByteBuffer) pixels).put(index, (Byte) value);
        } else {
            throw new UnsupportedOperationException("Unsupported pixel type");
        }
    }

    // Getters
    public T getPixels() { return pixels; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getChannels() { return channels; }
}