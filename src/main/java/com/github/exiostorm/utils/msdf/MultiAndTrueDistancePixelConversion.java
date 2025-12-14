package com.github.exiostorm.utils.msdf;

public class MultiAndTrueDistancePixelConversion extends DistancePixelConversion<EdgeSelectors.MultiAndTrueDistance> {
    public MultiAndTrueDistancePixelConversion(DistanceMapping mapping) {
        super(mapping);
    }

    @Override
    public void convert(float[] pixels, EdgeSelectors.MultiAndTrueDistance distance) {
        pixels[0] = (float) mapping.map(distance.c);
        pixels[1] = (float) mapping.map(distance.m);
        pixels[2] = (float) mapping.map(distance.y);
        pixels[3] = (float) mapping.map(distance.a);
    }
}
