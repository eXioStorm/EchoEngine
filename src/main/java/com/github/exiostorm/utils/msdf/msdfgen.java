package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

public class msdfgen {
    /**
     * Main generateMSDF implementation - generates a multi-channel signed distance field.
     * Edge colors must be assigned first! (See edgeColoringSimple)
     *
     * C++ equivalent (lines 94-100):
     * void generateMSDF(const BitmapSection<float, 3> &output, const Shape &shape,
     *                   const SDFTransformation &transformation, const MSDFGeneratorConfig &config)
     *
     * @param output The output bitmap (3 channels: red, green, blue)
     * @param shape The shape to generate MSDF from
     * @param transformation The SDF transformation parameters
     * @param config The MSDF generator configuration
     */
    public static void generateMSDF(BitmapRef<float[]> output,
                                    MsdfShape shape,
                                    SDFTransformation transformation,
                                    MSDFGeneratorConfig config) {
        // Choose the appropriate contour combiner based on overlap support
        if (config.isOverlapSupport) {
            // Use OverlappingContourCombiner for shapes with overlapping contours
            generateDistanceFieldOverlapping(output, shape, transformation);
        } else {
            // Use SimpleContourCombiner for simple shapes
            generateDistanceFieldSimple(output, shape, transformation);
        }

        // Apply error correction to improve quality
        msdfErrorCorrection(output, shape, transformation, config);
    }

    /**
     * Overloaded version using Projection and Range parameters.
     *
     * C++ equivalent (lines 124-129):
     * void generateMSDF(const BitmapSection<float, 3> &output, const Shape &shape,
     *                   const Projection &projection, Range range, const MSDFGeneratorConfig &config)
     *
     * @param output The output bitmap
     * @param shape The shape to generate MSDF from
     * @param projection The projection transformation
     * @param range The distance range
     * @param config The MSDF generator configuration
     */
    public static void generateMSDF(BitmapRef<float[]> output,
                                    MsdfShape shape,
                                    Projection projection,
                                    Range range,
                                    MSDFGeneratorConfig config) {
        // Construct SDFTransformation from projection and range, then call main method
        SDFTransformation transformation = new SDFTransformation(projection, new DistanceMapping(range));
        generateMSDF(output, shape, transformation, config);
    }

    /**
     * Legacy API version with separate scale and translate parameters.
     *
     * C++ equivalent (lines 156-158):
     * void generateMSDF(const BitmapSection<float, 3> &output, const Shape &shape, Range range,
     *                   const Vector2 &scale, const Vector2 &translate,
     *                   const ErrorCorrectionConfig &errorCorrectionConfig, bool overlapSupport)
     *
     * @param output The output bitmap
     * @param shape The shape to generate MSDF from
     * @param range The distance range
     * @param scale The scale vector
     * @param translate The translation vector
     * @param errorCorrectionConfig The error correction configuration
     * @param overlapSupport Whether to support overlapping contours
     */
    public static void generateMSDF(BitmapRef<float[]> output,
                                    MsdfShape shape,
                                    Range range,
                                    Vector2d scale,
                                    Vector2d translate,
                                    ErrorCorrectionConfig errorCorrectionConfig,
                                    boolean overlapSupport) {
        // Construct Projection and MSDFGeneratorConfig, then call main method
        Projection projection = new Projection(scale, translate);
        MSDFGeneratorConfig config = new MSDFGeneratorConfig(overlapSupport, errorCorrectionConfig);
        generateMSDF(output, shape, projection, range, config);
    }

    private static void msdfErrorCorrection(BitmapRef<float[]> output,
                                            MsdfShape shape,
                                            SDFTransformation transformation,
                                            MSDFGeneratorConfig config) {
        // Check if error correction mode is set to DO_NOT_CHECK_DISTANCE
        if (config.errorCorrection.distanceCheckMode ==
                ErrorCorrectionConfig.DistanceCheckMode.DO_NOT_CHECK_DISTANCE) {
            // Simple error correction without distance checking

            // Create stencil bitmap for marking errors
            BitmapRef<byte[]> stencil = new BitmapRef<>(
                    new byte[output.width * output.height],
                    output.width,
                    output.height
            );

            // Create error correction instance
            MSDFErrorCorrection errorCorrection = new MSDFErrorCorrection(stencil, transformation);
            errorCorrection.setMinDeviationRatio(config.errorCorrection.minDeviationRatio);
            errorCorrection.setMinImproveRatio(config.errorCorrection.minImproveRatio);

            // Protect corners and edges if requested
            /*TODO 20251229 missing logic for this
            if (config.errorCorrection.mode.protectsCorners()) {
                errorCorrection.protectCorners(shape);
            }
            if (config.errorCorrection.mode.protectsEdges()) {
                errorCorrection.protectEdges(output);
            }
             */
            // Find errors
            errorCorrection.findErrors(output);

            // Apply corrections
            errorCorrection.apply(output);
        } else {
            // Error correction with distance checking

            // Create stencil bitmap for marking errors
            BitmapRef<byte[]> stencil = new BitmapRef<>(
                    new byte[output.width * output.height],
                    output.width,
                    output.height
            );

            // Create error correction instance
            MSDFErrorCorrection errorCorrection = new MSDFErrorCorrection(stencil, transformation);
            errorCorrection.setMinDeviationRatio(config.errorCorrection.minDeviationRatio);
            errorCorrection.setMinImproveRatio(config.errorCorrection.minImproveRatio);

            // Protect corners and edges if requested
            /*TODO 20251229 missing logic for this
            if (config.errorCorrection.mode.protectsCorners()) {
                errorCorrection.protectCorners(shape);
            }
            if (config.errorCorrection.mode.protectsEdges()) {
                errorCorrection.protectEdges(output);
            }
             */
            // Find errors with shape distance checking
            ContourCombiners.SimpleMultiContourCombiner contourCombiner =
                    new ContourCombiners.SimpleMultiContourCombiner(shape);
            errorCorrection.findErrors(output, shape, contourCombiner);

            // Apply corrections
            errorCorrection.apply(output);
        }
    }
    /**
     * Helper method: Generate distance field using SimpleContourCombiner with MultiDistanceSelector.
     * This is the template instantiation from C++ line 97.
     * 
     * C++ equivalent (lines 54-71):
     * generateDistanceField<SimpleContourCombiner<MultiDistanceSelector>>(output, shape, transformation)
     *
     * @param output The output bitmap
     * @param shape The shape to process
     * @param transformation The SDF transformation
     */
    private static void generateDistanceFieldSimple(BitmapRef<float[]> output,
                                                     MsdfShape shape,
                                                     SDFTransformation transformation) {
        // Create the distance mapping for converting distances to pixel values
        DistanceMapping distanceMapping = transformation.distanceMapping;
        
        // Reorient the output bitmap to match the shape's Y-axis orientation
        output.reorient(shape.getYAxisOrientation());
        
        // Create contour combiner (simple version)
        ContourCombiners.SimpleMultiContourCombiner contourCombiner = 
            new ContourCombiners.SimpleMultiContourCombiner(shape);
        
        // Create the shape distance finder
        ShapeDistanceFinder<ContourCombiners.SimpleMultiContourCombiner, EdgeSelectors.MultiDistance> distanceFinder =
            new ShapeDistanceFinder<>(shape, contourCombiner);
        
        // Process each row with zigzag pattern (for better cache performance)
        int xDirection = 1;
        for (int y = 0; y < output.height; ++y) {
            // Start from left or right depending on direction
            int x = xDirection < 0 ? output.width - 1 : 0;
            
            for (int col = 0; col < output.width; ++col) {
                // Calculate the position in shape coordinates (center of pixel)
                Vector2d p = transformation.unproject(new Vector2d(x + 0.5, y + 0.5));
                
                // Calculate the distance at this point
                EdgeSelectors.MultiDistance distance = distanceFinder.distance(p);
                
                // Convert distance to pixel values and write to output
                int pixelIndex = output.sectionOperator(x, y);
                output.pixels[pixelIndex + 0] = (float) distanceMapping.map(distance.c);
                output.pixels[pixelIndex + 1] = (float) distanceMapping.map(distance.m);
                output.pixels[pixelIndex + 2] = (float) distanceMapping.map(distance.y);
                
                // Move to next pixel
                x += xDirection;
            }
            
            // Reverse direction for next row (zigzag pattern)
            xDirection = -xDirection;
        }
    }

    /**
     * Helper method: Generate distance field using OverlappingContourCombiner with MultiDistanceSelector.
     * This is the template instantiation from C++ line 96.
     * 
     * C++ equivalent (lines 54-71):
     * generateDistanceField<OverlappingContourCombiner<MultiDistanceSelector>>(output, shape, transformation)
     *
     * @param output The output bitmap
     * @param shape The shape to process
     * @param transformation The SDF transformation
     */
    private static void generateDistanceFieldOverlapping(BitmapRef<float[]> output,
                                                          MsdfShape shape,
                                                          SDFTransformation transformation) {
        // Create the distance mapping for converting distances to pixel values
        DistanceMapping distanceMapping = transformation.distanceMapping;
        
        // Reorient the output bitmap to match the shape's Y-axis orientation
        output.reorient(shape.getYAxisOrientation());
        
        // Create contour combiner (overlapping version)
        ContourCombiners.OverlappingMultiContourCombiner contourCombiner =
            new ContourCombiners.OverlappingMultiContourCombiner(shape);
        
        // Create the shape distance finder
        ShapeDistanceFinder<ContourCombiners.OverlappingMultiContourCombiner, EdgeSelectors.MultiDistance> distanceFinder =
            new ShapeDistanceFinder<>(shape, contourCombiner);
        
        // Process each row with zigzag pattern (for better cache performance)
        int xDirection = 1;
        for (int y = 0; y < output.height; ++y) {
            // Start from left or right depending on direction
            int x = xDirection < 0 ? output.width - 1 : 0;
            
            for (int col = 0; col < output.width; ++col) {
                // Calculate the position in shape coordinates (center of pixel)
                Vector2d p = transformation.unproject(new Vector2d(x + 0.5, y + 0.5));
                
                // Calculate the distance at this point
                EdgeSelectors.MultiDistance distance = distanceFinder.distance(p);
                
                // Convert distance to pixel values and write to output
                int pixelIndex = output.sectionOperator(x, y);
                output.pixels[pixelIndex + 0] = (float) distanceMapping.map(distance.c);
                output.pixels[pixelIndex + 1] = (float) distanceMapping.map(distance.m);
                output.pixels[pixelIndex + 2] = (float) distanceMapping.map(distance.y);
                
                // Move to next pixel
                x += xDirection;
            }
            
            // Reverse direction for next row (zigzag pattern)
            xDirection = -xDirection;
        }
    }
    public static void generateSDF(
            BitmapRef<float[]> output,
            MsdfShape msdfShape,
            Projection projection,
            Range range,
            GeneratorConfig config) {

        DistanceMapping mapping = new DistanceMapping(range.getLower(), range.getUpper());
        SDFTransformation transformation = new SDFTransformation(projection, mapping);

        if (config.isOverlapSupport) {
            generateDistanceFieldOverlapping(
                    output,
                    msdfShape,
                    transformation
            );
        } else {
            generateDistanceFieldSimple(
                    output,
                    msdfShape,
                    transformation
            );
        }
    }

    public static void generateSDF(
            BitmapRef<float[]> output,
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
}
