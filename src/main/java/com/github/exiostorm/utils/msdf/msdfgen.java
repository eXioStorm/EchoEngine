package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;
import java.awt.geom.Rectangle2D;

public class msdfgen {

    // Distance pixel conversion classes
    public abstract static class DistancePixelConversion<T> {
        protected DistanceMapping mapping;

        public DistancePixelConversion(DistanceMapping mapping) {
            this.mapping = mapping;
        }

        public abstract void convert(float[] pixels, T distance);
    }

    public static class SingleDistancePixelConversion extends DistancePixelConversion<Double> {
        public SingleDistancePixelConversion(DistanceMapping mapping) {
            super(mapping);
        }

        @Override
        public void convert(float[] pixels, Double distance) {
            pixels[0] = (float) mapping.map(distance);
        }
    }

    public static class MultiDistancePixelConversion extends DistancePixelConversion<EdgeSelectors.MultiDistance> {
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

    public static class MultiAndTrueDistancePixelConversion extends DistancePixelConversion<EdgeSelectors.MultiAndTrueDistance> {
        public MultiAndTrueDistancePixelConversion(DistanceMapping mapping) {
            super(mapping);
        }

        @Override
        public void convert(float[] pixels, EdgeSelectors.MultiAndTrueDistance distance) {
            pixels[0] = (float) mapping.map(distance.r);
            pixels[1] = (float) mapping.map(distance.g);
            pixels[2] = (float) mapping.map(distance.b);
            pixels[3] = (float) mapping.map(distance.a);
        }
    }

    // Generic distance field generation
    private static <T, C extends ContourCombiner<T>> void generateDistanceField(
            BitmapRef<Float> output, Shape shape, SDFTransformation transformation,
            Class<C> combinerClass, DistancePixelConversion<T> converter) {

        try {
            ShapeDistanceFinder<C> distanceFinder = new ShapeDistanceFinder<>(shape, combinerClass);
            boolean rightToLeft = false;

            for (int y = 0; y < output.getHeight(); y++) {
                int row = shape.inverseYAxis ? output.getHeight() - y - 1 : y;
                for (int col = 0; col < output.getWidth(); col++) {
                    int x = rightToLeft ? output.getWidth() - col - 1 : col;
                    Vector2d p = transformation.unproject(new Vector2d(x + 0.5, y + 0.5));
                    T distance = distanceFinder.distance(p);
                    converter.convert(output.getPixel(x, row), distance);
                }
                rightToLeft = !rightToLeft;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating distance field", e);
        }
    }

    // SDF Generation
    public static void generateSDF(BitmapRef<Float> output, Shape shape,
                                   SDFTransformation transformation, GeneratorConfig config) {
        SingleDistancePixelConversion converter = new SingleDistancePixelConversion(transformation.getDistanceMapping());

        if (config.isOverlapSupport()) {
            generateDistanceField(output, shape, transformation,
                    OverlappingContourCombiner.class, converter);
        } else {
            generateDistanceField(output, shape, transformation,
                    SimpleContourCombiner.class, converter);
        }
    }

    public static void generateSDF(BitmapRef<Float> output, Shape shape,
                                   SDFTransformation transformation) {
        generateSDF(output, shape, transformation, new GeneratorConfig());
    }

    // PSDF Generation
    public static void generatePSDF(BitmapRef<Float> output, Shape shape,
                                    SDFTransformation transformation, GeneratorConfig config) {
        SingleDistancePixelConversion converter = new SingleDistancePixelConversion(transformation.getDistanceMapping());

        if (config.isOverlapSupport()) {
            generateDistanceField(output, shape, transformation,
                    OverlappingPerpendicularContourCombiner.class, converter);
        } else {
            generateDistanceField(output, shape, transformation,
                    SimplePerpendicularContourCombiner.class, converter);
        }
    }

    public static void generatePSDF(BitmapRef<Float> output, Shape shape,
                                    SDFTransformation transformation) {
        generatePSDF(output, shape, transformation, new GeneratorConfig());
    }

    // MSDF Generation
    public static void generateMSDF(BitmapRef<Float> output, Shape shape,
                                    SDFTransformation transformation, MSDFGeneratorConfig config) {
        MultiDistancePixelConversion converter = new MultiDistancePixelConversion(transformation.getDistanceMapping());

        if (config.isOverlapSupport()) {
            generateDistanceField(output, shape, transformation,
                    OverlappingMultiContourCombiner.class, converter);
        } else {
            generateDistanceField(output, shape, transformation,
                    SimpleMultiContourCombiner.class, converter);
        }

        MsdfErrorCorrection.correct(output, shape, transformation, config);
    }

    public static void generateMSDF(BitmapRef<Float> output, Shape shape,
                                    SDFTransformation transformation) {
        generateMSDF(output, shape, transformation, new MSDFGeneratorConfig());
    }

    // MTSDF Generation
    public static void generateMTSDF(BitmapRef<Float> output, Shape shape,
                                     SDFTransformation transformation, MSDFGeneratorConfig config) {
        MultiAndTrueDistancePixelConversion converter = new MultiAndTrueDistancePixelConversion(transformation.getDistanceMapping());

        if (config.isOverlapSupport()) {
            generateDistanceField(output, shape, transformation,
                    OverlappingMultiAndTrueContourCombiner.class, converter);
        } else {
            generateDistanceField(output, shape, transformation,
                    SimpleMultiAndTrueContourCombiner.class, converter);
        }

        MsdfErrorCorrection.correct(output, shape, transformation, config);
    }

    public static void generateMTSDF(BitmapRef<Float> output, Shape shape,
                                     SDFTransformation transformation) {
        generateMTSDF(output, shape, transformation, new MSDFGeneratorConfig());
    }

    // Overloaded methods using Projection and Range
    public static void generateSDF(BitmapRef<Float> output, Shape shape,
                                   Projection projection, Range range, GeneratorConfig config) {
        generateSDF(output, shape, new SDFTransformation(projection, range), config);
    }

    public static void generateSDF(BitmapRef<Float> output, Shape shape,
                                   Projection projection, Range range) {
        generateSDF(output, shape, projection, range, new GeneratorConfig());
    }

    public static void generatePSDF(BitmapRef<Float> output, Shape shape,
                                    Projection projection, Range range, GeneratorConfig config) {
        generatePSDF(output, shape, new SDFTransformation(projection, range), config);
    }

    public static void generatePSDF(BitmapRef<Float> output, Shape shape,
                                    Projection projection, Range range) {
        generatePSDF(output, shape, projection, range, new GeneratorConfig());
    }

    public static void generateMSDF(BitmapRef<Float> output, Shape shape,
                                    Projection projection, Range range, MSDFGeneratorConfig config) {
        generateMSDF(output, shape, new SDFTransformation(projection, range), config);
    }

    public static void generateMSDF(BitmapRef<Float> output, Shape shape,
                                    Projection projection, Range range) {
        generateMSDF(output, shape, projection, range, new MSDFGeneratorConfig());
    }

    public static void generateMTSDF(BitmapRef<Float> output, Shape shape,
                                     Projection projection, Range range, MSDFGeneratorConfig config) {
        generateMTSDF(output, shape, new SDFTransformation(projection, range), config);
    }

    public static void generateMTSDF(BitmapRef<Float> output, Shape shape,
                                     Projection projection, Range range) {
        generateMTSDF(output, shape, projection, range, new MSDFGeneratorConfig());
    }

    // Legacy API compatibility methods
    public static void generatePseudoSDF(BitmapRef<Float> output, Shape shape,
                                         Projection projection, Range range, GeneratorConfig config) {
        generatePSDF(output, shape, new SDFTransformation(projection, range), config);
    }

    public static void generatePseudoSDF(BitmapRef<Float> output, Shape shape,
                                         Projection projection, Range range) {
        generatePseudoSDF(output, shape, projection, range, new GeneratorConfig());
    }

    public static void generateSDF(BitmapRef<Float> output, Shape shape, Range range,
                                   Vector2d scale, Vector2d translate, boolean overlapSupport) {
        generateSDF(output, shape, new Projection(scale, translate), range,
                new GeneratorConfig(overlapSupport));
    }

    public static void generateSDF(BitmapRef<Float> output, Shape shape, Range range,
                                   Vector2d scale, Vector2d translate) {
        generateSDF(output, shape, range, scale, translate, true);
    }

    public static void generatePSDF(BitmapRef<Float> output, Shape shape, Range range,
                                    Vector2d scale, Vector2d translate, boolean overlapSupport) {
        generatePSDF(output, shape, new Projection(scale, translate), range,
                new GeneratorConfig(overlapSupport));
    }

    public static void generatePSDF(BitmapRef<Float> output, Shape shape, Range range,
                                    Vector2d scale, Vector2d translate) {
        generatePSDF(output, shape, range, scale, translate, true);
    }

    public static void generatePseudoSDF(BitmapRef<Float> output, Shape shape, Range range,
                                         Vector2d scale, Vector2d translate, boolean overlapSupport) {
        generatePSDF(output, shape, new Projection(scale, translate), range,
                new GeneratorConfig(overlapSupport));
    }

    public static void generatePseudoSDF(BitmapRef<Float> output, Shape shape, Range range,
                                         Vector2d scale, Vector2d translate) {
        generatePseudoSDF(output, shape, range, scale, translate, true);
    }

    public static void generateMSDF(BitmapRef<Float> output, Shape shape, Range range,
                                    Vector2d scale, Vector2d translate,
                                    ErrorCorrectionConfig errorCorrectionConfig, boolean overlapSupport) {
        generateMSDF(output, shape, new Projection(scale, translate), range,
                new MSDFGeneratorConfig(overlapSupport, errorCorrectionConfig));
    }

    public static void generateMSDF(BitmapRef<Float> output, Shape shape, Range range,
                                    Vector2d scale, Vector2d translate, ErrorCorrectionConfig errorCorrectionConfig) {
        generateMSDF(output, shape, range, scale, translate, errorCorrectionConfig, true);
    }

    public static void generateMSDF(BitmapRef<Float> output, Shape shape, Range range,
                                    Vector2d scale, Vector2d translate) {
        generateMSDF(output, shape, range, scale, translate, new ErrorCorrectionConfig(), true);
    }

    public static void generateMTSDF(BitmapRef<Float> output, Shape shape, Range range,
                                     Vector2d scale, Vector2d translate,
                                     ErrorCorrectionConfig errorCorrectionConfig, boolean overlapSupport) {
        generateMTSDF(output, shape, new Projection(scale, translate), range,
                new MSDFGeneratorConfig(overlapSupport, errorCorrectionConfig));
    }

    public static void generateMTSDF(BitmapRef<Float> output, Shape shape, Range range,
                                     Vector2d scale, Vector2d translate, ErrorCorrectionConfig errorCorrectionConfig) {
        generateMTSDF(output, shape, range, scale, translate, errorCorrectionConfig, true);
    }

    public static void generateMTSDF(BitmapRef<Float> output, Shape shape, Range range,
                                     Vector2d scale, Vector2d translate) {
        generateMTSDF(output, shape, range, scale, translate, new ErrorCorrectionConfig(), true);
    }

    // Legacy implementation methods
    public static void generateSDF_legacy(BitmapRef<Float> output, Shape shape, Range range,
                                          Vector2d scale, Vector2d translate) {
        DistanceMapping distanceMapping = new DistanceMapping(range);

        for (int y = 0; y < output.getHeight(); y++) {
            int row = shape.inverseYAxis ? output.getHeight() - y - 1 : y;
            for (int x = 0; x < output.getWidth(); x++) {
                Vector2d p = new Vector2d(x + 0.5, y + 0.5).div(scale).sub(translate);
                SignedDistance minDistance = new SignedDistance();

                for (Contours.Contour contour : shape.contours) {
                    for (EdgeHolder edge : contour.edges) {
                        DoubleReference dummy = new DoubleReference();
                        SignedDistance distance = edge.edge.signedDistance(p, new double[] {dummy.getValue()});
                        if (distance.compareTo(minDistance) < 0) {
                            minDistance = distance;
                        }
                    }
                }

                output.getPixel(x, row)[0] = (float) distanceMapping.map(minDistance.distance);
            }
        }
    }

    public static void generatePSDF_legacy(BitmapRef<Float> output, Shape shape, Range range,
                                           Vector2d scale, Vector2d translate) {
        DistanceMapping distanceMapping = new DistanceMapping(range);

        for (int y = 0; y < output.getHeight(); y++) {
            int row = shape.inverseYAxis ? output.getHeight() - y - 1 : y;
            for (int x = 0; x < output.getWidth(); x++) {
                Vector2d p = new Vector2d(x + 0.5, y + 0.5).div(scale).sub(translate);
                SignedDistance minDistance = new SignedDistance();
                EdgeHolder nearEdge = null;
                double nearParam = 0;

                for (Contours.Contour contour : shape.contours) {
                    for (EdgeHolder edge : contour.edges) {
                        DoubleReference param = new DoubleReference();
                        SignedDistance distance = edge.edge.signedDistance(p, new double[]{param.getValue()});
                        if (distance.compareTo(minDistance) < 0) {
                            minDistance = distance;
                            nearEdge = edge;
                            nearParam = param.getValue();
                        }
                    }
                }

                if (nearEdge != null) {
                    nearEdge.distanceToPerpendicularDistance(minDistance, p, nearParam);
                }

                output.getPixel(x, row)[0] = (float) distanceMapping.map(minDistance.distance);
            }
        }
    }

    public static void generatePseudoSDF_legacy(BitmapRef<Float> output, Shape shape, Range range,
                                                Vector2d scale, Vector2d translate) {
        generatePSDF_legacy(output, shape, range, scale, translate);
    }

    public static void generateMSDF_legacy(BitmapRef<Float> output, Shape shape, Range range,
                                           Vector2d scale, Vector2d translate,
                                           ErrorCorrectionConfig errorCorrectionConfig) {
        DistanceMapping distanceMapping = new DistanceMapping(range);

        for (int y = 0; y < output.getHeight(); y++) {
            int row = shape.inverseYAxis ? output.getHeight() - y - 1 : y;
            for (int x = 0; x < output.getWidth(); x++) {
                Vector2d p = new Vector2d(x + 0.5, y + 0.5).div(scale).sub(translate);

                // Structure to hold RGB channel data
                class ChannelData {
                    SignedDistance minDistance = new SignedDistance();
                    EdgeHolder nearEdge = null;
                    double nearParam = 0;
                }

                ChannelData r = new ChannelData();
                ChannelData g = new ChannelData();
                ChannelData b = new ChannelData();

                for (Contours.Contour contour : shape.contours) {
                    for (EdgeHolder edge : contour.edges) {
                        DoubleReference param = new DoubleReference();
                        SignedDistance distance = edge.edge.signedDistance(p, new double[] {param.getValue()});

                        if ((edge.color & EdgeColorEnum.RED.getValue().color) != 0 &&
                                distance.compareTo(r.minDistance) < 0) {
                            r.minDistance = distance;
                            r.nearEdge = edge;
                            r.nearParam = param.getValue();
                        }
                        if ((edge.color & EdgeColorEnum.GREEN.getValue().color) != 0 &&
                                distance.compareTo(g.minDistance) < 0) {
                            g.minDistance = distance;
                            g.nearEdge = edge;
                            g.nearParam = param.getValue();
                        }
                        if ((edge.color & EdgeColorEnum.BLUE.getValue().color) != 0 &&
                                distance.compareTo(b.minDistance) < 0) {
                            b.minDistance = distance;
                            b.nearEdge = edge;
                            b.nearParam = param.getValue();
                        }
                    }
                }

                if (r.nearEdge != null) {
                    r.nearEdge.distanceToPerpendicularDistance(r.minDistance, p, r.nearParam);
                }
                if (g.nearEdge != null) {
                    g.nearEdge.distanceToPerpendicularDistance(g.minDistance, p, g.nearParam);
                }
                if (b.nearEdge != null) {
                    b.nearEdge.distanceToPerpendicularDistance(b.minDistance, p, b.nearParam);
                }

                float[] pixel = output.getPixel(x, row);
                pixel[0] = (float) distanceMapping.map(r.minDistance.distance);
                pixel[1] = (float) distanceMapping.map(g.minDistance.distance);
                pixel[2] = (float) distanceMapping.map(b.minDistance.distance);
            }
        }

        errorCorrectionConfig.setDistanceCheckMode(ErrorCorrectionConfig.DistanceCheckMode.DO_NOT_CHECK_DISTANCE);
        MsdfErrorCorrection.correct(output, shape, new Projection(scale, translate), range,
                new MSDFGeneratorConfig(false, errorCorrectionConfig));
    }

    public static void generateMSDF_legacy(BitmapRef<Float> output, Shape shape, Range range,
                                           Vector2d scale, Vector2d translate) {
        generateMSDF_legacy(output, shape, range, scale, translate, new ErrorCorrectionConfig());
    }

    public static void generateMTSDF_legacy(BitmapRef<Float> output, Shape shape, Range range,
                                            Vector2d scale, Vector2d translate,
                                            ErrorCorrectionConfig errorCorrectionConfig) {
        DistanceMapping distanceMapping = new DistanceMapping(range);

        for (int y = 0; y < output.getHeight(); y++) {
            int row = shape.inverseYAxis ? output.getHeight() - y - 1 : y;
            for (int x = 0; x < output.getWidth(); x++) {
                Vector2d p = new Vector2d(x + 0.5, y + 0.5).div(scale).sub(translate);

                SignedDistance minDistance = new SignedDistance();

                // Structure to hold RGB channel data
                class ChannelData {
                    SignedDistance minDistance = new SignedDistance();
                    EdgeHolder nearEdge = null;
                    double nearParam = 0;
                }

                ChannelData r = new ChannelData();
                ChannelData g = new ChannelData();
                ChannelData b = new ChannelData();

                for (Contours.Contour contour : shape.contours) {
                    for (EdgeHolder edge : contour.edges) {
                        DoubleReference param = new DoubleReference();
                        SignedDistance distance = edge.edge.signedDistance(p, new double[] {param.getValue()});

                        if (distance.compareTo(minDistance) < 0) {
                            minDistance = distance;
                        }
                        if ((edge.color & EdgeColorEnum.RED.getValue().color) != 0 &&
                                distance.compareTo(r.minDistance) < 0) {
                            r.minDistance = distance;
                            r.nearEdge = edge;
                            r.nearParam = param.getValue();
                        }
                        if ((edge.color & EdgeColorEnum.GREEN.getValue().color) != 0 &&
                                distance.compareTo(g.minDistance) < 0) {
                            g.minDistance = distance;
                            g.nearEdge = edge;
                            g.nearParam = param.getValue();
                        }
                        if ((edge.color & EdgeColorEnum.BLUE.getValue().color) != 0 &&
                                distance.compareTo(b.minDistance) < 0) {
                            b.minDistance = distance;
                            b.nearEdge = edge;
                            b.nearParam = param.getValue();
                        }
                    }
                }

                if (r.nearEdge != null) {
                    r.nearEdge.distanceToPerpendicularDistance(r.minDistance, p, r.nearParam);
                }
                if (g.nearEdge != null) {
                    g.nearEdge.distanceToPerpendicularDistance(g.minDistance, p, g.nearParam);
                }
                if (b.nearEdge != null) {
                    b.nearEdge.distanceToPerpendicularDistance(b.minDistance, p, b.nearParam);
                }

                float[] pixel = output.getPixel(x, row);
                pixel[0] = (float) distanceMapping.map(r.minDistance.distance);
                pixel[1] = (float) distanceMapping.map(g.minDistance.distance);
                pixel[2] = (float) distanceMapping.map(b.minDistance.distance);
                pixel[3] = (float) distanceMapping.map(minDistance.distance);
            }
        }

        errorCorrectionConfig.setDistanceCheckMode(ErrorCorrectionConfig.DistanceCheckMode.DO_NOT_CHECK_DISTANCE);
        MsdfErrorCorrection.correct(output, shape, new Projection(scale, translate), range,
                new MSDFGeneratorConfig(false, errorCorrectionConfig));
    }

    public static void generateMTSDF_legacy(BitmapRef<Float> output, Shape shape, Range range,
                                            Vector2d scale, Vector2d translate) {
        generateMTSDF_legacy(output, shape, range, scale, translate, new ErrorCorrectionConfig());
    }
}