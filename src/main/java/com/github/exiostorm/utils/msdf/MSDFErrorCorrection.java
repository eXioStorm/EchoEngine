package com.github.exiostorm.utils.msdf;

import com.github.exiostorm.utils.enums.YAxisOrientation;
import com.github.exiostorm.utils.msdf.enums.EdgeColorEnum;
import static com.github.exiostorm.utils.msdf.MathUtils.*;
import org.joml.Vector2d;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
//TODO 20251218 This class is not identical to C++ implementation. After fixing this, and BitmapRef we should be done!
/**
 * Performs error correction on a computed MSDF to eliminate interpolation artifacts.
 * This is a low-level class, you may want to use the API in msdf-error-correction.h instead.
 */
public class MSDFErrorCorrection {

    private static final double ARTIFACT_T_EPSILON = 0.01;
    private static final double PROTECTION_RADIUS_TOLERANCE = 1.001;

    private static final int CLASSIFIER_FLAG_CANDIDATE = 0x01;
    private static final int CLASSIFIER_FLAG_ARTIFACT = 0x02;
    private final DistanceMapping.Delta dist = new DistanceMapping.Delta(1);

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
    private static class ShapeDistanceChecker<T extends ContourCombiners.ContourCombiner> {

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
                    double rawDistance;
                    Object distanceResult = parent.distanceFinder.distance(new Vector2d(parent.shapeCoord).add(shapeCoordOffset));
                    if (distanceResult instanceof Number) {
                        rawDistance = ((Number) distanceResult).doubleValue();
                    } else {
                        //TODO ChatGPT said to not accept this case
                        throw new IllegalStateException("ShapeDistanceFinder returned non-numeric distance");
                        //rawDistance = 0.0; // fallback
                    }
                    float refPSD = (float)parent.distanceMapping.map(rawDistance);

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

        private final SimpleTrueShapeDistanceFinder distanceFinder;
        private final BitmapRef sdf;
        private final DistanceMapping distanceMapping;
        private Vector2d texelSize;
        private final double minImproveRatio;

        public ShapeDistanceChecker(BitmapRef sdf, MsdfShape msdfShape, Projection projection,
                                    DistanceMapping distanceMapping, double minImproveRatio) {
            this.distanceFinder = new SimpleTrueShapeDistanceFinder(msdfShape);
            this.sdf = sdf;
            this.distanceMapping = distanceMapping;
            this.minImproveRatio = minImproveRatio;
            this.texelSize = projection.unprojectVector(new Vector2d(1.0, 1.0));
            //TODO 20251219 need to re-implement this after I move this boolean to BitmapRef.
            /*if (msdfShape.inverseYAxis) {
                this.texelSize.y = -this.texelSize.y;
            }*/
        }

        public ArtifactClassifier classifier(Vector2d direction, double span) {
            return new ArtifactClassifier(this, direction, span);
        }
        private static void interpolate(float[] result, BitmapRef bitmap, Vector2d coord) {
            // Clamp to [0, width] and [0, height] FIRST (C++ clamp behavior)
            double px = Math.max(0.0, Math.min((double) bitmap.getWidth(), coord.x));
            double py = Math.max(0.0, Math.min((double) bitmap.getHeight(), coord.y));

            // THEN subtract 0.5
            px -= 0.5;
            py -= 0.5;

            // Compute integer coordinates
            int x0 = (int) Math.floor(px);
            int y0 = (int) Math.floor(py);
            int x1 = x0 + 1;
            int y1 = y0 + 1;

            // Compute fractional parts for interpolation
            double tx = px - x0;
            double ty = py - y0;

            // Clamp integer coordinates to valid array indices [0, width-1] and [0, height-1]
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            x0 = Math.max(0, Math.min(w - 1, x0));
            x1 = Math.max(0, Math.min(w - 1, x1));
            y0 = Math.max(0, Math.min(h - 1, y0));
            y1 = Math.max(0, Math.min(h - 1, y1));

            // Bilinear interpolation
            int channels = result.length;
            for (int c = 0; c < channels; ++c) {
                float v00 = ((Number) bitmap.getPixel(x0, y0, c)).floatValue();
                float v10 = ((Number) bitmap.getPixel(x1, y0, c)).floatValue();
                float v01 = ((Number) bitmap.getPixel(x0, y1, c)).floatValue();
                float v11 = ((Number) bitmap.getPixel(x1, y1, c)).floatValue();

                // Mix horizontally at bottom and top
                double vBottom = v00 + tx * (v10 - v00);
                double vTop = v01 + tx * (v11 - v01);

                // Mix vertically
                result[c] = (float) (vBottom + ty * (vTop - vBottom));
            }
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
        if (stencil.getPixels() instanceof byte[]) {
            Arrays.fill((byte[]) stencil.getPixels(), (byte)0);
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
    /// Flags all texels that are interpolated at corners as protected.
    public void protectCorners(MsdfShape msdfShape) {
        // Reorient the stencil based on the shape's Y-axis orientation
        stencil.reorient(msdfShape.getYAxisOrientation());

        for (Contours.Contour contour : msdfShape.contours) {
            if (!contour.edges.isEmpty()) {
                // Get the last edge as prevEdge
                EdgeSegment prevEdge = contour.edges.get(contour.edges.size() - 1).edge;

                for (EdgeHolder edgeHolder : contour.edges) {
                    EdgeSegment edge = edgeHolder.edge;

                    // Find colors common to both edges
                    int commonColor = prevEdge.edgeColor & edge.edgeColor;

                    // If the color changes from prevEdge to edge, this is a corner.
                    // The C++ condition is: !(commonColor & (commonColor - 1))
                    // This checks if commonColor is 0 or a power of 2 (has at most 1 bit set)
                    // This means the edges don't share multiple colors - indicating a corner
                    if ((commonColor & (commonColor - 1)) == 0) {
                        // Find the four texels that envelop the corner and mark them as protected.
                        Vector2d p = transformation.project(edge.point(0));
                        int l = (int) Math.floor(p.x - 0.5);
                        int b = (int) Math.floor(p.y - 0.5);
                        int r = l + 1;
                        int t = b + 1;

                        // Check that the positions are within bounds.
                        if (l < stencil.getWidth() && b < stencil.getHeight() && r >= 0 && t >= 0) {
                            if (l >= 0 && b >= 0) {
                                byte currentValue = (Byte) stencil.getPixel(l, b, 0);
                                stencil.setPixel(l, b, 0, (byte)(currentValue | Flags.PROTECTED));
                            }
                            if (r < stencil.getWidth() && b >= 0) {
                                byte currentValue = (Byte) stencil.getPixel(r, b, 0);
                                stencil.setPixel(r, b, 0, (byte)(currentValue | Flags.PROTECTED));
                            }
                            if (l >= 0 && t < stencil.getHeight()) {
                                byte currentValue = (Byte) stencil.getPixel(l, t, 0);
                                stencil.setPixel(l, t, 0, (byte)(currentValue | Flags.PROTECTED));
                            }
                            if (r < stencil.getWidth() && t < stencil.getHeight()) {
                                byte currentValue = (Byte) stencil.getPixel(r, t, 0);
                                stencil.setPixel(r, t, 0, (byte)(currentValue | Flags.PROTECTED));
                            }
                        }
                    }
                    prevEdge = edge;
                }
            }
        }
    }

    /// Flags all texels that contribute to edges as protected.
    /// Flags all texels that contribute to edges as protected.
    public void protectEdges(BitmapRef sdf) {
        float radius;

        // CRITICAL FIX: Reorient stencil to match sdf orientation
        stencil.reorient(sdf.yOrientation);

        // Horizontal texel pairs
        radius = (float)(PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(transformation.getDistanceMapping().map(dist), 0)).length());

        for (int y = 0; y < sdf.getHeight(); ++y) {
            for (int x = 0; x < sdf.getWidth() - 1; ++x) {
                // Get left pixel (current)
                float left0 = (Float) sdf.getPixel(x, y, 0);
                float left1 = (Float) sdf.getPixel(x, y, 1);
                float left2 = (Float) sdf.getPixel(x, y, 2);

                // Get right pixel (next)
                float right0 = (Float) sdf.getPixel(x + 1, y, 0);
                float right1 = (Float) sdf.getPixel(x + 1, y, 1);
                float right2 = (Float) sdf.getPixel(x + 1, y, 2);

                float lm = median(left0, left1, left2);
                float rm = median(right0, right1, right2);

                if (Math.abs(lm - 0.5f) + Math.abs(rm - 0.5f) < radius) {
                    float[] left = {left0, left1, left2};
                    float[] right = {right0, right1, right2};
                    int mask = edgeBetweenTexels(left, right);
                    protectExtremeChannels(stencil, x, y, left, lm, mask);
                    protectExtremeChannels(stencil, x + 1, y, right, rm, mask);
                }
            }
        }

        // Vertical texel pairs
        radius = (float)(PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(0, transformation.getDistanceMapping().map(dist))).length());

        for (int y = 0; y < sdf.getHeight() - 1; ++y) {
            for (int x = 0; x < sdf.getWidth(); ++x) {
                // Get bottom pixel (current)
                float bottom0 = (Float) sdf.getPixel(x, y, 0);
                float bottom1 = (Float) sdf.getPixel(x, y, 1);
                float bottom2 = (Float) sdf.getPixel(x, y, 2);

                // Get top pixel (next row)
                float top0 = (Float) sdf.getPixel(x, y + 1, 0);
                float top1 = (Float) sdf.getPixel(x, y + 1, 1);
                float top2 = (Float) sdf.getPixel(x, y + 1, 2);

                float bm = median(bottom0, bottom1, bottom2);
                float tm = median(top0, top1, top2);

                if (Math.abs(bm - 0.5f) + Math.abs(tm - 0.5f) < radius) {
                    float[] bottom = {bottom0, bottom1, bottom2};
                    float[] top = {top0, top1, top2};

                    int mask = edgeBetweenTexels(bottom, top);
                    protectExtremeChannels(stencil, x, y, bottom, bm, mask);
                    protectExtremeChannels(stencil, x, y + 1, top, tm, mask);
                }
            }
        }

        // Diagonal texel pairs
        // CRITICAL FIX: Use single parameter Vector2d constructor (not two separate values)
        radius = (float)(PROTECTION_RADIUS_TOLERANCE *
                transformation.unprojectVector(new Vector2d(
                        transformation.getDistanceMapping().map(dist)
                )).length());

        for (int y = 0; y < sdf.getHeight() - 1; ++y) {
            for (int x = 0; x < sdf.getWidth() - 1; ++x) {
                // Get all four pixels in the quad
                float lb0 = (Float) sdf.getPixel(x, y, 0);
                float lb1 = (Float) sdf.getPixel(x, y, 1);
                float lb2 = (Float) sdf.getPixel(x, y, 2);

                float rb0 = (Float) sdf.getPixel(x + 1, y, 0);
                float rb1 = (Float) sdf.getPixel(x + 1, y, 1);
                float rb2 = (Float) sdf.getPixel(x + 1, y, 2);

                float lt0 = (Float) sdf.getPixel(x, y + 1, 0);
                float lt1 = (Float) sdf.getPixel(x, y + 1, 1);
                float lt2 = (Float) sdf.getPixel(x, y + 1, 2);

                float rt0 = (Float) sdf.getPixel(x + 1, y + 1, 0);
                float rt1 = (Float) sdf.getPixel(x + 1, y + 1, 1);
                float rt2 = (Float) sdf.getPixel(x + 1, y + 1, 2);

                float mlb = median(lb0, lb1, lb2);
                float mrb = median(rb0, rb1, rb2);
                float mlt = median(lt0, lt1, lt2);
                float mrt = median(rt0, rt1, rt2);

                // Check left-bottom to right-top diagonal
                if (Math.abs(mlb - 0.5f) + Math.abs(mrt - 0.5f) < radius) {
                    float[] lb = {lb0, lb1, lb2};
                    float[] rt = {rt0, rt1, rt2};
                    int mask = edgeBetweenTexels(lb, rt);
                    protectExtremeChannels(stencil, x, y, lb, mlb, mask);
                    protectExtremeChannels(stencil, x + 1, y + 1, rt, mrt, mask);
                }

                // Check right-bottom to left-top diagonal
                if (Math.abs(mrb - 0.5f) + Math.abs(mlt - 0.5f) < radius) {
                    float[] rb = {rb0, rb1, rb2};
                    float[] lt = {lt0, lt1, lt2};
                    int mask = edgeBetweenTexels(rb, lt);
                    protectExtremeChannels(stencil, x + 1, y, rb, mrb, mask);
                    protectExtremeChannels(stencil, x, y + 1, lt, mlt, mask);
                }
            }
        }
    }

    /// Flags all texels as protected.
    public void protectAll() {
        byte[] pixels = (byte[]) stencil.getPixels();
        for (int i = 0; i < pixels.length; ++i) {
            pixels[i] |= Flags.PROTECTED;
        }
    }

    /// Flags texels that are expected to cause interpolation artifacts based on analysis of the SDF only.
    public void findErrors(BitmapRef sdf) {
        // CRITICAL: Reorient stencil to match SDF's orientation
        stencil.reorient(sdf.getYOrientation());

        // Compute the expected deltas between values of horizontally, vertically, and diagonally adjacent texels.
        double hSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(dist), 0)).length();
        double vSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(0, transformation.getDistanceMapping().map(dist))).length();
        double dSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(dist))
        ).length();

        // Inspect all texels.
        for (int y = 0; y < sdf.getHeight(); ++y) {
            for (int x = 0; x < sdf.getWidth(); ++x) {
                // Get current pixel's channel values
                float c0 = (Float) sdf.getPixel(x, y, 0);
                float c1 = (Float) sdf.getPixel(x, y, 1);
                float c2 = (Float) sdf.getPixel(x, y, 2);
                float[] c = {c0, c1, c2};

                float cm = median(c[0], c[1], c[2]);
                boolean protectedFlag = ((Byte) stencil.getPixel(x, y, 0) & Flags.PROTECTED) != 0;

                // Cache neighbor values (like C++ does with pointers)
                float[] l = null, b = null, r = null, t = null;
                boolean hasError = false;

                // Check horizontal and vertical neighbors first
                if (x > 0) {
                    l = new float[] {
                            (Float) sdf.getPixel(x - 1, y, 0),
                            (Float) sdf.getPixel(x - 1, y, 1),
                            (Float) sdf.getPixel(x - 1, y, 2)
                    };
                    if (hasLinearArtifact(new BaseArtifactClassifier(hSpan, protectedFlag), cm, c, l)) {
                        hasError = true;
                    }
                }

                if (y > 0) {
                    b = new float[] {
                            (Float) sdf.getPixel(x, y - 1, 0),
                            (Float) sdf.getPixel(x, y - 1, 1),
                            (Float) sdf.getPixel(x, y - 1, 2)
                    };
                    if (hasLinearArtifact(new BaseArtifactClassifier(vSpan, protectedFlag), cm, c, b)) {
                        hasError = true;
                    }
                }

                if (x < sdf.getWidth() - 1) {
                    r = new float[] {
                            (Float) sdf.getPixel(x + 1, y, 0),
                            (Float) sdf.getPixel(x + 1, y, 1),
                            (Float) sdf.getPixel(x + 1, y, 2)
                    };
                    if (hasLinearArtifact(new BaseArtifactClassifier(hSpan, protectedFlag), cm, c, r)) {
                        hasError = true;
                    }
                }

                if (y < sdf.getHeight() - 1) {
                    t = new float[] {
                            (Float) sdf.getPixel(x, y + 1, 0),
                            (Float) sdf.getPixel(x, y + 1, 1),
                            (Float) sdf.getPixel(x, y + 1, 2)
                    };
                    if (hasLinearArtifact(new BaseArtifactClassifier(vSpan, protectedFlag), cm, c, t)) {
                        hasError = true;
                    }
                }

                // Check diagonal neighbors - reuse cached values when possible
                if (x > 0 && y > 0) {
                    // Lazily load l and b if not already fetched
                    if (l == null) {
                        l = new float[] {
                                (Float) sdf.getPixel(x - 1, y, 0),
                                (Float) sdf.getPixel(x - 1, y, 1),
                                (Float) sdf.getPixel(x - 1, y, 2)
                        };
                    }
                    if (b == null) {
                        b = new float[] {
                                (Float) sdf.getPixel(x, y - 1, 0),
                                (Float) sdf.getPixel(x, y - 1, 1),
                                (Float) sdf.getPixel(x, y - 1, 2)
                        };
                    }
                    float[] lb = new float[] {
                            (Float) sdf.getPixel(x - 1, y - 1, 0),
                            (Float) sdf.getPixel(x - 1, y - 1, 1),
                            (Float) sdf.getPixel(x - 1, y - 1, 2)
                    };
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag), cm, c, l, b, lb)) {
                        hasError = true;
                    }
                }

                if (x < sdf.getWidth() - 1 && y > 0) {
                    if (r == null) {
                        r = new float[] {
                                (Float) sdf.getPixel(x + 1, y, 0),
                                (Float) sdf.getPixel(x + 1, y, 1),
                                (Float) sdf.getPixel(x + 1, y, 2)
                        };
                    }
                    if (b == null) {
                        b = new float[] {
                                (Float) sdf.getPixel(x, y - 1, 0),
                                (Float) sdf.getPixel(x, y - 1, 1),
                                (Float) sdf.getPixel(x, y - 1, 2)
                        };
                    }
                    float[] rb = new float[] {
                            (Float) sdf.getPixel(x + 1, y - 1, 0),
                            (Float) sdf.getPixel(x + 1, y - 1, 1),
                            (Float) sdf.getPixel(x + 1, y - 1, 2)
                    };
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag), cm, c, r, b, rb)) {
                        hasError = true;
                    }
                }

                if (x > 0 && y < sdf.getHeight() - 1) {
                    if (l == null) {
                        l = new float[] {
                                (Float) sdf.getPixel(x - 1, y, 0),
                                (Float) sdf.getPixel(x - 1, y, 1),
                                (Float) sdf.getPixel(x - 1, y, 2)
                        };
                    }
                    if (t == null) {
                        t = new float[] {
                                (Float) sdf.getPixel(x, y + 1, 0),
                                (Float) sdf.getPixel(x, y + 1, 1),
                                (Float) sdf.getPixel(x, y + 1, 2)
                        };
                    }
                    float[] lt = new float[] {
                            (Float) sdf.getPixel(x - 1, y + 1, 0),
                            (Float) sdf.getPixel(x - 1, y + 1, 1),
                            (Float) sdf.getPixel(x - 1, y + 1, 2)
                    };
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag), cm, c, l, t, lt)) {
                        hasError = true;
                    }
                }

                if (x < sdf.getWidth() - 1 && y < sdf.getHeight() - 1) {
                    if (r == null) {
                        r = new float[] {
                                (Float) sdf.getPixel(x + 1, y, 0),
                                (Float) sdf.getPixel(x + 1, y, 1),
                                (Float) sdf.getPixel(x + 1, y, 2)
                        };
                    }
                    if (t == null) {
                        t = new float[] {
                                (Float) sdf.getPixel(x, y + 1, 0),
                                (Float) sdf.getPixel(x, y + 1, 1),
                                (Float) sdf.getPixel(x, y + 1, 2)
                        };
                    }
                    float[] rt = new float[] {
                            (Float) sdf.getPixel(x + 1, y + 1, 0),
                            (Float) sdf.getPixel(x + 1, y + 1, 1),
                            (Float) sdf.getPixel(x + 1, y + 1, 2)
                    };
                    if (hasDiagonalArtifact(new BaseArtifactClassifier(dSpan, protectedFlag), cm, c, r, t, rt)) {
                        hasError = true;
                    }
                }

                // Mark the stencil if an error was detected
                if (hasError) {
                    byte currentValue = (Byte) stencil.getPixel(x, y, 0);
                    stencil.setPixel(x, y, 0, (byte)(currentValue | Flags.ERROR));
                }
            }
        }
    }

    /// Flags texels that are expected to cause interpolation artifacts based on analysis of the SDF and comparison with the exact shape distance.
    public void findErrorsWithShape(BitmapRef sdf, MsdfShape msdfShape) {
        sdf.reorient(msdfShape.getYAxisOrientation());
        stencil.reorient(sdf.yOrientation);  // FIX #1: Direct pass, no logic reversal

        double hSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(dist), 0)).length();
        double vSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(0, transformation.getDistanceMapping().map(dist))).length();
        double dSpan = minDeviationRatio * transformation.unprojectVector(
                new Vector2d(transformation.getDistanceMapping().map(dist))
        ).length();

        ShapeDistanceChecker shapeDistanceChecker = new ShapeDistanceChecker(sdf, msdfShape, transformation,
                transformation.getDistanceMapping(), minImproveRatio);

        // Iterate over all pixels
        for (int y = 0; y < sdf.getHeight(); ++y) {
            for (int x = 0; x < sdf.getWidth(); ++x) {
                // Check if already marked as error
                byte stencilValue = ((Number) stencil.getPixel(x, y, 0)).byteValue();
                if ((stencilValue & Flags.ERROR) != 0) {
                    continue;
                }

                // Get current pixel's channel values
                float[] c = new float[] {
                        ((Number) sdf.getPixel(x, y, 0)).floatValue(),
                        ((Number) sdf.getPixel(x, y, 1)).floatValue(),
                        ((Number) sdf.getPixel(x, y, 2)).floatValue()
                };

                // Set up the shape distance checker for this pixel
                shapeDistanceChecker.shapeCoord = transformation.unproject(new Vector2d(x + 0.5, y + 0.5));
                shapeDistanceChecker.sdfCoord = new Vector2d(x + 0.5, y + 0.5);
                shapeDistanceChecker.msd = c;
                shapeDistanceChecker.protectedFlag = ((stencilValue & Flags.PROTECTED) != 0);
                float cm = median(c[0], c[1], c[2]);

                // Cache neighbor pointers for reuse (like the C++ version)
                float[] l = null, b = null, r = null, t = null;
                boolean hasError = false;

                // Check horizontal and vertical neighbors
                if (x > 0) {
                    l = new float[] {
                            ((Number) sdf.getPixel(x - 1, y, 0)).floatValue(),
                            ((Number) sdf.getPixel(x - 1, y, 1)).floatValue(),
                            ((Number) sdf.getPixel(x - 1, y, 2)).floatValue()
                    };
                    if (hasLinearArtifact(shapeDistanceChecker.classifier(new Vector2d(-1, 0), hSpan), cm, c, l)) {
                        hasError = true;
                    }
                }
                if (y > 0) {
                    b = new float[] {
                            ((Number) sdf.getPixel(x, y - 1, 0)).floatValue(),
                            ((Number) sdf.getPixel(x, y - 1, 1)).floatValue(),
                            ((Number) sdf.getPixel(x, y - 1, 2)).floatValue()
                    };
                    if (hasLinearArtifact(shapeDistanceChecker.classifier(new Vector2d(0, -1), vSpan), cm, c, b)) {
                        hasError = true;
                    }
                }
                if (x < sdf.getWidth() - 1) {
                    r = new float[] {
                            ((Number) sdf.getPixel(x + 1, y, 0)).floatValue(),
                            ((Number) sdf.getPixel(x + 1, y, 1)).floatValue(),
                            ((Number) sdf.getPixel(x + 1, y, 2)).floatValue()
                    };
                    if (hasLinearArtifact(shapeDistanceChecker.classifier(new Vector2d(+1, 0), hSpan), cm, c, r)) {
                        hasError = true;
                    }
                }
                if (y < sdf.getHeight() - 1) {
                    t = new float[] {
                            ((Number) sdf.getPixel(x, y + 1, 0)).floatValue(),
                            ((Number) sdf.getPixel(x, y + 1, 1)).floatValue(),
                            ((Number) sdf.getPixel(x, y + 1, 2)).floatValue()
                    };
                    if (hasLinearArtifact(shapeDistanceChecker.classifier(new Vector2d(0, +1), vSpan), cm, c, t)) {
                        hasError = true;
                    }
                }

                // Check diagonal neighbors - fetch diagonal pixels directly as needed
                if (x > 0 && y > 0) {
                    if (l == null) {
                        l = new float[] {
                                ((Number) sdf.getPixel(x - 1, y, 0)).floatValue(),
                                ((Number) sdf.getPixel(x - 1, y, 1)).floatValue(),
                                ((Number) sdf.getPixel(x - 1, y, 2)).floatValue()
                        };
                    }
                    if (b == null) {
                        b = new float[] {
                                ((Number) sdf.getPixel(x, y - 1, 0)).floatValue(),
                                ((Number) sdf.getPixel(x, y - 1, 1)).floatValue(),
                                ((Number) sdf.getPixel(x, y - 1, 2)).floatValue()
                        };
                    }
                    float[] lb = new float[] {
                            ((Number) sdf.getPixel(x - 1, y - 1, 0)).floatValue(),
                            ((Number) sdf.getPixel(x - 1, y - 1, 1)).floatValue(),
                            ((Number) sdf.getPixel(x - 1, y - 1, 2)).floatValue()
                    };
                    if (hasDiagonalArtifact(shapeDistanceChecker.classifier(new Vector2d(-1, -1), dSpan), cm, c, l, b, lb)) {
                        hasError = true;
                    }
                }

                if (x < sdf.getWidth() - 1 && y > 0) {
                    if (r == null) {
                        r = new float[] {
                                ((Number) sdf.getPixel(x + 1, y, 0)).floatValue(),
                                ((Number) sdf.getPixel(x + 1, y, 1)).floatValue(),
                                ((Number) sdf.getPixel(x + 1, y, 2)).floatValue()
                        };
                    }
                    if (b == null) {
                        b = new float[] {
                                ((Number) sdf.getPixel(x, y - 1, 0)).floatValue(),
                                ((Number) sdf.getPixel(x, y - 1, 1)).floatValue(),
                                ((Number) sdf.getPixel(x, y - 1, 2)).floatValue()
                        };
                    }
                    float[] rb = new float[] {
                            ((Number) sdf.getPixel(x + 1, y - 1, 0)).floatValue(),
                            ((Number) sdf.getPixel(x + 1, y - 1, 1)).floatValue(),
                            ((Number) sdf.getPixel(x + 1, y - 1, 2)).floatValue()
                    };
                    if (hasDiagonalArtifact(shapeDistanceChecker.classifier(new Vector2d(+1, -1), dSpan), cm, c, r, b, rb)) {
                        hasError = true;
                    }
                }

                if (x > 0 && y < sdf.getHeight() - 1) {
                    if (l == null) {
                        l = new float[] {
                                ((Number) sdf.getPixel(x - 1, y, 0)).floatValue(),
                                ((Number) sdf.getPixel(x - 1, y, 1)).floatValue(),
                                ((Number) sdf.getPixel(x - 1, y, 2)).floatValue()
                        };
                    }
                    if (t == null) {
                        t = new float[] {
                                ((Number) sdf.getPixel(x, y + 1, 0)).floatValue(),
                                ((Number) sdf.getPixel(x, y + 1, 1)).floatValue(),
                                ((Number) sdf.getPixel(x, y + 1, 2)).floatValue()
                        };
                    }
                    float[] lt = new float[] {
                            ((Number) sdf.getPixel(x - 1, y + 1, 0)).floatValue(),
                            ((Number) sdf.getPixel(x - 1, y + 1, 1)).floatValue(),
                            ((Number) sdf.getPixel(x - 1, y + 1, 2)).floatValue()
                    };
                    if (hasDiagonalArtifact(shapeDistanceChecker.classifier(new Vector2d(-1, +1), dSpan), cm, c, l, t, lt)) {
                        hasError = true;
                    }
                }

                if (x < sdf.getWidth() - 1 && y < sdf.getHeight() - 1) {
                    if (r == null) {
                        r = new float[] {
                                ((Number) sdf.getPixel(x + 1, y, 0)).floatValue(),
                                ((Number) sdf.getPixel(x + 1, y, 1)).floatValue(),
                                ((Number) sdf.getPixel(x + 1, y, 2)).floatValue()
                        };
                    }
                    if (t == null) {
                        t = new float[] {
                                ((Number) sdf.getPixel(x, y + 1, 0)).floatValue(),
                                ((Number) sdf.getPixel(x, y + 1, 1)).floatValue(),
                                ((Number) sdf.getPixel(x, y + 1, 2)).floatValue()
                        };
                    }
                    float[] rt = new float[] {
                            ((Number) sdf.getPixel(x + 1, y + 1, 0)).floatValue(),
                            ((Number) sdf.getPixel(x + 1, y + 1, 1)).floatValue(),
                            ((Number) sdf.getPixel(x + 1, y + 1, 2)).floatValue()
                    };
                    if (hasDiagonalArtifact(shapeDistanceChecker.classifier(new Vector2d(+1, +1), dSpan), cm, c, r, t, rt)) {
                        hasError = true;
                    }
                }

                if (hasError) {
                    //TODO 20251222 we can't cast float to byte. entire Bitmap-Ref, Section, Const's, etc. need re-implemented.
                    byte currentValue = ((Number) stencil.getPixel(x, y, 0)).byteValue();
                    stencil.setPixel(x, y, 0, (currentValue | Flags.ERROR));
                    //stencil.setPixel(x, y, 0, currentValue);
                }
            }
        }
    }

    /// Modifies the MSDF so that all texels with the error flag are converted to single-channel.
    public void apply(BitmapRef sdf) {
        // Reorient the sdf bitmap to match the stencil's orientation
        sdf.reorient(stencil.yOrientation);

        // Iterate through all pixels
        for (int y = 0; y < sdf.getHeight(); ++y) {
            for (int x = 0; x < sdf.getWidth(); ++x) {
                // Check if this pixel has an error flag
                byte stencilValue = ((Number) stencil.getPixel(x, y, 0)).byteValue();
                if ((stencilValue & Flags.ERROR) != 0) {
                    // Get the three channel values
                    float ch0 = ((Number) sdf.getPixel(x, y, 0)).floatValue();
                    float ch1 = ((Number) sdf.getPixel(x, y, 1)).floatValue();
                    float ch2 = ((Number) sdf.getPixel(x, y, 2)).floatValue();

                    // Calculate median
                    float m = median(ch0, ch1, ch2);

                    // Set all color channels to the median
                    sdf.setPixel(x, y, 0, m);
                    sdf.setPixel(x, y, 1, m);
                    sdf.setPixel(x, y, 2, m);
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

            byte currentValue = (Byte) stencil.getPixel(x, y, 0);
            stencil.setPixel(x, y, 0, (byte)(currentValue | Flags.PROTECTED));
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