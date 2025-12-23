package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.enums.YAxisOrientation;

/**
 * Constant (read-only) reference to a 2D image bitmap.
 * Pixel storage not owned or managed by the object.
 *
 * Corresponds to C++ BitmapConstRef template.
 *
 * @param <T> The pixel data type (float[], int[], byte[], etc.)
 */
@Deprecated
public class DprBitmapConstRef<T> {
    protected final T pixels;
    protected final int width;
    protected final int height;
    protected final int channels;
    protected boolean yOrientation;

    public DprBitmapConstRef() {
        this.pixels = null;
        this.width = 0;
        this.height = 0;
        this.channels = 1;
        this.yOrientation = YAxisOrientation.getDefault().getBool();
    }
    public DprBitmapConstRef(T pixels, int width, int height) {
        this(pixels, width, height, 1, YAxisOrientation.getDefault().getBool());
    }

    public DprBitmapConstRef(T pixels, int width, int height, int channels) {
        this(pixels, width, height, channels, YAxisOrientation.getDefault().getBool());
    }

    public DprBitmapConstRef(T pixels, int width, int height, int channels, boolean yOrientation) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.yOrientation = yOrientation;
    }

    /**
     * Copy constructor from BitmapRef.
     * Corresponds to C++ BitmapConstRef(const BitmapRef<T, N> &orig).
     */
    public DprBitmapConstRef(DprBitmapRef<T> orig) {
        this.pixels = orig.pixels;
        this.width = orig.width;
        this.height = orig.height;
        this.channels = orig.channels;
        this.yOrientation = orig.yOrientation;
    }

    /**
     * Returns the starting index for pixel at (x, y) in the underlying array.
     * C++ equivalent: operator()(int x, int y) which returns const T* pointer.
     */
    public int getPixelIndex(int x, int y) {
        return channels * (width * y + x);
    }

    /**
     * Get a specific channel value at coordinates (x, y, channel).
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
     * Get all channel values for pixel at (x, y).
     * Returns a new array containing [channel0, channel1, ..., channelN-1].
     */
    public float[] getPixel(int x, int y) {
        float[] result = new float[channels];
        int baseIndex = getPixelIndex(x, y);

        if (pixels instanceof float[]) {
            float[] arr = (float[]) pixels;
            for (int i = 0; i < channels; i++) {
                result[i] = arr[baseIndex + i];
            }
        } else if (pixels instanceof double[]) {
            double[] arr = (double[]) pixels;
            for (int i = 0; i < channels; i++) {
                result[i] = (float) arr[baseIndex + i];
            }
        } else if (pixels instanceof int[]) {
            int[] arr = (int[]) pixels;
            for (int i = 0; i < channels; i++) {
                result[i] = arr[baseIndex + i];
            }
        } else if (pixels instanceof byte[]) {
            byte[] arr = (byte[]) pixels;
            for (int i = 0; i < channels; i++) {
                result[i] = arr[baseIndex + i];
            }
        } else {
            throw new UnsupportedOperationException("Unsupported pixel type: " + pixels.getClass());
        }

        return result;
    }

    /**
     * Returns a constant reference to a rectangular section of the bitmap.
     * Bounds are [xMin, yMin) to [xMax, yMax) - excluding xMax and yMax.
     */
    public DprBitmapConstSection<T> getSection(int xMin, int yMin, int xMax, int yMax) {
        if (xMin < 0 || yMin < 0 || xMax > width || yMax > height || xMin >= xMax || yMin >= yMax) {
            throw new IllegalArgumentException("Invalid section bounds");
        }

        int offset = channels * (width * yMin + xMin);
        int sectionWidth = xMax - xMin;
        int sectionHeight = yMax - yMin;
        int rowStride = channels * width;

        return new DprBitmapConstSection<>(pixels, offset, sectionWidth, sectionHeight, channels, rowStride, yOrientation);
    }

    /**
     * Returns a constant reference to a rectangular section of the bitmap.
     * C++ has both getSection and getConstSection that return the same thing.
     */
    public DprBitmapConstSection<T> getConstSection(int xMin, int yMin, int xMax, int yMax) {
        return getSection(xMin, yMin, xMax, yMax);
    }

    // Getters
    public T getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getChannels() {
        return channels;
    }

    public boolean getYOrientation() {
        return yOrientation;
    }
}