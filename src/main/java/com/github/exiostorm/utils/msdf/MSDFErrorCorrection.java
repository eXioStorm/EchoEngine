package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import org.joml.Vector2d;

import java.util.Arrays;

import static com.github.exiostorm.utils.msdf.Arithmetics.clamp;
import static com.github.exiostorm.utils.msdf.Arithmetics.mix;
import static com.github.exiostorm.utils.msdf.EquationSolver.solveQuadratic;
import static com.github.exiostorm.utils.msdf.MSDFErrorCorrection.ErrorCorrectionConfig.DEFAULT_MIN_DEVIATION_RATIO;
import static com.github.exiostorm.utils.msdf.MSDFErrorCorrection.ErrorCorrectionConfig.DEFAULT_MIN_IMPROVE_RATIO;
import static com.github.exiostorm.utils.msdf.MathUtils.median;

public class MSDFErrorCorrection {

    private static final double ARTIFACT_T_EPSILON = 0.01;
    private static final double PROTECTION_RADIUS_TOLERANCE = 1.001;

    private static final int CLASSIFIER_FLAG_CANDIDATE = 0x01;
    private static final int CLASSIFIER_FLAG_ARTIFACT = 0x02;
    public static final class Flags {
        // Might need these to be int instead of byte if anything ever goes over 127.
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
    //TODO 20251228 @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
    // @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

    /**
     * The shape distance checker evaluates the exact shape distance to find additional artifacts
     * at a significant performance cost. This is equivalent to the C++ template class:
     * template <template <typename> class ContourCombiner, int N> class ShapeDistanceChecker
     *
     * @param <C> ContourCombiner type
     * @param <D> Distance type returned by the ContourCombiner
     */
    public class ShapeDistanceChecker<C extends ContourCombiners.ContourCombiner<D>, D> {

        // Public fields (matching C++ public members)
        public Vector2d shapeCoord;
        public Vector2d sdfCoord;
        //TODO 20251228 erm, this might need corrected.
        public float[] msd;  // Pointer to current MSD values (3 channels)
        public boolean protectedFlag;

        // Private fields
        private final ShapeDistanceFinder<C, D> distanceFinder;
        private final BitmapRef<float[]> sdf;
        private final int nChannels;  // N from C++ template
        private final DistanceMapping distanceMapping;
        private Vector2d texelSize;
        private final double minImproveRatio;

        /**
         * Constructor matching C++ version
         * @param sdf The bitmap section containing the signed distance field
         * @param shape The shape to check distances against
         * @param projection The projection transformation
         * @param distanceMapping The distance mapping function
         * @param minImproveRatio Minimum ratio for improvement detection
         * @param contourCombiner The contour combiner to use
         */
        public ShapeDistanceChecker(BitmapRef<float[]> sdf,
                                    MsdfShape shape,
                                    Projection projection,
                                    DistanceMapping distanceMapping,
                                    double minImproveRatio,
                                    C contourCombiner) {
            this.sdf = sdf;
            this.nChannels = sdf.nChannels;
            this.distanceMapping = distanceMapping;
            this.minImproveRatio = minImproveRatio;
            this.distanceFinder = new ShapeDistanceFinder<>(shape, contourCombiner);
            this.texelSize = projection.unprojectVector(new Vector2d(1, 1));
            this.shapeCoord = new Vector2d();
            this.sdfCoord = new Vector2d();
            this.msd = new float[3];
        }

        /**
         * Creates an artifact classifier for a specific direction
         * @param direction The direction vector for interpolation
         * @param span The span for the classifier
         * @return A new ArtifactClassifier instance
         */
        public ArtifactClassifier classifier(Vector2d direction, double span) {
            return new ArtifactClassifier(this, direction, span);
        }



        /**
         * Inner class representing the artifact classifier
         * Extends BaseArtifactClassifier functionality
         */
        public class ArtifactClassifier {

            private final ShapeDistanceChecker<C, D> parent;
            private final Vector2d direction;
            private final double span;
            private final boolean protectedFlag;

            /**
             * Constructor for ArtifactClassifier
             * @param parent The parent ShapeDistanceChecker
             * @param direction The direction for interpolation
             * @param span The span for range testing
             */
            public ArtifactClassifier(ShapeDistanceChecker<C, D> parent, Vector2d direction, double span) {
                this.parent = parent;
                this.direction = new Vector2d(direction);
                this.span = span;
                this.protectedFlag = parent.protectedFlag;
            }

            /**
             * Evaluates if the combined results of the tests performed on the median value m
             * interpolated at t indicate an artifact.
             * @param t Interpolation parameter
             * @param m Median value at t
             * @param flags Flags from range test
             * @return True if an artifact is detected
             */
            public boolean evaluate(double t, float m, int flags) {
                if ((flags & CLASSIFIER_FLAG_CANDIDATE) != 0) {
                    // Skip expensive distance evaluation if already classified as artifact
                    if ((flags & CLASSIFIER_FLAG_ARTIFACT) != 0) {
                        return true;
                    }

                    Vector2d tVector = new Vector2d(direction).mul(t);
                    float[] oldMSD = new float[nChannels];
                    float[] newMSD = new float[3];

                    // Compute the color that would be currently interpolated at the artifact candidate's position
                    Vector2d sdfCoord = new Vector2d(parent.sdfCoord).add(tVector);
                    interpolate(oldMSD, parent.sdf, sdfCoord);

                    // Compute the color that would be interpolated if error correction was applied
                    double aWeight = (1 - Math.abs(tVector.x)) * (1 - Math.abs(tVector.y));
                    float aPSD = median(parent.msd[0], parent.msd[1], parent.msd[2]);
                    newMSD[0] = (float) (oldMSD[0] + aWeight * (aPSD - parent.msd[0]));
                    newMSD[1] = (float) (oldMSD[1] + aWeight * (aPSD - parent.msd[1]));
                    newMSD[2] = (float) (oldMSD[2] + aWeight * (aPSD - parent.msd[2]));

                    // Compute the evaluated distance before and after error correction
                    float oldPSD = median(oldMSD[0], oldMSD[1], oldMSD[2]);
                    float newPSD = median(newMSD[0], newMSD[1], newMSD[2]);

                    // Compute exact shape distance
                    Vector2d shapePoint = new Vector2d(parent.shapeCoord)
                            .add(new Vector2d(tVector).mul(parent.texelSize));

                    D distanceResult = parent.distanceFinder.distance(shapePoint);
                    double rawDistance = extractDistance(distanceResult);
                    float refPSD = (float) parent.distanceMapping.map(rawDistance);

                    // Compare the differences of the exact distance and the before/after distances
                    return parent.minImproveRatio * Math.abs(newPSD - refPSD) < Math.abs(oldPSD - refPSD);
                }
                return false;
            }

            //TODO 20251228 investigate this helper method~ not part of what we asked to generate.
            private double extractDistance(D distanceResult) {
                if (distanceResult instanceof Double) {
                    return (Double) distanceResult;
                } else if (distanceResult instanceof EdgeSelectors.MultiDistance md) {
                    return median(md.c, md.m, md.y);
                }
                throw new IllegalStateException("Unknown distance type: " + distanceResult.getClass());
            }
        }
    }
    /**
     * Interpolates values from the bitmap at the given position
     * Based on the interpolate function from bitmap-interpolation.hpp
     * @param output Output array to store interpolated values
     * @param bitmap The bitmap to interpolate from
     * @param pos Position to interpolate at
     */
    private void interpolate(float[] output, BitmapRef<float[]> bitmap, Vector2d pos) {
        //TODO 20251228 this method was supposed to be part of it's own class bitmap-inerpolation.hpp, it gets referenced here, and in render-sdf.cpp
        double x = clamp(pos.x, bitmap.width);
        double y = clamp(pos.y, bitmap.height);

        x -= 0.5;
        y -= 0.5;

        int l = (int) Math.floor(x);
        int b = (int) Math.floor(y);
        int r = l + 1;
        int t = b + 1;

        double lr = x - l;
        double bt = y - b;

        l = clamp(l, bitmap.width - 1);
        r = clamp(r, bitmap.width - 1);
        b = clamp(b, bitmap.height - 1);
        t = clamp(t, bitmap.height - 1);

        float[] pixels = bitmap.pixels;
        int lbIdx = bitmap.sectionOperator(l, b);
        int rbIdx = bitmap.sectionOperator(r, b);
        int ltIdx = bitmap.sectionOperator(l, t);
        int rtIdx = bitmap.sectionOperator(r, t);
        int channels = pixels.length;
        for (int i = 0; i < channels && i < output.length; ++i) {
            output[i] = mix(
                    mix(pixels[lbIdx + i], pixels[rbIdx + i], lr),
                    mix(pixels[ltIdx + i], pixels[rtIdx + i], lr),
                    bt
            );
        }
    }

    private BitmapRef<byte[]> stencil;
    private SDFTransformation transformation;
    private double minDeviationRatio;
    private double minImproveRatio;
    
    public MSDFErrorCorrection() {
        this.minDeviationRatio = DEFAULT_MIN_DEVIATION_RATIO;
        this.minImproveRatio = DEFAULT_MIN_IMPROVE_RATIO;
    }
    // also, Java byte is not 100% the same as C++ byte, C++ byte is range 0 to 255, Java is -128 to 127.
    /**
     * Constructor with stencil and transformation.
     *
     * @param stencil The stencil bitmap (byte array with 1 channel)
     * @param transformation The SDF transformation
     */
    public MSDFErrorCorrection(BitmapRef<byte[]> stencil, SDFTransformation transformation) {
        this.stencil = stencil;
        this.transformation = transformation;
        this.minDeviationRatio = DEFAULT_MIN_DEVIATION_RATIO;
        this.minImproveRatio = DEFAULT_MIN_IMPROVE_RATIO;

        // Initialize stencil to zero (equivalent to memset in C++)
        if (stencil.pixels != null) {
            //TODO 20251228
            //Arrays.fill is doing the same as C++ using for() to loop through the data setting everything to 0 byte.
            // I'm only confused on the part "sizeof(byte)*stencil.width"
            Arrays.fill(stencil.pixels, (byte) 0);
        }
    }
    // Identical to C++ implementation.
    public void setMinDeviationRatio(double minDeviationRatio) {
        this.minDeviationRatio = minDeviationRatio;
    }
    // Identical to C++ implementation.
    public void setMinImproveRatio(double minImproveRatio) {
        this.minImproveRatio = minImproveRatio;
    }

    //TODO 20251228 not 100% certain this is identical, however it's never used currently so I'm prioritizing moving forward with it as it is now.
    public void protectCorners(MsdfShape msdfShape) {

        stencil.reorient(msdfShape.getYAxisOrientation());

        for (Contours.Contour contour : msdfShape.contours) {
            if (!contour.edges.isEmpty()) {

                EdgeSegment prevEdge = contour.edges.getLast().edge;

                for (EdgeHolder edgeHolder : contour.edges) {
                    EdgeSegment edge = edgeHolder.edge;

                    int commonColor = prevEdge.edgeColor & edge.edgeColor;

                    if ((commonColor & (commonColor - 1)) == 0) {

                        Vector2d p = transformation.project(edge.point(0));
                        int l = (int) Math.floor(p.x - 0.5);
                        int b = (int) Math.floor(p.y - 0.5);
                        int r = l + 1;
                        int t = b + 1;


                        if (l < stencil.width && b < stencil.height && r >= 0 && t >= 0) {
                            if (l >= 0 && b >= 0)
                                setStencilValue(l, b, (byte) (getStencilValue(l, b) | Flags.PROTECTED));
                            if (r < stencil.width && b >= 0)
                                setStencilValue(r, b, (byte) (getStencilValue(r, b) | Flags.PROTECTED));
                            if (l >= 0 && t < stencil.height)
                                setStencilValue(l, t, (byte) (getStencilValue(l, t) | Flags.PROTECTED));
                            if (r < stencil.width && t < stencil.height)
                                setStencilValue(r, t, (byte) (getStencilValue(r, t) | Flags.PROTECTED));
                        }
                    }
                    prevEdge = edge;
                }
            }
        }
    }

    /**
     * Determines if a channel contributes to an edge between two texels.
     */
    private static boolean edgeBetweenTexelsChannel(float[] a, int aIdx, float[] b, int bIdx, int channel) {
        // Find interpolation ratio t (0 < t < 1) where an edge is expected
        double t = (a[aIdx + channel] - 0.5) / (a[aIdx + channel] - b[bIdx + channel]);
        if (t > 0 && t < 1) {
            // Interpolate channel values at t
            float c0 = mix(a[aIdx], b[bIdx], t);
            float c1 = mix(a[aIdx + 1], b[bIdx + 1], t);
            float c2 = mix(a[aIdx + 2], b[bIdx + 2], t);
            // This is only an edge if the zero-distance channel is the median
            return median(c0, c1, c2) == (channel == 0 ? c0 : channel == 1 ? c1 : c2);
        }
        return false;
    }

    /**
     * Returns a bit mask of which channels contribute to an edge between two texels.
     */
    private static int edgeBetweenTexels(float[] a, int aIdx, float[] b, int bIdx) {
        return (edgeBetweenTexelsChannel(a, aIdx, b, bIdx, 0) ? EdgeColorEnum.RED.getValue().color : 0) +
                (edgeBetweenTexelsChannel(a, aIdx, b, bIdx, 1) ? EdgeColorEnum.GREEN.getValue().color : 0) +
                (edgeBetweenTexelsChannel(a, aIdx, b, bIdx, 2) ? EdgeColorEnum.BLUE.getValue().color : 0);
    }

    /**
     * Marks texel as protected if one of its non-median channels is present in the channel mask.
     */
    private void protectExtremeChannels(int x, int y, float[] msd, int msdIdx, float m, int mask) {
        if ((mask & EdgeColorEnum.RED.getValue().color) != 0 && msd[msdIdx] != m ||
                (mask & EdgeColorEnum.GREEN.getValue().color) != 0 && msd[msdIdx + 1] != m ||
                (mask & EdgeColorEnum.BLUE.getValue().color) != 0 && msd[msdIdx + 2] != m) {
            //TODO 20251228 cast to byte here... even though both values are byte... could be a problem maybe?
            setStencilValue(x, y, (byte) (getStencilValue(x, y) | Flags.PROTECTED));
        }
    }

    /**
     * Flags all texels that contribute to edges as protected.
     *
     * @param sdf The SDF bitmap (3 or 4 channels)
     */
    public void protectEdges(BitmapRef<float[]> sdf) {
        //TODO 20251228 not 100% certain this is identical, however it's never used currently so I'm prioritizing moving forward with it as it is now.
        // additionally, Qwen says they're mostly identical, C++ just being more efficient.
        float radius;
        stencil.reorient(sdf.yOrientation);

        // Horizontal texel pairs
        radius = (float) (PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(
                        transformation.distanceMapping.map(new DistanceMapping.Delta(1)), 0)).length());

        for (int y = 0; y < sdf.height; y++) {
            for (int x = 0; x < sdf.width - 1; x++) {
                int leftIdx = sdf.sectionOperator(x, y);
                int rightIdx = sdf.sectionOperator(x + 1, y);

                float lm = median(sdf.pixels[leftIdx], sdf.pixels[leftIdx + 1], sdf.pixels[leftIdx + 2]);
                float rm = median(sdf.pixels[rightIdx], sdf.pixels[rightIdx + 1], sdf.pixels[rightIdx + 2]);

                if (Math.abs(lm - 0.5f) + Math.abs(rm - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(sdf.pixels, leftIdx, sdf.pixels, rightIdx);
                    protectExtremeChannels(x, y, sdf.pixels, leftIdx, lm, mask);
                    protectExtremeChannels(x + 1, y, sdf.pixels, rightIdx, rm, mask);
                }
            }
        }

        // Vertical texel pairs
        radius = (float) (PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(0,
                        transformation.distanceMapping.map(new DistanceMapping.Delta(1)))).length());

        for (int y = 0; y < sdf.height - 1; y++) {
            for (int x = 0; x < sdf.width; x++) {
                int bottomIdx = sdf.sectionOperator(x, y);
                int topIdx = sdf.sectionOperator(x, y + 1);

                float bm = median(sdf.pixels[bottomIdx], sdf.pixels[bottomIdx + 1], sdf.pixels[bottomIdx + 2]);
                float tm = median(sdf.pixels[topIdx], sdf.pixels[topIdx + 1], sdf.pixels[topIdx + 2]);

                if (Math.abs(bm - 0.5f) + Math.abs(tm - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(sdf.pixels, bottomIdx, sdf.pixels, topIdx);
                    protectExtremeChannels(x, y, sdf.pixels, bottomIdx, bm, mask);
                    protectExtremeChannels(x, y + 1, sdf.pixels, topIdx, tm, mask);
                }
            }
        }

        // Diagonal texel pairs
        radius = (float) (PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(
                        transformation.distanceMapping.map(new DistanceMapping.Delta(1)), 0)).length());

        for (int y = 0; y < sdf.height - 1; y++) {
            for (int x = 0; x < sdf.width - 1; x++) {
                int lbIdx = sdf.sectionOperator(x, y);
                int rbIdx = sdf.sectionOperator(x + 1, y);
                int ltIdx = sdf.sectionOperator(x, y + 1);
                int rtIdx = sdf.sectionOperator(x + 1, y + 1);

                float mlb = median(sdf.pixels[lbIdx], sdf.pixels[lbIdx + 1], sdf.pixels[lbIdx + 2]);
                float mrb = median(sdf.pixels[rbIdx], sdf.pixels[rbIdx + 1], sdf.pixels[rbIdx + 2]);
                float mlt = median(sdf.pixels[ltIdx], sdf.pixels[ltIdx + 1], sdf.pixels[ltIdx + 2]);
                float mrt = median(sdf.pixels[rtIdx], sdf.pixels[rtIdx + 1], sdf.pixels[rtIdx + 2]);

                if (Math.abs(mlb - 0.5f) + Math.abs(mrt - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(sdf.pixels, lbIdx, sdf.pixels, rtIdx);
                    protectExtremeChannels(x, y, sdf.pixels, lbIdx, mlb, mask);
                    protectExtremeChannels(x + 1, y + 1, sdf.pixels, rtIdx, mrt, mask);
                }
                if (Math.abs(mrb - 0.5f) + Math.abs(mlt - 0.5f) < radius) {
                    int mask = edgeBetweenTexels(sdf.pixels, rbIdx, sdf.pixels, ltIdx);
                    protectExtremeChannels(x + 1, y, sdf.pixels, rbIdx, mrb, mask);
                    protectExtremeChannels(x, y + 1, sdf.pixels, ltIdx, mlt, mask);
                }
            }
        }
    }

    /**
     * Flags all texels as protected.
     */
    public void protectAll() {
        for (int y = 0; y < stencil.height; y++) {
            for (int x = 0; x < stencil.width; x++) {
                int idx = stencil.sectionOperator(x, y);
                stencil.pixels[idx] = (byte) (stencil.pixels[idx] | Flags.PROTECTED);
            }
        }
    }

    /**
     * Returns the median of the linear interpolation of texels a, b at t.
     */
    private static float interpolatedMedian(float[] a, int aIdx, float[] b, int bIdx, double t) {
        return median(
                mix(a[aIdx], b[bIdx], t),
                mix(a[aIdx + 1], b[bIdx + 1], t),
                mix(a[aIdx + 2], b[bIdx + 2], t)
        );
    }

    /**
     * Returns the median of the bilinear interpolation with the given constant, linear, and quadratic terms at t.
     */
    private static float interpolatedMedian(float[] a, float[] l, float[] q, double t) {
        return (float) median(
                t * (t * q[0] + l[0]) + a[0],
                t * (t * q[1] + l[1]) + a[1],
                t * (t * q[2] + l[2]) + a[2]
        );
    }
    
    private static boolean hasLinearArtifactInner(BaseArtifactClassifier classifier, float am, float bm,
                                                  float[] a, int aIdx, float[] b, int bIdx,
                                                  float dA, float dB) {
        //TODO 20251228 not 100% certain this is identical.
        // had to dissect part from my previous existing method, and new proposed method. high chance it's not correct. First method we'll have to likely come back to.

        // Find interpolation ratio t (0 < t < 1) where two color channels are equal (mix(dA, dB, t) == 0).
        double t = dA / (dA - dB);
        if (t > ARTIFACT_T_EPSILON && t < 1 - ARTIFACT_T_EPSILON) {
            // Interpolate median at t
            float xm = interpolatedMedian(a, aIdx, b, bIdx, t);
            return classifier.evaluate(t, xm, classifier.rangeTest(0, 1, t, am, bm, xm));
        }
        return false;
    }
    //TODO compare to :
    /*
    static bool hasLinearArtifactInner(const ArtifactClassifier &artifactClassifier, float am, float bm, const float *a, const float *b, float dA, float dB) {
    double t = (double) dA/(dA-dB);
    if (t > ARTIFACT_T_EPSILON && t < 1-ARTIFACT_T_EPSILON) {
        float xm = interpolatedMedian(a, b, t);
        return artifactClassifier.evaluate(t, xm, artifactClassifier.rangeTest(0, 1, t, am, bm, xm));
    }
    return false;
}*/

    /**
     * Checks if a bilinear interpolation artifact will occur at a point where two specific color channels are equal.
     */
    private static boolean hasDiagonalArtifactInner(BaseArtifactClassifier classifier,
                                                    float am, float dm,
                                                    float[] a, float[] l, float[] q,
                                                    float dA, float dBC, float dD,
                                                    double tEx0, double tEx1) {
        //TODO 20251228 not 100% certain this is identical.
        // Find interpolation ratios t where two color channels are equal
        double[] t = new double[2];
        int solutions = solveQuadratic(t, dD - dBC + dA, dBC - dA - dA, dA);

        for (int i = 0; i < solutions; i++) {
            if (t[i] > ARTIFACT_T_EPSILON && t[i] < 1 - ARTIFACT_T_EPSILON) {
                // Interpolate median at t
                float xm = interpolatedMedian(a, l, q, t[i]);
                int rangeFlags = classifier.rangeTest(0, 1, t[i], am, dm, xm);

                // Check against local extremes
                double[] tEnd = new double[2];
                float[] em = new float[2];

                if (tEx0 > 0 && tEx0 < 1) {
                    tEnd[0] = 0; tEnd[1] = 1;
                    em[0] = am; em[1] = dm;
                    int idx = tEx0 > t[i] ? 1 : 0;
                    tEnd[idx] = tEx0;
                    em[idx] = interpolatedMedian(a, l, q, tEx0);
                    rangeFlags |= classifier.rangeTest(tEnd[0], tEnd[1], t[i], em[0], em[1], xm);
                }

                if (tEx1 > 0 && tEx1 < 1) {
                    tEnd[0] = 0; tEnd[1] = 1;
                    em[0] = am; em[1] = dm;
                    int idx = tEx1 > t[i] ? 1 : 0;
                    tEnd[idx] = tEx1;
                    em[idx] = interpolatedMedian(a, l, q, tEx1);
                    rangeFlags |= classifier.rangeTest(tEnd[0], tEnd[1], t[i], em[0], em[1], xm);
                }

                if (classifier.evaluate(t[i], xm, rangeFlags)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a linear interpolation artifact will occur between two horizontally or vertically adjacent texels.
     */
    private static boolean hasLinearArtifact(BaseArtifactClassifier classifier,
                                             float am, float[] a, int aIdx, float[] b, int bIdx) {
        //TODO 20251228 not 100% certain this is identical.
        // holy math. could totally be something wrong here, I can't follow at all what's going on.
        float bm = median(b[bIdx], b[bIdx + 1], b[bIdx + 2]);
        // Out of the pair, only report artifacts for the texel further from the edge
        return Math.abs(am - 0.5f) >= Math.abs(bm - 0.5f) && (
                hasLinearArtifactInner(classifier, am, bm, a, aIdx, b, bIdx,
                        a[aIdx + 1] - a[aIdx], b[bIdx + 1] - b[bIdx]) ||
                        hasLinearArtifactInner(classifier, am, bm, a, aIdx, b, bIdx,
                                a[aIdx + 2] - a[aIdx + 1], b[bIdx + 2] - b[bIdx + 1]) ||
                        hasLinearArtifactInner(classifier, am, bm, a, aIdx, b, bIdx,
                                a[aIdx] - a[aIdx + 2], b[bIdx] - b[bIdx + 2])
        );
    }
    /*C++
    static bool hasLinearArtifact(const ArtifactClassifier &artifactClassifier, float am, const float *a, const float *b) {
    float bm = median(b[0], b[1], b[2]);
    return (
        // Out of the pair, only report artifacts for the texel further from the edge to minimize side effects.
        fabsf(am-.5f) >= fabsf(bm-.5f) && (
            // Check points where each pair of color channels meets.
            hasLinearArtifactInner(artifactClassifier, am, bm, a, b, a[1]-a[0], b[1]-b[0]) ||
            hasLinearArtifactInner(artifactClassifier, am, bm, a, b, a[2]-a[1], b[2]-b[1]) ||
            hasLinearArtifactInner(artifactClassifier, am, bm, a, b, a[0]-a[2], b[0]-b[2])
        )
    );
}
     */
    /*Java alt
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
*/

    /**
     * Checks if a bilinear interpolation artifact will occur between two diagonally adjacent texels.
     */
    private static boolean hasDiagonalArtifact(BaseArtifactClassifier classifier,
                                               float am,
                                               float[] a, int aIdx,
                                               float[] b, int bIdx,
                                               float[] c, int cIdx,
                                               float[] d, int dIdx) {
        //TODO 20251228 not 100% certain this is identical.
        // holy math. could totally be something wrong here, I can't follow at all what's going on.
        float dm = median(d[dIdx], d[dIdx + 1], d[dIdx + 2]);

        // Out of the pair, only report artifacts for the texel further from the edge
        if (Math.abs(am - 0.5f) >= Math.abs(dm - 0.5f)) {
            float[] abc = new float[3];
            abc[0] = a[aIdx] - b[bIdx] - c[cIdx];
            abc[1] = a[aIdx + 1] - b[bIdx + 1] - c[cIdx + 1];
            abc[2] = a[aIdx + 2] - b[bIdx + 2] - c[cIdx + 2];

            // Compute linear terms
            float[] l = new float[3];
            l[0] = -a[aIdx] - abc[0];
            l[1] = -a[aIdx + 1] - abc[1];
            l[2] = -a[aIdx + 2] - abc[2];

            // Compute quadratic terms
            float[] q = new float[3];
            q[0] = d[dIdx] + abc[0];
            q[1] = d[dIdx + 1] + abc[1];
            q[2] = d[dIdx + 2] + abc[2];

            // Compute local extremes
            double[] tEx = new double[3];
            tEx[0] = -0.5 * l[0] / q[0];
            tEx[1] = -0.5 * l[1] / q[1];
            tEx[2] = -0.5 * l[2] / q[2];

            float[] aArr = new float[3];
            aArr[0] = a[aIdx]; aArr[1] = a[aIdx + 1]; aArr[2] = a[aIdx + 2];

            // Check points where each pair of color channels meets
            return hasDiagonalArtifactInner(classifier, am, dm, aArr, l, q,
                    a[aIdx + 1] - a[aIdx],
                    b[bIdx + 1] - b[bIdx] + c[cIdx + 1] - c[cIdx],
                    d[dIdx + 1] - d[dIdx], tEx[0], tEx[1]) ||
                    hasDiagonalArtifactInner(classifier, am, dm, aArr, l, q,
                            a[aIdx + 2] - a[aIdx + 1],
                            b[bIdx + 2] - b[bIdx + 1] + c[cIdx + 2] - c[cIdx + 1],
                            d[dIdx + 2] - d[dIdx + 1], tEx[1], tEx[2]) ||
                    hasDiagonalArtifactInner(classifier, am, dm, aArr, l, q,
                            a[aIdx] - a[aIdx + 2],
                            b[bIdx] - b[bIdx + 2] + c[cIdx] - c[cIdx + 2],
                            d[dIdx] - d[dIdx + 2], tEx[2], tEx[0]);
        }
        return false;
    }

    /**
     * Flags texels that are expected to cause interpolation artifacts based on analysis of the SDF only.
     *
     * @param sdf The SDF bitmap (3 or 4 channels)
     */
    public void findErrors(BitmapRef<float[]> sdf) {
        //TODO 20251228 not 100% certain this is identical, however it's never used currently so I'm prioritizing moving forward with it as it is now.
        stencil.reorient(sdf.yOrientation);

        // Compute the expected deltas between values of horizontally, vertically, and diagonally adjacent texels
        double hSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.distanceMapping.map(new DistanceMapping.Delta(1)), 0)).length();
        double vSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(0, transformation.distanceMapping.map(new DistanceMapping.Delta(1)))).length();
        double dSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.distanceMapping.map(new DistanceMapping.Delta(1)), 0)).length();

        // Inspect all texels
        for (int y = 0; y < sdf.height; y++) {
            for (int x = 0; x < sdf.width; x++) {
                int cIdx = sdf.sectionOperator(x, y);
                float cm = median(sdf.pixels[cIdx], sdf.pixels[cIdx + 1], sdf.pixels[cIdx + 2]);
                boolean protectedFlag = (getStencilValue(x, y) & Flags.PROTECTED) != 0;

                boolean hasError = false;

                // Check left neighbor
                if (x > 0) {
                    int lIdx = sdf.sectionOperator(x - 1, y);
                    if (hasLinearArtifact(new BaseArtifactClassifier(hSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, lIdx)) {
                        hasError = true;
                    }
                }

                // Check bottom neighbor
                if (!hasError && y > 0) {
                    int bIdx = sdf.sectionOperator(x, y - 1);
                    if (hasLinearArtifact(new BaseArtifactClassifier(vSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, bIdx)) {
                        hasError = true;
                    }
                }

                // Check right neighbor
                if (!hasError && x < sdf.width - 1) {
                    int rIdx = sdf.sectionOperator(x + 1, y);
                    if (hasLinearArtifact(new BaseArtifactClassifier(hSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, rIdx)) {
                        hasError = true;
                    }
                }

                // Check top neighbor
                if (!hasError && y < sdf.height - 1) {
                    int tIdx = sdf.sectionOperator(x, y + 1);
                    if (hasLinearArtifact(new BaseArtifactClassifier(vSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, tIdx)) {
                        hasError = true;
                    }
                }

                // Check diagonal neighbors
                if (!hasError && x > 0 && y > 0) {
                    int lIdx = sdf.sectionOperator(x - 1, y);
                    int bIdx = sdf.sectionOperator(x, y - 1);
                    int lbIdx = sdf.sectionOperator(x - 1, y - 1);
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, lIdx,
                            sdf.pixels, bIdx, sdf.pixels, lbIdx)) {
                        hasError = true;
                    }
                }

                if (!hasError && x < sdf.width - 1 && y > 0) {
                    int rIdx = sdf.sectionOperator(x + 1, y);
                    int bIdx = sdf.sectionOperator(x, y - 1);
                    int rbIdx = sdf.sectionOperator(x + 1, y - 1);
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, rIdx,
                            sdf.pixels, bIdx, sdf.pixels, rbIdx)) {
                        hasError = true;
                    }
                }

                if (!hasError && x > 0 && y < sdf.height - 1) {
                    int lIdx = sdf.sectionOperator(x - 1, y);
                    int tIdx = sdf.sectionOperator(x, y + 1);
                    int ltIdx = sdf.sectionOperator(x - 1, y + 1);
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, lIdx,
                            sdf.pixels, tIdx, sdf.pixels, ltIdx)) {
                        hasError = true;
                    }
                }

                if (!hasError && x < sdf.width - 1 && y < sdf.height - 1) {
                    int rIdx = sdf.sectionOperator(x + 1, y);
                    int tIdx = sdf.sectionOperator(x, y + 1);
                    int rtIdx = sdf.sectionOperator(x + 1, y + 1);
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag),
                            cm, sdf.pixels, cIdx, sdf.pixels, rIdx,
                            sdf.pixels, tIdx, sdf.pixels, rtIdx)) {
                        hasError = true;
                    }
                }

                if (hasError) {
                    setStencilValue(x, y, (byte) (getStencilValue(x, y) | Flags.ERROR));
                }
            }
        }
    }
















    //TODO 20251228 a couple helper methods to avoid massive one liner code blocks everywhere, just a limitation of our arrays.
    private byte getStencilValue(int x, int y) {
        int idx = stencil.sectionOperator(x, y);
        return stencil.pixels[idx];
    }

    private void setStencilValue(int x, int y, byte value) {
        int idx = stencil.sectionOperator(x, y);
        stencil.pixels[idx] = value;
    }

}
