package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.enums.YAxisOrientation;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
/**
 * Reference to a 2D image bitmap or buffer acting as one.
 * Pixel storage not owned or managed by the object.
 */
public class BitmapRef<T> {
    protected T pixels;
    protected int width;
    protected int height;
    protected int channels;
    protected boolean yOrientation = YAxisOrientation.getDefault().getBool();

    public BitmapRef() {
        this.pixels = null;
        this.width = 0;
        this.height = 0;
        this.channels = 1;
        this.yOrientation = YAxisOrientation.getDefault().getBool();
    }

    public BitmapRef(T pixels, int width, int height) {
        this(pixels, width, height, 1);
    }

    public BitmapRef(T pixels, int width, int height, int channels) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.yOrientation = YAxisOrientation.getDefault().getBool();
    }

    /**
     * Get pixel data at coordinates (x, y).
     * Returns array starting position for multi-channel data.
     */
    // Then modify getPixelIndex to respect yOrientation:
    public int getPixelIndex(int x, int y) {
        int actualY = yOrientation ? (height - 1 - y) : y;
        return channels * (width * actualY + x);
    }


    /**
     * Get a specific channel value at coordinates (x, y, channel).
     * Channel should be in range [0, channels-1].
     */
    public float getChannel(int x, int y, int channel) {
        if (channel < 0 || channel >= channels) {
            throw new IndexOutOfBoundsException("Channel " + channel + " out of bounds [0, " + channels + ")");
        }

        int index = getPixelIndex(x, y) + channel;

        if (pixels instanceof float[]) {
            return ((float[]) pixels)[index];
        } else if (pixels instanceof double[]) {
            return (float) ((double[]) pixels)[index];
        } else if (pixels instanceof int[]) {
            return ((int[]) pixels)[index];
        } else if (pixels instanceof byte[]) {
            return ((byte[]) pixels)[index];
        }

        throw new UnsupportedOperationException("Unsupported pixel type: " + pixels.getClass());
    }

    /**
     * Set a specific channel value at coordinates (x, y, channel).
     * Channel should be in range [0, channels-1].
     */
    public void setChannel(int x, int y, int channel, float value) {
        if (channel < 0 || channel >= channels) {
            throw new IndexOutOfBoundsException("Channel " + channel + " out of bounds [0, " + channels + ")");
        }

        int index = getPixelIndex(x, y) + channel;

        if (pixels instanceof float[]) {
            ((float[]) pixels)[index] = value;
        } else if (pixels instanceof double[]) {
            ((double[]) pixels)[index] = value;
        } else if (pixels instanceof int[]) {
            ((int[]) pixels)[index] = (int) value;
        } else if (pixels instanceof byte[]) {
            ((byte[]) pixels)[index] = (byte) value;
        } else {
            throw new UnsupportedOperationException("Unsupported pixel type: " + pixels.getClass());
        }
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
    /**
     * Reorients the bitmap's Y-axis based on the provided orientation.
     * After calling this, all getPixel/setPixel operations will be adjusted accordingly.
     */
    public void reorient(Boolean orientation) {
        this.yOrientation = orientation == YAxisOrientation.Y_DOWNWARD.getBool();
    }
    public boolean getYOrientation() { return yOrientation; }

    // Getters
    public T getPixels() { return pixels; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getChannels() { return channels; }
}