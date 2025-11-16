package com.github.exiostorm.utils.msdf;

public class MultiDistancePixelConversion extends DistancePixelConversion<EdgeSelectors.MultiDistance> {
    public MultiDistancePixelConversion(DistanceMapping mapping) {
        super(mapping);
    }

    @Override
    public void convert(float[] pixels, EdgeSelectors.MultiDistance distance) {
        pixels[0] = (float) mapping.map(distance.r);
        pixels[1] = (float) mapping.map(distance.g);
        pixels[2] = (float) mapping.map(distance.b);
    }
}
