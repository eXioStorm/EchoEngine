package com.github.exiostorm.utils.msdf;

public final class ErrorCorrectionConfig {

    // Equivalent of: static const double defaultMinDeviationRatio;
    public static final double DEFAULT_MIN_DEVIATION_RATIO =
            ErrorCorrectionConfig.DEFAULT_MIN_DEVIATION_RATIO_INTERNAL;

    // Equivalent of: static const double defaultMinImproveRatio;
    public static final double DEFAULT_MIN_IMPROVE_RATIO =
            ErrorCorrectionConfig.DEFAULT_MIN_IMPROVE_RATIO_INTERNAL;

    // Your code says these come from MSDFErrorCorrection: keep that consistent
    private static final double DEFAULT_MIN_DEVIATION_RATIO_INTERNAL =
            DprMSDFErrorCorrection.ErrorCorrectionConfig.DEFAULT_MIN_DEVIATION_RATIO;

    private static final double DEFAULT_MIN_IMPROVE_RATIO_INTERNAL =
            DprMSDFErrorCorrection.ErrorCorrectionConfig.DEFAULT_MIN_IMPROVE_RATIO;

    // ----------------------------------------------------------
    // Enum Mode
    // ----------------------------------------------------------
    public enum Mode {
        DISABLED,
        INDISCRIMINATE,
        EDGE_PRIORITY,
        EDGE_ONLY
    }

    // ----------------------------------------------------------
    // Enum DistanceCheckMode
    // ----------------------------------------------------------
    public enum DistanceCheckMode {
        DO_NOT_CHECK_DISTANCE,
        CHECK_DISTANCE_AT_EDGE,
        ALWAYS_CHECK_DISTANCE
    }

    // ----------------------------------------------------------
    // Fields
    // ----------------------------------------------------------
    public Mode mode;
    public DistanceCheckMode distanceCheckMode;

    public double minDeviationRatio;
    public double minImproveRatio;

    /**
     * Buffer equivalent to: byte* buffer
     * In Java this must be a byte[] (or null)
     */
    public byte[] buffer;

    // ----------------------------------------------------------
    // Constructor
    // ----------------------------------------------------------
    public ErrorCorrectionConfig() {
        this(
                Mode.EDGE_PRIORITY,
                DistanceCheckMode.CHECK_DISTANCE_AT_EDGE,
                DEFAULT_MIN_DEVIATION_RATIO,
                DEFAULT_MIN_IMPROVE_RATIO,
                null
        );
    }

    public ErrorCorrectionConfig(
            Mode mode,
            DistanceCheckMode distanceCheckMode,
            double minDeviationRatio,
            double minImproveRatio,
            byte[] buffer
    ) {
        this.mode = mode;
        this.distanceCheckMode = distanceCheckMode;
        this.minDeviationRatio = minDeviationRatio;
        this.minImproveRatio = minImproveRatio;
        this.buffer = buffer;
    }
}
