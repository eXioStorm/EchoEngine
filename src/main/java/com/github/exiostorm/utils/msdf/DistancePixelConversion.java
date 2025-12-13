package com.github.exiostorm.utils.msdf;

public abstract class DistancePixelConversion<T> {
    protected DistanceMapping mapping;

    public DistancePixelConversion(DistanceMapping mapping) {
        this.mapping = mapping;
    }

    public abstract void convert(float[] pixels, T distance);
}