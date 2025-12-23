package com.github.exiostorm.utils.msdf;

public class BitmapRef<T> {

    protected T pixels;
    protected int width;
    protected int height;
    protected int channels;
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

    /**
     * renamed to refOperator, from C++ 'operator', to avoid confusion with section 'operator' that returns an index of a section instead of index from entirety.
     */
    public int refOperator(int x, int y) {
        // actualY if false = height -1 - y
        //    -1 to avoid out of bounds, then subtract y for the negative value to put our y opposite of the original spot.
        // if true then just supply provided y.
        int actualY = yOrientation ? (height - 1 - y) : y;
        // supposed to be equivalent to : return pixels+N*(width*y+x);
        // pixels+channels*(width*actualY+x)
        // sounds like this is correct, ignore *pixels+,
        // because the initial value was a pointer that just tells it to return the value of that pointer from the address resulting from the math done after-wards?
        return channels * (width * actualY + x);
    }
    public int sectionOperator(int x, int y, int stride) {
        //TODO 20251224 "bookmark", last spot worked / to be worked.
        return 0;
    }
    /**
     * THIS CANNOT BE UNDONE!
     */
    public void lockBitmap() {
        this.lock = true;
    }
    //TODO BitmapRef
    /* Notes :
     * Java does not have *pointers like C++, so *pointer arithmetic becomes array index arithmetic in Java.
     * *operator() from C++ again uses pointer arithmetic, so in Java we need to use array index arithmetic to get proper return value from this method.
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
     */

}
