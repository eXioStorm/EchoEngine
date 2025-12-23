package com.github.exiostorm.utils.msdf;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays; /**
 * A 2D image bitmap with N channels of type T.
 * Pixel memory is managed by the class.
 */
@Deprecated // unable to locate where this was originally used, going to delete after we get everything working. Engine still launches even when fully commented out.
public class DprBitmap<T> {
    private T pixels;
    private int width;
    private int height;
    private int channels;
    private Class<T> pixelType;

    public DprBitmap() {
        this.pixels = null;
        this.width = 0;
        this.height = 0;
        this.channels = 1;
    }

    @SuppressWarnings("unchecked")
    public DprBitmap(Class<T> pixelType, int width, int height) {
        this(pixelType, width, height, 1);
    }

    @SuppressWarnings("unchecked")
    public DprBitmap(Class<T> pixelType, int width, int height, int channels) {
        this.pixelType = pixelType;
        this.width = width;
        this.height = height;
        this.channels = channels;

        int totalSize = width * height * channels;

        if (pixelType == float[].class) {
            this.pixels = (T) new float[totalSize];
        } else if (pixelType == int[].class) {
            this.pixels = (T) new int[totalSize];
        } else if (pixelType == byte[].class) {
            this.pixels = (T) new byte[totalSize];
        } else if (pixelType == FloatBuffer.class) {
            this.pixels = (T) FloatBuffer.allocate(totalSize);
        } else if (pixelType == IntBuffer.class) {
            this.pixels = (T) IntBuffer.allocate(totalSize);
        } else if (pixelType == ByteBuffer.class) {
            this.pixels = (T) ByteBuffer.allocate(totalSize);
        } else {
            throw new UnsupportedOperationException("Unsupported pixel type: " + pixelType);
        }
    }

    public DprBitmap(DprBitmapConstRef<T> orig) {
        this.width = orig.getWidth();
        this.height = orig.getHeight();
        this.channels = orig.getChannels();

        // Determine type from original
        T origPixels = orig.getPixels();
        copyFromReference(origPixels);
    }

    public DprBitmap(DprBitmap<T> orig) {
        this.width = orig.width;
        this.height = orig.height;
        this.channels = orig.channels;
        this.pixelType = orig.pixelType;

        copyFromReference(orig.pixels);
    }

    @SuppressWarnings("unchecked")
    private void copyFromReference(T srcPixels) {
        int totalSize = width * height * channels;

        if (srcPixels instanceof float[]) {
            this.pixels = (T) Arrays.copyOf((float[]) srcPixels, totalSize);
            this.pixelType = (Class<T>) float[].class;
        } else if (srcPixels instanceof int[]) {
            this.pixels = (T) Arrays.copyOf((int[]) srcPixels, totalSize);
            this.pixelType = (Class<T>) int[].class;
        } else if (srcPixels instanceof byte[]) {
            this.pixels = (T) Arrays.copyOf((byte[]) srcPixels, totalSize);
            this.pixelType = (Class<T>) byte[].class;
        } else if (srcPixels instanceof FloatBuffer) {
            FloatBuffer src = (FloatBuffer) srcPixels;
            FloatBuffer dest = FloatBuffer.allocate(totalSize);
            src.rewind();
            dest.put(src);
            dest.rewind();
            this.pixels = (T) dest;
            this.pixelType = (Class<T>) FloatBuffer.class;
        } else if (srcPixels instanceof IntBuffer) {
            IntBuffer src = (IntBuffer) srcPixels;
            IntBuffer dest = IntBuffer.allocate(totalSize);
            src.rewind();
            dest.put(src);
            dest.rewind();
            this.pixels = (T) dest;
            this.pixelType = (Class<T>) IntBuffer.class;
        } else if (srcPixels instanceof ByteBuffer) {
            ByteBuffer src = (ByteBuffer) srcPixels;
            ByteBuffer dest = ByteBuffer.allocate(totalSize);
            src.rewind();
            dest.put(src);
            dest.rewind();
            this.pixels = (T) dest;
            this.pixelType = (Class<T>) ByteBuffer.class;
        } else {
            throw new UnsupportedOperationException("Unsupported pixel type");
        }
    }

    // Assignment operators
    public DprBitmap<T> assign(DprBitmapConstRef<T> orig) {
        if (this.pixels != orig.getPixels()) {
            this.width = orig.getWidth();
            this.height = orig.getHeight();
            this.channels = orig.getChannels();
            copyFromReference(orig.getPixels());
        }
        return this;
    }

    public DprBitmap<T> assign(DprBitmap<T> orig) {
        if (this != orig) {
            this.width = orig.width;
            this.height = orig.height;
            this.channels = orig.channels;
            this.pixelType = orig.pixelType;
            copyFromReference(orig.pixels);
        }
        return this;
    }

    // Accessors
    public int width() { return width; }
    public int height() { return height; }
    public int getChannels() { return channels; }

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

    // Conversion operators equivalent
    public T getPixelData() {
        return pixels;
    }

    public DprBitmapRef<T> toBitmapRef() {
        return new DprBitmapRef<>(pixels, width, height, channels);
    }

    public DprBitmapConstRef<T> toBitmapConstRef() {
        return new DprBitmapConstRef<>(pixels, width, height, channels, false);
    }
}
