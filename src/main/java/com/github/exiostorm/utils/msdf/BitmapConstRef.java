package com.github.exiostorm.utils.msdf;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer; /**
 * Constant reference to a 2D image bitmap or buffer acting as one.
 * Pixel storage not owned or managed by the object.
 */
public class BitmapConstRef<T> {
    protected final T pixels;
    protected final int width;
    protected final int height;
    protected final int channels;

    public BitmapConstRef() {
        this.pixels = null;
        this.width = 0;
        this.height = 0;
        this.channels = 1;
    }

    public BitmapConstRef(T pixels, int width, int height) {
        this(pixels, width, height, 1);
    }

    public BitmapConstRef(T pixels, int width, int height, int channels) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.channels = channels;
    }

    public BitmapConstRef(BitmapRef<T> orig) {
        this.pixels = orig.pixels;
        this.width = orig.width;
        this.height = orig.height;
        this.channels = orig.channels;
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

    // Getters
    public T getPixels() { return pixels; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getChannels() { return channels; }
}
