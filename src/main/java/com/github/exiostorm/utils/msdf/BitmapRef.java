package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.enums.YAxisOrientation;

//TODO move methods to another class to keep this one small?
// (though maybe not... or maybe yes? maybe make a method to return that class, then can be like bmp.util().reorient())

//TODO 20251228 - Make ANOTHER class to act as the C++ pointer, I can't believe Claude never suggested this previously. example :
/*
public class PixelPointer {
    public final float[] array;
    public final int offset;
    }
 */
//TODO 20251228 NEVERMIND! turns out this is what causes our headache with casting. explains why Claude got so confused before.
// THOUGH, could perhaps make a pointer class that only contains the offset values instead of the pixels. no casting required.
public class BitmapRef<T> {
    protected T pixels;
    protected int width;
    protected int height;
    protected int nChannels;
    protected boolean yOrientation;
    /**
     * The distance between rows, originally only from section classes.
     */
    protected int rowStride;
    /**
     * necessary for array arithmetic. <br>
     * originally only from section classes for getting section info.
     */
    protected int offset;
    /**
     * When 'lock' is set to true during object creation it marks it as read-only, and cannot be changed.
     */
    protected boolean lock;

    //TODO not sure if I like this approach or not, though it does directly link the utility class to this one so you can easily figure out what does what.
    public BitmapUtil util;

    public BitmapRef() {
        this.lock = false;
        this.pixels = null;
        this.width = 0;
        this.height = 0;
        this.rowStride = 0;
        this.offset = 0;
        this.nChannels = 1;
        this.yOrientation = YAxisOrientation.getDefault().getBool();
    }
    public BitmapRef(boolean lock, T pixels, int width, int height) {
        this(lock, pixels, width, height, 0, 0, 1, YAxisOrientation.getDefault().getBool());
    }
    public BitmapRef(boolean lock, T pixels, int width, int height, int rowStride) {
        this(lock, pixels, width, height, rowStride, 0, 1, YAxisOrientation.getDefault().getBool());
    }
    public BitmapRef(boolean lock, T pixels, int width, int height, int rowStride, int offset) {
        this(lock, pixels, width, height, rowStride, offset, 1, YAxisOrientation.getDefault().getBool());
    }
    public BitmapRef(boolean lock, T pixels, int width, int height, int rowStride, int offset, int nChannels) {
        this(lock, pixels, width, height, rowStride, offset, nChannels, YAxisOrientation.getDefault().getBool());
    }
    public BitmapRef(T pixels, int width, int height) {
        this(false, pixels, width, height, 0, 0, 1, YAxisOrientation.getDefault().getBool());
    }
    public BitmapRef(T pixels, int width, int height, int rowStride) {
        this(false, pixels, width, height, rowStride, 0, 1, YAxisOrientation.getDefault().getBool());
    }
    public BitmapRef(T pixels, int width, int height, int rowStride, int offset) {
        this(false, pixels, width, height, rowStride, offset, 1, YAxisOrientation.getDefault().getBool());
    }
    public BitmapRef(T pixels, int width, int height, int rowStride, int offset, int nChannels) {
        this(false, pixels, width, height, rowStride, offset, nChannels, YAxisOrientation.getDefault().getBool());
    }

    /**
     * Instead of a new class for const, const is now defined during creation or an irreversible switch.
     * <br>if you don't define lock as true when creating a BitmapRef object, then it defaults to false.
     */
    public BitmapRef(boolean lock, T pixels, int width, int height, int rowStride, int offset, int nChannels, boolean yOrientation ) {
        this.lock = lock;
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.rowStride = rowStride;
        this.offset = offset;
        this.nChannels = nChannels;
        this.yOrientation = yOrientation;
    }
    /**
     * renamed to refOperator, from C++ 'operator', to avoid confusion with section 'operator' that returns an index of a section instead of index from entirety.
     */
    public int refOperator(int x, int y) {
        // supposed to be equivalent to : return pixels+N*(width*y+x);
        // pixels+nChannels*(width*actualY+x)
        // sounds like this is correct,
        // ignore *pixels+ because the initial value was a pointer that just tells it to return the value of that pointer from the address resulting from the math done after-wards?
        //TODO got rid of actualY, that was part of our issues, was modifying logic of over 200 other references.
        return nChannels * (width * y + x);
    }
    public int sectionOperator(int x, int y) {
        //TODO got rid of actualY, that was part of our issues, was modifying logic of over 200 other references.
        return offset + rowStride * y + nChannels * x;
    }

    //TODO keep careful observation over this method, it SEEMS correct,
    // but the math loses me. if 'rowStride == 0' then this is not a section, therefore no references to rowStride...
    // but wouldn't we need to create rowStride since the return should itself be a section? perhaps checking 'if rowStride == 0' isn't the best approach...
    // edit : it's good! originally would return a BitmapSection forcing the else statement anyways. think we're good to move on!
    public BitmapRef<T> getSection(int xMin, int yMin, int xMax, int yMax) {
        if (rowStride == 0) {
            int newOffset = offset + nChannels * (width * yMin + xMin);
            return new BitmapRef<>(lock, pixels, xMax - xMin, yMax - yMin,
                    nChannels * width, newOffset, nChannels, yOrientation);
        } else {
            int newOffset = offset + rowStride * yMin + nChannels * xMin;
            return new BitmapRef<>(lock, pixels, xMax - xMin, yMax - yMin,
                    rowStride, newOffset, nChannels, yOrientation);
        }
    }
    /*TODO
     * return BitmapSection<T, N>(pixels+N*(width*yMin+xMin), xMax-xMin, yMax-yMin, N*width, yOrientation);
     * return BitmapSection<T, N>(pixels+rowStride*yMin+N*xMin, xMax-xMin, yMax-yMin, rowStride, yOrientation);
     */
    //Might want to look closer at this implementation? don't remember
    public boolean reorient(boolean newYAxisOrientation) {
        if (pixels == null) return false;
        if (yOrientation == newYAxisOrientation) {
            return true; // Already in correct orientation
        }
        int elementsPerRow = width * nChannels;
        // we need multiple cases, simply because of where we create the new array to move the data to. can't 'new T[src.length]'
        switch (pixels) {
            case byte[] src -> {
                byte[] flipped = new byte[src.length];
                for (int y = 0; y < height; y++) {
                    int srcRow = height - 1 - y;
                    System.arraycopy(src, srcRow * elementsPerRow,
                            flipped, y * elementsPerRow, elementsPerRow);
                }
                pixels = (T) flipped;
            }
            case int[] src -> {
                int[] flipped = new int[src.length];
                for (int y = 0; y < height; y++) {
                    int srcRow = height - 1 - y;
                    System.arraycopy(src, srcRow * elementsPerRow,
                            flipped, y * elementsPerRow, elementsPerRow);
                }
                pixels = (T) flipped;
            }
            case float[] src -> {
                float[] flipped = new float[src.length];
                for (int y = 0; y < height; y++) {
                    int srcRow = height - 1 - y;
                    System.arraycopy(src, srcRow * elementsPerRow,
                            flipped, y * elementsPerRow, elementsPerRow);
                }
                pixels = (T) flipped;
            }
            case double[] src -> {
                double[] flipped = new double[src.length];
                for (int y = 0; y < height; y++) {
                    int srcRow = height - 1 - y;
                    System.arraycopy(src, srcRow * elementsPerRow,
                            flipped, y * elementsPerRow, elementsPerRow);
                }
                pixels = (T) flipped;
            }
            case null, default -> throw new UnsupportedOperationException("Unsupported pixel type");
        }
        yOrientation = newYAxisOrientation;
        return true;
    }
    /**
     * <b>THIS CANNOT BE UNDONE!</b>
     */
    public void lockBitmap() {
        this.lock = true;
    }






    //TODO BitmapRef
    /* Notes :
     * Java does not have *pointers like C++, so *pointer arithmetic becomes array index arithmetic in Java.
     * *operator() from C++ again uses pointer arithmetic, so in Java we need to use array index arithmetic to get proper return value from this method.
     */
    //TODO
    /* int actualY = yOrientation ? (height - 1 - y) : y;
     * actualY if false = height -1 - y
     * -1 to avoid out of bounds, then subtract y for the negative value to put our y opposite of the original spot.
     * if true then just supply provided y.
     */

    /*
    Note : this old method seems to be lacking double[]?
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

        public BitmapRef<T> getSection(int xMin, int yMin, int xMax, int yMax) {
        if (rowStride == 0) {
            return new BitmapRef<>(nChannels*(width*yMin+xMin), xMax-xMin, yMax-yMin, nChannels*width, yOrientation);
        } else {
            return new BitmapRef<>(true, rowStride*yMin+nChannels*xMin, xMax-xMin, yMax-yMin, rowStride, yOrientation);
        }
        return null;
    }
     */

}
