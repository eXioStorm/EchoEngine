package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

import java.util.Arrays;

public class msdfgen {

    // Generic distance field generation
    private static <D, C extends ContourCombiners.ContourCombiner<D>>
    void generateDistanceField(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            SDFTransformation transformation,
            Class<C> combinerClass,
            DistancePixelConversion<D> converter)
    {
        try {
            // Construct combiner instance with (Shape) constructor
            C combiner = combinerClass.getConstructor(MsdfShape.class).newInstance(msdfShape);

            ShapeDistanceFinder<C, D> distanceFinder =
                    new ShapeDistanceFinder<>(msdfShape, combiner);

            boolean rightToLeft = false;
            final int width = output.getWidth();
            final int height = output.getHeight();
            final int channels = output.getChannels();

            // temporary buffer for pixel channels (will be reused)
            float[] pixelChannels = new float[channels];

            for (int y = 0; y < height; y++) {
                //int row = msdfShape.inverseYAxis ? height - y - 1 : y;

                for (int col = 0; col < width; col++) {
                    int x = rightToLeft ? width - col - 1 : col;

                    // sample point in shape space
                    Vector2d p = transformation.unproject(new Vector2d(x + 0.5, y + 0.5));

                    // compute distance (distanceFinder.distance returns Object)
                    D distance = distanceFinder.distance(p);

                    // Note: conversion writes into the provided float[].
                    // Make sure the converter expects at most 'channels' entries.
                    // If converter writes fewer entries, the rest remain unchanged (we reset them below).
                    // Reset pixelChannels to 0 to avoid stale channels if converter writes fewer channels.
                    Arrays.fill(pixelChannels, 0f);

                    converter.convert(pixelChannels, distance);

                    // write converted channels back into the BitmapRef
                    for (int c = 0; c < channels; c++) {
                        // setPixel expects boxed Objects; we pass Float
                        output.setPixel(x, y, c, pixelChannels[c]);
                    }
                }

                rightToLeft = !rightToLeft;
            }

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate contour combiner: " + combinerClass, e);
        } catch (Exception e) {
            throw new RuntimeException("Error generating distance field", e);
        }
    }
    // SDF Generation
    public static void generateSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            SDFTransformation transformation) {
        generateSDF(output, msdfShape, transformation, new GeneratorConfig());
    }
    public static void generateSDF(DprBitmapRef<float[]> output,
                                   MsdfShape msdfShape,
                                   SDFTransformation transformation,
                                   GeneratorConfig config) {
        if (config == null) {
            config = new GeneratorConfig();
        }
        // Use SingleDistancePixelConversion because the original C++ used TrueDistanceSelector
        // which corresponds to a single double distance per sample.
        DistancePixelConversion<Double> converter =
                new SingleDistancePixelConversion(transformation.distanceMapping);

        if (config.isOverlapSupport) {
            // OverlappingContourCombiner uses TrueDistanceSelector internally (single-distance)
            generateDistanceField(output,
                    msdfShape,
                    transformation,
                    ContourCombiners.OverlappingContourCombiner.class,
                    converter);
        } else {
            generateDistanceField(output,
                    msdfShape,
                    transformation,
                    ContourCombiners.SimpleContourCombiner.class,
                    converter);
        }
    }
    public static void generateSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Projection projection,
            Range range,
            GeneratorConfig config) {

        DistanceMapping mapping = new DistanceMapping(range.getLower(), range.getUpper());
        SDFTransformation transformation = new SDFTransformation(projection, mapping);

        if (config.isOverlapSupport) {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.OverlappingContourCombiner.class,
                    new SingleDistancePixelConversion(mapping)
            );
        } else {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.SimpleContourCombiner.class,
                    new SingleDistancePixelConversion(mapping)
            );
        }
    }

    public static void generateSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Range range,
            Vector2d scale,
            Vector2d translate,
            boolean overlapSupport) {

        // Wrap scale/translate into a Projection, then delegate to main method
        Projection proj = new Projection(scale, translate);
        GeneratorConfig config = new GeneratorConfig(overlapSupport);
        generateSDF(output, msdfShape, proj, range, config);
    }

    // PSDF Generation
    public static void generatePSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            SDFTransformation transformation,
            GeneratorConfig config) {
        // Use a default DistanceMapping instance here; replace if you have a specific mapping.
        DistanceMapping mapping = new DistanceMapping();

        // Single-channel PSDF uses a single-distance conversion
        SingleDistancePixelConversion converter = new SingleDistancePixelConversion(mapping);

        if (config != null && config.isOverlapSupport) {
            // Overlapping combiner
            generateDistanceField(output, msdfShape, transformation,
                    ContourCombiners.OverlappingContourCombiner.class,
                    converter);
        } else {
            // Simple combiner
            generateDistanceField(output, msdfShape, transformation,
                    ContourCombiners.SimpleContourCombiner.class,
                    converter);
        }
    }

    public static void generatePSDF(
        DprBitmapRef<float[]> output,
        MsdfShape msdfShape,
        SDFTransformation transformation) {
    generatePSDF(output, msdfShape, transformation, new GeneratorConfig());
}

    public static void generatePSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Range range,
            Vector2d scale,
            Vector2d translate,
            boolean overlapSupport) {
        // Construct a Projection from scale & translate
        Projection projection = new Projection(scale, translate);
        // Wrap it in a GeneratorConfig
        GeneratorConfig config = new GeneratorConfig(overlapSupport);
        // Call the main overloaded method
        generatePSDF(output, msdfShape, projection, range, config);
    }

    public static void generatePSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Projection projection,
            Range range,
            GeneratorConfig config) {

        // Create a DistanceMapping from the Range
        DistanceMapping mapping = new DistanceMapping(range.getLower(), range.getUpper()); // <-- your mapping constructor

        // Create the SDFTransformation using the projection and mapping
        SDFTransformation transformation = new SDFTransformation(projection, mapping);

        // Single-channel PSDF uses a single-distance conversion
        SingleDistancePixelConversion converter = new SingleDistancePixelConversion(mapping);

        if (config != null && config.isOverlapSupport) {
            generateDistanceField(output, msdfShape,
                    transformation,
                    ContourCombiners.OverlappingContourCombiner.class,
                    converter);
        } else {
            generateDistanceField(output, msdfShape,
                    transformation,
                    ContourCombiners.SimpleContourCombiner.class,
                    converter);
        }
    }



    // MSDF Generation
    public static void generateMSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            SDFTransformation transformation,
            MSDFGeneratorConfig config) {

        MultiDistancePixelConversion converter = new MultiDistancePixelConversion(transformation.distanceMapping);
        // Choose contour combiner based on overlap support
        if (config.isOverlapSupport) {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.OverlappingMultiContourCombiner.class,
                    converter
            );
        } else {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.SimpleMultiContourCombiner.class,
                    converter
            );
        }

        // Apply MSDF error correction
        DprMSDFErrorCorrection errorCorrection = new DprMSDFErrorCorrection(output, transformation);
        errorCorrection.setMinDeviationRatio(ErrorCorrectionConfig.DEFAULT_MIN_DEVIATION_RATIO);
        errorCorrection.setMinImproveRatio(ErrorCorrectionConfig.DEFAULT_MIN_IMPROVE_RATIO);

        // If shape info is needed for better correction
        errorCorrection.findErrorsWithShape(output, msdfShape);

        // Apply the corrections to the MSDF bitmap
        errorCorrection.apply(output);
    }

    public static void generateMSDF(DprBitmapRef<float[]> output, MsdfShape msdfShape,
                                    SDFTransformation transformation) {
        generateMSDF(output, msdfShape, transformation, new MSDFGeneratorConfig());
    }

    public static void generateMSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Projection projection,
            Range range,
            MSDFGeneratorConfig config) {

        // Build the SDF transformation
        SDFTransformation transformation = new SDFTransformation(projection, new DistanceMapping(range));

        // Choose combiner
        MultiDistancePixelConversion converter =
                new MultiDistancePixelConversion(transformation.distanceMapping);

        if (config.isOverlapSupport) {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.OverlappingMultiContourCombiner.class,
                    converter
            );
        } else {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.SimpleMultiContourCombiner.class,
                    converter
            );
        }

        // ---- MSDF ERROR CORRECTION ----
        DprMSDFErrorCorrection errorCorrection = new DprMSDFErrorCorrection(output, transformation);

        errorCorrection.setMinDeviationRatio(config.errorCorrection.minDeviationRatio);
        errorCorrection.setMinImproveRatio(config.errorCorrection.minImproveRatio);

        errorCorrection.findErrorsWithShape(output, msdfShape);
        errorCorrection.apply(output);
    }

    public static void generateMSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Range range,
            Vector2d scale,
            Vector2d translate,
            ErrorCorrectionConfig errorCorrectionConfig,
            boolean overlapSupport) {

        Projection projection = new Projection(scale, translate);

        MSDFGeneratorConfig config =
                new MSDFGeneratorConfig(overlapSupport, errorCorrectionConfig);

        generateMSDF(output, msdfShape, projection, range, config);
    }

    // MTSDF Generation
    public static void generateMTSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            SDFTransformation transformation,
            MSDFGeneratorConfig config
    ) {
        // Distance conversion for MSDF (4 channels)
        MultiAndTrueDistancePixelConversion converter =
                new MultiAndTrueDistancePixelConversion(new DistanceMapping());

        // Generate the distance field
        if (config.isOverlapSupport) {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.OverlappingMultiAndTrueContourCombiner.class,
                    converter
            );
        } else {
            generateDistanceField(
                    output,
                    msdfShape,
                    transformation,
                    ContourCombiners.SimpleMultiAndTrueContourCombiner.class,
                    converter
            );
        }

        // Apply error correction using your MSDFErrorCorrection class
        DprMSDFErrorCorrection errorCorrection = new DprMSDFErrorCorrection(output, transformation);
        // Set configuration values from MSDFGeneratorConfig if needed
        if (config.errorCorrection != null) {
            errorCorrection.setMinDeviationRatio(ErrorCorrectionConfig.DEFAULT_MIN_DEVIATION_RATIO);
            errorCorrection.setMinImproveRatio(ErrorCorrectionConfig.DEFAULT_MIN_IMPROVE_RATIO);
        }
        // Correct the MSDF bitmap
        errorCorrection.apply(output);
    }

    public static void generateMTSDF(DprBitmapRef<float[]> output, MsdfShape msdfShape,
                                     SDFTransformation transformation) {
        generateMTSDF(output, msdfShape, transformation, new MSDFGeneratorConfig());
    }
    /**
     * Java equivalent of:
     * void generateMTSDF(const BitmapSection<float, 4> &output, const Shape &shape,
     *                    const Projection &projection, Range range, const MSDFGeneratorConfig &config)
     */
    public static void generateMTSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Projection projection,
            Range range,
            MSDFGeneratorConfig config
    ) {
        // Create a DistanceMapping from the Range and then the SDFTransformation.
        // Assumes DistanceMapping has a constructor that accepts Range; adapt if your API differs.
        DistanceMapping mapping = new DistanceMapping(range);
        SDFTransformation transformation = new SDFTransformation(projection, mapping);

        // Delegate to your already-converted implementation
        generateMTSDF(output, msdfShape, transformation, config);
    }

    /**
     * Java equivalent of:
     * void generateMTSDF(const BitmapSection<float,4> &output, const Shape &shape,
     *                    Range range, const Vector2 &scale, const Vector2 &translate,
     *                    const ErrorCorrectionConfig &errorCorrectionConfig, bool overlapSupport)
     */
    public static void generateMTSDF(
            DprBitmapRef<float[]> output,
            MsdfShape msdfShape,
            Range range,
            Vector2d scale,
            Vector2d translate,
            ErrorCorrectionConfig errorCorrectionConfig,
            boolean overlapSupport
    ) {
        // Build a Projection from scale/translate
        Projection projection = new Projection(scale, translate);

        // Build generator config from overlapSupport and errorCorrectionConfig
        MSDFGeneratorConfig genConfig = new MSDFGeneratorConfig(overlapSupport, errorCorrectionConfig);

        // Call the other overload
        generateMTSDF(output, msdfShape, projection, range, genConfig);
    }


    // Legacy API compatibility methods
}