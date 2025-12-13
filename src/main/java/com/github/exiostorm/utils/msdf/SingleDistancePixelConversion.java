package com.github.exiostorm.utils.msdf;

public class SingleDistancePixelConversion extends DistancePixelConversion<Double> {
    public SingleDistancePixelConversion(DistanceMapping mapping) {
        super(mapping);
    }

    @Override
    public void convert(float[] pixels, Double distance) {
        pixels[0] = (float) mapping.map(distance);
    }
}
