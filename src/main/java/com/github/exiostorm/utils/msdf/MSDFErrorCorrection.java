package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import static com.github.exiostorm.utils.msdf.MathUtils.*;
import org.joml.Vector2d;
import java.util.Arrays;

/**
 * Performs error correction on a computed MSDF to eliminate interpolation artifacts.
 * This is a low-level class, you may want to use the API in msdf-error-correction.h instead.
 */
public class MSDFErrorCorrection {

    private static final double ARTIFACT_T_EPSILON = 0.01;
    private static final double PROTECTION_RADIUS_TOLERANCE = 1.001;

    private static final int CLASSIFIER_FLAG_CANDIDATE = 0x01;
    private static final int CLASSIFIER_FLAG_ARTIFACT = 0x02;

    /// Stencil flags.
    public static final class Flags {
        /// Texel marked as potentially causing interpolation errors.
        public static final byte ERROR = 1;
        /// Texel marked as protected. Protected texels are only given the error flag if they cause inversion artifacts.
        public static final byte PROTECTED = 2;
    }

    public static final class ErrorCorrectionConfig {
        public static final double DEFAULT_MIN_DEVIATION_RATIO = 1.11111111111111111;
        public static final double DEFAULT_MIN_IMPROVE_RATIO = 1.11111111111111111;
    }

    /// The base artifact classifier recognizes artifacts based on the contents of the SDF alone.
    private static class BaseArtifactClassifier {
        private final double span;
        private final boolean protectedFlag;

        public BaseArtifactClassifier(double span, boolean protectedFlag) {
            this.span = span;
            this.protectedFlag = protectedFlag;
        }

        /// Evaluates if the median value xm interpolated at xt in the range between am at at and bm at bt indicates an artifact.
        public int rangeTest(double at, double bt, double xt, float am, float bm, float xm) {
            // For protected texels, only consider inversion artifacts (interpolated median has different sign than boundaries).
            // For the rest, it is sufficient that the interpolated median is outside its boundaries.
            if ((am > 0.5f && bm > 0.5f && xm <= 0.5f) || (am < 0.5f && bm < 0.5f && xm >= 0.5f) ||
                    (!protectedFlag && median(am, bm, xm) != xm)) {
                double axSpan = (xt - at) * span;
                double bxSpan = (bt - xt) * span;
                // Check if the interpolated median's value is in the expected range based on its distance (span) from boundaries a, b.
                if (!(xm >= am - axSpan && xm <= am + axSpan && xm >= bm - bxSpan && xm <= bm + bxSpan)) {
                    return CLASSIFIER_FLAG_CANDIDATE | CLASSIFIER_FLAG_ARTIFACT;
                }
                return CLASSIFIER_FLAG_CANDIDATE;
            }
            return 0;
        }

        /// Returns true if the combined results of the tests performed on the median value m interpolated at t indicate an artifact.
        public boolean evaluate(double t, float m, int flags) {
            return (flags & 2) != 0;
        }
    }

    /// The shape distance checker evaluates the exact shape distance to find additional artifacts at a significant performance cost.
    private static class ShapeDistanceChecker<N extends Number> {

        public static class ArtifactClassifier extends BaseArtifactClassifier {
            private final ShapeDistanceChecker parent;
            private final Vector2d direction;

            public ArtifactClassifier(ShapeDistanceChecker parent, Vector2d direction, double span) {
                super(span, parent.protectedFlag);
                this.parent = parent;
                this.direction = new Vector2d(direction);
            }

            @Override
            public boolean evaluate(double t, float m, int flags) {
                if ((flags & CLASSIFIER_FLAG_CANDIDATE) != 0) {
                    // Skip expensive distance evaluation if the point has already been classified as an artifact by the base classifier.
                    if ((flags & CLASSIFIER_FLAG_ARTIFACT) != 0) {
                        return true;
                    }
                    Vector2d tVector = new Vector2d(direction).mul(t);
                    float[] oldMSD = new float[3];
                    float[] newMSD = new float[3];

                    // Compute the color that would be currently interpolated at the artifact candidate's position.
                    Vector2d sdfCoord = new Vector2d(parent.sdfCoord).add(tVector);
                    interpolate(oldMSD, parent.sdf, sdfCoord);

                    // Compute the color that would be interpolated at the artifact candidate's position if error correction was applied on the current texel.
                    double aWeight = (1 - Math.abs(tVector.x)) * (1 - Math.abs(tVector.y));
                    float aPSD = median(parent.msd[0], parent.msd[1], parent.msd[2]);
                    newMSD[0] = (float)(oldMSD[0] + aWeight * (aPSD - parent.msd[0]));
                    newMSD[1] = (float)(oldMSD[1] + aWeight * (aPSD - parent.msd[1]));
                    newMSD[2] = (float)(oldMSD[2] + aWeight * (aPSD - parent.msd[2]));

                    // Compute the evaluated distance (interpolated median) before and after error correction, as well as the exact shape distance.
                    float oldPSD = median(oldMSD[0], oldMSD[1], oldMSD[2]);
                    float newPSD = median(newMSD[0], newMSD[1], newMSD[2]);
                    Vector2d shapeCoordOffset = new Vector2d(tVector).mul(parent.texelSize);
                    float refPSD = (float)parent.distanceMapping.map(parent.distanceFinder.distance(new Vector2d(parent.shapeCoord).add(shapeCoordOffset)));

                    // Compare the differences of the exact distance and the before and after distances.
                    return parent.minImproveRatio * Math.abs(newPSD - refPSD) < Math.abs(oldPSD - refPSD);
                }
                return false;
            }
        }

        public Vector2d shapeCoord;
        public Vector2d sdfCoord;
        public float[] msd;
        public boolean protectedFlag;

        private final ShapeDistanceFinder distanceFinder;
        private final BitmapConstRef sdf;
        private final DistanceMapping distanceMapping;
        private Vector2d texelSize;
        private final double minImproveRatio;

        public ShapeDistanceChecker(BitmapConstRef sdf, Shape shape, Projection projection,
                                    DistanceMapping distanceMapping, double minImproveRatio) {
            this.distanceFinder = new ShapeDistanceFinder(shape);
            this.sdf = sdf;
            this.distanceMapping = distanceMapping;
            this.minImproveRatio = minImproveRatio;
            this.texelSize = projection.unprojectVector(new Vector2d(1.0, 1.0));
            if (shape.inverseYAxis) {
                this.texelSize.y = -this.texelSize.y;
            }
        }

        public ArtifactClassifier classifier(Vector2d direction, double span) {
            return new ArtifactClassifier(this, direction, span);
        }

        private static void interpolate(float[] result, BitmapConstRef bitmap, Vector2d coord) {
            // Bilinear interpolation implementation
            // This would need to be implemented based on your bitmap interpolation logic
            // For now, this is a placeholder
        }
    }

    private BitmapRef stencil;
    private SDFTransformation transformation;
    private double minDeviationRatio;
    private double minImproveRatio;

    public MSDFErrorCorrection() {
        this.minDeviationRatio = ErrorCorrectionConfig.DEFAULT_MIN_DEVIATION_RATIO;
        this.minImproveRatio = ErrorCorrectionConfig.DEFAULT_MIN_IMPROVE_RATIO;
    }

    public MSDFErrorCorrection(BitmapRef stencil, SDFTransformation transformation) {
        this.stencil = stencil;
        this.transformation = transformation;
        this.minDeviationRatio = ErrorCorrectionConfig.DEFAULT_MIN_DEVIATION_RATIO;
        this.minImproveRatio = ErrorCorrectionConfig.DEFAULT_MIN_IMPROVE_RATIO;

        // Clear the stencil
        if (stencil != null && stencil.getPixels() != null) {
            Arrays.fill(stencil.getPixels(), (byte)0);
        }
    }

    /// Sets the minimum ratio between the actual and maximum expected distance delta to be considered an error.
    public void setMinDeviationRatio(double minDeviationRatio) {
        this.minDeviationRatio = minDeviationRatio;
    }

    /// Sets the minimum ratio between the pre-correction distance error and the post-correction distance error.
    public void setMinImproveRatio(double minImproveRatio) {
        this.minImproveRatio = minImproveRatio;
    }

    /// Flags all texels that are interpolated at corners as protected.
    public void protectCorners(Shape shape) {
        for (Contours.Contour contour : shape.contours) {
            if (!contour.edges.isEmpty()) {
                EdgeSegment prevEdge = contour.edges.get(contour.edges.size() - 1).edge;
                for (EdgeHolder edge : contour.edges) {
                    int commonColor = prevEdge.edgeColor.color & edge.edge.edgeColor.color;
                    // If the color changes from prevEdge to edge, this is a corner.
                    if ((commonColor & (commonColor - 1)) == 0) {
                        // Find the four texels that envelop the corner and mark them as protected.
                        Vector2d p = transformation.project(edge.edge.point(0));
                        int l = (int) Math.floor(p.x - 0.5);
                        int b = (int) Math.floor(p.y - 0.5);
                        if (shape.inverseYAxis) {
                            b = stencil.getHeight() - b - 2;
                        }
                        int r = l + 1;
                        int t = b + 1;
                        // Check that the positions are within bounds.
                        if (l < stencil.getWidth() && b < stencil.getHeight() && r >= 0 && t >= 0) {
                            if (l >= 0 && b >= 0) {
                                stencil.setPixel(l, b, (byte)(stencil.getPixel(l, b) | Flags.PROTECTED));
                            }
                            if (r < stencil.getWidth() && b >= 0) {
                                stencil.setPixel(r, b, (byte)(stencil.getPixel(r, b) | Flags.PROTECTED));
                            }
                            if (l >= 0 && t < stencil.getHeight()) {
                                stencil.setPixel(l, t, (byte)(stencil.getPixel(l, t) | Flags.PROTECTED));
                            }
                            if (r < stencil.getWidth() && t < stencil.getHeight()) {
                                stencil.setPixel(r, t, (byte)(stencil.getPixel(r, t) | Flags.PROTECTED));
                            }
                        }
                    }
                    prevEdge = edge.edge;
                }
            }
        }
    }

    /// Flags all texels that contribute to edges as protected.
    public void protectEdges(BitmapConstRef sdf) {
        float radius;

        // Horizontal texel pairs
        radius = (float)(PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(transformation.getDistanceMapping().map(1.0), 0)).length());

        for (int y = 0; y < sdf.getHeight(); ++y) {
            for (int x = 0; x < sdf.getWidth() - 1; ++x) {
                float[] left = sdf.getPixel(x, y);
                float[] right = sdf.getPixel(x + 1, y);
                float lm = median(left[0], left[1], left[2]);
                float rm = median(right[0], right[1], right[2]);
                if (Math.abs(lm - 0.5f) + Math.abs(rm - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(left, right);
                    protectExtremeChannels(stencil, x, y, left, lm, mask);
                    protectExtremeChannels(stencil, x + 1, y, right, rm, mask);
                }
            }
        }

        // Vertical texel pairs
        radius = (float)(PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(0, transformation.getDistanceMapping().map(1.0))).length());

        for (int y = 0; y < sdf.getHeight() - 1; ++y) {
            for (int x = 0; x < sdf.getWidth(); ++x) {
                float[] bottom = sdf.getPixel(x, y);
                float[] top = sdf.getPixel(x, y + 1);
                float bm = median(bottom[0], bottom[1], bottom[2]);
                float tm = median(top[0], top[1], top[2]);
                if (Math.abs(bm - 0.5f) + Math.abs(tm - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(bottom, top);
                    protectExtremeChannels(stencil, x, y, bottom, bm, mask);
                    protectExtremeChannels(stencil, x, y + 1, top, tm, mask);
                }
            }
        }

        // Diagonal texel pairs
        radius = (float)(PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(transformation.getDistanceMapping().map(1.0), 0)).length());

        for (int y = 0; y < sdf.getHeight() - 1; ++y) {
            for (int x = 0; x < sdf.getWidth() - 1; ++x) {
                float[] lb = sdf.getPixel(x, y);
                float[] rb = sdf.getPixel(x + 1, y);
                float[] lt = sdf.getPixel(x, y + 1);
                float[] rt = sdf.getPixel(x + 1, y + 1);

                float mlb = median(lb[0], lb[1], lb[2]);
                float mrb = median(rb[0], rb[1], rb[2]);
                float mlt = median(lt[0], lt[1], lt[2]);
                float mrt = median(rt[0], rt[1], rt[2]);

                if (Math.abs(mlb - 0.5f) + Math.abs(mrt - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(lb, rt);
                    protectExtremeChannels(stencil, x, y, lb, mlb, mask);
                    protectExtremeChannels(stencil, x + 1, y + 1, rt, mrt, mask);
                }
                if (Math.abs(mrb - 0.5f) + Math.abs(mlt - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(rb, lt);
                    protectExtremeChannels(stencil, x + 1, y, rb, mrb, mask);
                    protectExtremeChannels(stencil, x, y + 1, lt, mlt, mask);
                }
            }
        }
    }

    /// Flags all texels as protected.
    public void protectAll() {
        byte[] pixels = stencil.getPixels();
        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] |= Flags.PROTECTED;
        }
    }

    /// Flags texels that are expected to cause interpolation artifacts based on analysis of the SDF only.
    public void findErrors(BitmapConstRef sdf) {
        // Compute the expected deltas between values of horizontally, vertically, and diagonally adjacent texels.
        double hSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(1.0), 0)).length();
        double vSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(0, transformation.getDistanceMapping().map(1.0))).length();
        double dSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(1.0), 0)).length();

        // Inspect all texels.
        for (int y = 0; y < sdf.getHeight(); ++y) {
            for (int x = 0; x < sdf.getWidth(); ++x) {
                float[] c = sdf.getPixel(x, y);
                float cm = median(c[0], c[1], c[2]);
                boolean protectedFlag = (stencil.getPixel(x, y) & Flags.PROTECTED) != 0;

                boolean hasError = false;

                // Check all 8 neighbors
                if (x > 0) {
                    float[] l = sdf.getPixel(x - 1, y);
                    if (hasLinearArtifact(new BaseArtifactClassifier(hSpan, protectedFlag), cm, c, l)) {
                        hasError = true;
                    }
                }
                if (y > 0) {
                    float[] b = sdf.getPixel(x, y - 1);
                    if (hasLinearArtifact(new BaseArtifactClassifier(vSpan, protectedFlag), cm, c, b)) {
                        hasError = true;
                    }
                }
                if (x < sdf.getWidth() - 1) {
                    float[] r = sdf.getPixel(x + 1, y);
                    if (hasLinearArtifact(new BaseArtifactClassifier(hSpan, protectedFlag), cm, c, r)) {
                        hasError = true;
                    }
                }
                if (y < sdf.getHeight() - 1) {
                    float[] t = sdf.getPixel(x, y + 1);
                    if (hasLinearArtifact(new BaseArtifactClassifier(vSpan, protectedFlag), cm, c, t)) {
                        hasError = true;
                    }
                }

                // Diagonal checks
                if (x > 0 && y > 0) {
                    float[] l = sdf.getPixel(x - 1, y);
                    float[] b = sdf.getPixel(x, y - 1);
                    float[] lb = sdf.getPixel(x - 1, y - 1);
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag), cm, c, l, b, lb)) {
                        hasError = true;
                    }
                }
                // Additional diagonal checks...

                if (hasError) {
                    stencil.setPixel(x, y, (byte)(stencil.getPixel(x, y) | Flags.ERROR));
                }
            }
        }
    }

    /// Flags texels that are expected to cause interpolation artifacts based on analysis of the SDF and comparison with the exact shape distance.
    public void findErrorsWithShape(BitmapConstRef sdf, Shape shape) {
        // Compute the expected deltas between values of horizontally, vertically, and diagonally adjacent texels.
        double hSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(1.0), 0)).length();
        double vSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(0, transformation.getDistanceMapping().map(1.0))).length();
        double dSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(1.0), 0)).length();

        ShapeDistanceChecker shapeDistanceChecker = new ShapeDistanceChecker(sdf, shape, transformation,
                transformation.getDistanceMapping(), minImproveRatio);

        // Inspect all texels.
        for (int y = 0; y < sdf.getHeight(); ++y) {
            int row = shape.inverseYAxis ? sdf.getHeight() - y - 1 : y;
            for (int x = 0; x < sdf.getWidth(); ++x) {
                if ((stencil.getPixel(x, row) & Flags.ERROR) != 0) {
                    continue;
                }

                float[] c = sdf.getPixel(x, row);
                shapeDistanceChecker.shapeCoord = transformation.unproject(new Vector2d(x + 0.5, y + 0.5));
                shapeDistanceChecker.sdfCoord = new Vector2d(x + 0.5, row + 0.5);
                shapeDistanceChecker.msd = c;
                shapeDistanceChecker.protectedFlag = (stencil.getPixel(x, row) & Flags.PROTECTED) != 0;
                float cm = median(c[0], c[1], c[2]);

                boolean hasError = false;

                // Check all 8 neighbors with shape distance checker
                if (x > 0) {
                    float[] l = sdf.getPixel(x - 1, row);
                    if (hasLinearArtifact(shapeDistanceChecker.classifier(new Vector2d(-1, 0), hSpan), cm, c, l)) {
                        hasError = true;
                    }
                }
                // Additional neighbor checks...

                if (hasError) {
                    stencil.setPixel(x, row, (byte)(stencil.getPixel(x, row) | Flags.ERROR));
                }
            }
        }
    }

    /// Modifies the MSDF so that all texels with the error flag are converted to single-channel.
    public void apply(BitmapRef sdf) {
        for (int y = 0; y < sdf.getHeight(); ++y) {
            for (int x = 0; x < sdf.getWidth(); ++x) {
                if ((stencil.getPixel(x, y) & Flags.ERROR) != 0) {
                    // Set all color channels to the median.
                    float[] texel = sdf.getPixel(x, y);
                    float m = median(texel[0], texel[1], texel[2]);
                    texel[0] = m;
                    texel[1] = m;
                    texel[2] = m;
                }
            }
        }
    }

    /// Returns the stencil in its current state (see Flags).
    public BitmapRef getStencil() {
        return stencil;
    }

    private static float mix(float a, float b, double t) {
        return (float)(a + t * (b - a));
    }

    /// Determines if the channel contributes to an edge between the two texels a, b.
    private static boolean edgeBetweenTexelsChannel(float[] a, float[] b, int channel) {
        // Find interpolation ratio t (0 < t < 1) where an edge is expected (mix(a[channel], b[channel], t) == 0.5).
        double t = (a[channel] - 0.5) / (a[channel] - b[channel]);
        if (t > 0 && t < 1) {
            // Interpolate channel values at t.
            float[] c = new float[3];
            c[0] = mix(a[0], b[0], t);
            c[1] = mix(a[1], b[1], t);
            c[2] = mix(a[2], b[2], t);
            // This is only an edge if the zero-distance channel is the median.
            return median(c[0], c[1], c[2]) == c[channel];
        }
        return false;
    }

    /// Returns a bit mask of which channels contribute to an edge between the two texels a, b.
    private static int edgeBetweenTexels(float[] a, float[] b) {
        return (EdgeColorEnum.RED.getValue().color * (edgeBetweenTexelsChannel(a, b, 0) ? 1 : 0) +
                EdgeColorEnum.GREEN.getValue().color * (edgeBetweenTexelsChannel(a, b, 1) ? 1 : 0) +
                EdgeColorEnum.BLUE.getValue().color * (edgeBetweenTexelsChannel(a, b, 2) ? 1 : 0));
    }

    /// Marks texel as protected if one of its non-median channels is present in the channel mask.
    private void protectExtremeChannels(BitmapRef stencil, int x, int y, float[] msd, float m, int mask) {
        if ((mask & EdgeColorEnum.RED.getValue().color) != 0 && msd[0] != m ||
                (mask & EdgeColorEnum.GREEN.getValue().color) != 0 && msd[1] != m ||
                (mask & EdgeColorEnum.BLUE.getValue().color) != 0 && msd[2] != m) {
            stencil.setPixel(x, y, (byte)(stencil.getPixel(x, y) | Flags.PROTECTED));
        }
    }

    /// Returns the median of the linear interpolation of texels a, b at t.
    private static float interpolatedMedian(float[] a, float[] b, double t) {
        return median(
                mix(a[0], b[0], t),
                mix(a[1], b[1], t),
                mix(a[2], b[2], t)
        );
    }

    /// Returns the median of the bilinear interpolation with the given constant, linear, and quadratic terms at t.
    private static float interpolatedMedian(float[] a, float[] l, float[] q, double t) {
        return (float)median(
                t * (t * q[0] + l[0]) + a[0],
                t * (t * q[1] + l[1]) + a[1],
                t * (t * q[2] + l[2]) + a[2]
        );
    }

    /// Determines if the interpolated median xm is an artifact.
    private static boolean isArtifact(boolean isProtected, double axSpan, double bxSpan, float am, float bm, float xm) {
        return (
                // For protected texels, only report an artifact if it would cause fill inversion (change between positive and negative distance).
                (!isProtected || (am > 0.5f && bm > 0.5f && xm <= 0.5f) || (am < 0.5f && bm < 0.5f && xm >= 0.5f)) &&
                        // This is an artifact if the interpolated median is outside the range of possible values based on its distance from a, b.
                        !(xm >= am - axSpan && xm <= am + axSpan && xm >= bm - bxSpan && xm <= bm + bxSpan)
        );
    }

    /// Checks if a linear interpolation artifact will occur at a point where two specific color channels are equal - such points have extreme median values.
    private static boolean hasLinearArtifactInner(BaseArtifactClassifier artifactClassifier, float am, float bm,
                                                  float[] a, float[] b, float dA, float dB) {
        // Find interpolation ratio t (0 < t < 1) where two color channels are equal (mix(dA, dB, t) == 0).
        double t = (double) dA / (dA - dB);
        if (t > ARTIFACT_T_EPSILON && t < 1 - ARTIFACT_T_EPSILON) {
            // Interpolate median at t and let the classifier decide if its value indicates an artifact.
            float xm = interpolatedMedian(a, b, t);
            return artifactClassifier.evaluate(t, xm, artifactClassifier.rangeTest(0, 1, t, am, bm, xm));
        }
        return false;
    }

    /// Checks if a linear interpolation artifact will occur inbetween two horizontally or vertically adjacent texels a, b.
    private static boolean hasLinearArtifact(BaseArtifactClassifier artifactClassifier, float am, float[] a, float[] b) {
        float bm = median(b[0], b[1], b[2]);
        return (
                // Out of the pair, only report artifacts for the texel further from the edge to minimize side effects.
                Math.abs(am - 0.5f) >= Math.abs(bm - 0.5f) && (
                        // Check points where each pair of color channels meets.
                        hasLinearArtifactInner(artifactClassifier, am, bm, a, b, a[1] - a[0], b[1] - b[0]) ||
                                hasLinearArtifactInner(artifactClassifier, am, bm, a, b, a[2] - a[1], b[2] - b[1]) ||
                                hasLinearArtifactInner(artifactClassifier, am, bm, a, b, a[0] - a[2], b[0] - b[2])
                )
        );
    }

    /// Checks if a bilinear interpolation artifact will occur inbetween two diagonally adjacent texels a, d (with b, c forming the other diagonal).
    private static boolean hasDiagonalArtifact(BaseArtifactClassifier artifactClassifier, float am,
                                               float[] a, float[] b, float[] c, float[] d) {
        float dm = median(d[0], d[1], d[2]);
        // Out of the pair, only report artifacts for the texel further from the edge to minimize side effects.
        if (Math.abs(am - 0.5f) >= Math.abs(dm - 0.5f)) {
            float[] abc = new float[3];
            abc[0] = a[0] - b[0] - c[0];
            abc[1] = a[1] - b[1] - c[1];
            abc[2] = a[2] - b[2] - c[2];

            // Compute the linear terms for bilinear interpolation.
            float[] l = new float[3];
            l[0] = -a[0] - abc[0];
            l[1] = -a[1] - abc[1];
            l[2] = -a[2] - abc[2];

            // Compute the quadratic terms for bilinear interpolation.
            float[] q = new float[3];
            q[0] = d[0] + abc[0];
            q[1] = d[1] + abc[1];
            q[2] = d[2] + abc[2];

            // Compute interpolation ratios tEx (0 < tEx[i] < 1) for the local extremes of each color channel (the derivative 2*q[i]*tEx[i]+l[i] == 0).
            double[] tEx = new double[3];
            tEx[0] = -0.5 * l[0] / q[0];
            tEx[1] = -0.5 * l[1] / q[1];
            tEx[2] = -0.5 * l[2] / q[2];

            // Check points where each pair of color channels meets.
            return (
                    hasDiagonalArtifactInner(artifactClassifier, am, dm, a, l, q,
                            a[1] - a[0], b[1] - b[0] + c[1] - c[0], d[1] - d[0], tEx[0], tEx[1]) ||
                            hasDiagonalArtifactInner(artifactClassifier, am, dm, a, l, q,
                                    a[2] - a[1], b[2] - b[1] + c[2] - c[1], d[2] - d[1], tEx[1], tEx[2]) ||
                            hasDiagonalArtifactInner(artifactClassifier, am, dm, a, l, q,
                                    a[0] - a[2], b[0] - b[2] + c[0] - c[2], d[0] - d[2], tEx[2], tEx[0])
            );
        }
        return false;
    }

    /// Checks if a bilinear interpolation artifact will occur at a point where two specific color channels are equal - such points have extreme median values.
    private static boolean hasDiagonalArtifactInner(BaseArtifactClassifier artifactClassifier, float am, float dm,
                                                    float[] a, float[] l, float[] q, float dA, float dBC, float dD,
                                                    double tEx0, double tEx1) {
        // Find interpolation ratios t (0 < t[i] < 1) where two color channels are equal.
        double[] t = new double[2];
        int solutions = solveQuadratic(t, dD - dBC + dA, dBC - dA - dA, dA);

        for (int i = 0; i < solutions; ++i) {
            // Solutions t[i] == 0 and t[i] == 1 are singularities and occur very often because two channels are usually equal at texels.
            if (t[i] > ARTIFACT_T_EPSILON && t[i] < 1 - ARTIFACT_T_EPSILON) {
                // Interpolate median xm at t.
                float xm = interpolatedMedian(a, l, q, t[i]);
                // Determine if xm deviates too much from medians of a, d.
                int rangeFlags = artifactClassifier.rangeTest(0, 1, t[i], am, dm, xm);

                // Additionally, check xm against the interpolated medians at the local extremes tEx0, tEx1.
                double[] tEnd = new double[2];
                float[] em = new float[2];

                // tEx0
                if (tEx0 > 0 && tEx0 < 1) {
                    tEnd[0] = 0;
                    tEnd[1] = 1;
                    em[0] = am;
                    em[1] = dm;
                    int idx = tEx0 > t[i] ? 1 : 0;
                    tEnd[idx] = tEx0;
                    em[idx] = interpolatedMedian(a, l, q, tEx0);
                    rangeFlags |= artifactClassifier.rangeTest(tEnd[0], tEnd[1], t[i], em[0], em[1], xm);
                }

                // tEx1
                if (tEx1 > 0 && tEx1 < 1) {
                    tEnd[0] = 0;
                    tEnd[1] = 1;
                    em[0] = am;
                    em[1] = dm;
                    int idx = tEx1 > t[i] ? 1 : 0;
                    tEnd[idx] = tEx1;
                    em[idx] = interpolatedMedian(a, l, q, tEx1);
                    rangeFlags |= artifactClassifier.rangeTest(tEnd[0], tEnd[1], t[i], em[0], em[1], xm);
                }

                if (artifactClassifier.evaluate(t[i], xm, rangeFlags)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Solves quadratic equation ax^2 + bx + c = 0. Returns the number of solutions.
    private static int solveQuadratic(double[] solutions, double a, double b, double c) {
        if (Math.abs(a) < 1e-14) {
            if (Math.abs(b) < 1e-14) {
                return 0;
            }
            solutions[0] = -c / b;
            return 1;
        }

        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return 0;
        }

        if (discriminant == 0) {
            solutions[0] = -b / (2 * a);
            return 1;
        }

        double sqrtDiscriminant = Math.sqrt(discriminant);
        solutions[0] = (-b - sqrtDiscriminant) / (2 * a);
        solutions[1] = (-b + sqrtDiscriminant) / (2 * a);
        return 2;
    }
}