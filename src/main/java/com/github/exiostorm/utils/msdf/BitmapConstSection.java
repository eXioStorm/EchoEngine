package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.enums.YAxisOrientation;

/**
 * Constant (read-only) reference to a 2D image bitmap with non-contiguous rows.
 * Can represent a section of a larger bitmap, bitmap with padded rows,
 * or vertically flipped bitmap (rowStride can be negative).
 *
 * Pixel storage not owned or managed by the object.
 *
 * Corresponds to C++ BitmapConstSection template.
 *
 * @param <T> The pixel data type (float[], int[], byte[], etc.)
 */
public class BitmapConstSection<T> {
    protected final T pixels;
    protected int offset; // Starting offset in the array (mutable for reorient)
    protected final int width;
    protected final int height;
    protected final int channels;
    /**
     * Specifies the difference between the beginnings of adjacent pixel rows
     * as the number of array elements. Can be negative for vertical flipping.
     * Mutable for reorient operation.
     */
    protected int rowStride;
    /**
     * Y-axis orientation. Mutable for reorient operation.
     */
    protected boolean yOrientation;

    public BitmapConstSection() {
        this.pixels = null;
        this.offset = 0;
        this.width = 0;
        this.height = 0;
        this.channels = 1;
        this.rowStride = 0;
        this.yOrientation = YAxisOrientation.getDefault().getBool();
    }

    public BitmapConstSection(T pixels, int width, int height) {
        this(pixels, 0, width, height, 1, width, YAxisOrientation.getDefault().getBool());
    }

    public BitmapConstSection(T pixels, int width, int height, int channels) {
        this(pixels, 0, width, height, channels, channels * width, YAxisOrientation.getDefault().getBool());
    }

    public BitmapConstSection(T pixels, int width, int height, int channels, boolean yOrientation) {
        this(pixels, 0, width, height, channels, channels * width, yOrientation);
    }

    public BitmapConstSection(T pixels, int offset, int width, int height, int channels, int rowStride, boolean yOrientation) {
        this.pixels = pixels;
        this.offset = offset;
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.rowStride = rowStride;
        this.yOrientation = yOrientation;
    }

    /**
     * Copy constructor from BitmapRef.
     */
    public BitmapConstSection(BitmapRef<T> orig) {
        this.pixels = orig.pixels;
        this.offset = 0;
        this.width = orig.width;
        this.height = orig.height;
        this.channels = orig.channels;
        this.rowStride = orig.channels * orig.width;
        this.yOrientation = orig.yOrientation;
    }

    /**
     * Copy constructor from BitmapConstRef.
     */
    public BitmapConstSection(BitmapConstRef<T> orig) {
        this.pixels = orig.pixels;
        this.offset = 0;
        this.width = orig.width;
        this.height = orig.height;
        this.channels = orig.channels;
        this.rowStride = orig.channels * orig.width;
        this.yOrientation = orig.yOrientation;
    }

    /**
     * Copy constructor from BitmapSection.
     */
    public BitmapConstSection(BitmapSection<T> orig) {
        this.pixels = orig.pixels;
        this.offset = orig.offset;
        this.width = orig.width;
        this.height = orig.height;
        this.channels = orig.channels;
        this.rowStride = orig.rowStride;
        this.yOrientation = orig.yOrientation;
    }

    /**
     * Returns the starting index for pixel at (x, y) in the underlying array.
     *
     * C++ equivalent: pixels + rowStride*y + N*x
     */
    public int getPixelIndex(int x, int y) {
        return offset + rowStride * y + channels * x;
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
     * Returns a constant reference to a rectangular subsection of the bitmap.
     * Bounds are [xMin, yMin) to [xMax, yMax) - excluding xMax and yMax.
     */
    public BitmapConstSection<T> getSection(int xMin, int yMin, int xMax, int yMax) {
        if (xMin < 0 || yMin < 0 || xMax > width || yMax > height || xMin >= xMax || yMin >= yMax) {
            throw new IllegalArgumentException("Invalid section bounds");
        }

        int newOffset = offset + rowStride * yMin + channels * xMin;
        int sectionWidth = xMax - xMin;
        int sectionHeight = yMax - yMin;

        return new BitmapConstSection<>(pixels, newOffset, sectionWidth, sectionHeight, channels, rowStride, yOrientation);
    }

    /**
     * Returns a constant reference to a rectangular subsection of the bitmap.
     * C++ has both getSection and getConstSection that return the same thing.
     */
    public BitmapConstSection<T> getConstSection(int xMin, int yMin, int xMax, int yMax) {
        return getSection(xMin, yMin, xMax, yMax);
    }

    /**
     * Makes sure that the section's Y-axis orientation matches the argument
     * by potentially reordering its rows (flipping the bitmap vertically).
     *
     * This is a critical function for correct glyph rendering.
     * Note: Even though this is a const reference, the C++ version allows
     * this mutation as it only affects the view, not the underlying data.
     */
    public void reorient(boolean newYAxisOrientation) {
        if (yOrientation != newYAxisOrientation) {
            // Move to the start of the last row
            offset += rowStride * (height - 1);
            // Reverse the direction of row traversal
            rowStride = -rowStride;
            // Update orientation
            yOrientation = newYAxisOrientation;
        }
    }

    // Getters
    public T getPixels() {
        return pixels;
    }

    public int getOffset() {
        return offset;
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

    public int getRowStride() {
        return rowStride;
    }

    public boolean getYOrientation() {
        return yOrientation;
    }
}