package com.github.exiostorm.utils.msdf;

/**
 * Linear transformation of signed distance values.
 */
public class DistanceMapping {

    /**
     * Explicitly designates value as distance delta rather than an absolute distance.
     */
    public static class Delta {
        public final double value;

        public Delta(double distanceDelta) {
            this.value = distanceDelta;
        }

        public double getValue() {
            return value;
        }

        // Implicit conversion to double (similar to C++ operator double())
        public double toDouble() {
            return value;
        }

        @Override
        public String toString() {
            return "Delta(" + value + ")";
        }
    }

    private final double scale;
    private final double translate;

    // Static factory method
    public static DistanceMapping inverse(Range range) {
        double rangeWidth = range.getUpper() - range.getLower();
        return new DistanceMapping(rangeWidth, range.getLower() / (rangeWidth != 0 ? rangeWidth : 1));
    }

    // Default constructor
    public DistanceMapping() {
        this.scale = 1.0;
        this.translate = 0.0;
    }

    // Constructor from Range
    public DistanceMapping(Range range) {
        double rangeWidth = range.getUpper() - range.getLower();
        this.scale = 1.0 / rangeWidth;
        this.translate = -range.getLower();
    }

    // Private constructor for internal use
    DistanceMapping(double scale, double translate) {
        this.scale = scale;
        this.translate = translate;
    }

    // Apply mapping to absolute distance
    public double map(double d) {
        return scale * (d + translate);
    }

    // Apply mapping to distance delta
    public double map(Delta d) {
        return scale * d.value;
    }

    // Convenience overload for direct double values treated as deltas
    public double mapDelta(double deltaValue) {
        return scale * deltaValue;
    }

    // Get the inverse mapping
    public DistanceMapping inverse() {
        return new DistanceMapping(1.0 / scale, -scale * translate);
    }

    // Getters for internal values (useful for debugging)
    public double getScale() {
        return scale;
    }

    public double getTranslate() {
        return translate;
    }

    @Override
    public String toString() {
        return String.format("DistanceMapping(scale=%.6f, translate=%.6f)", scale, translate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DistanceMapping other = (DistanceMapping) obj;
        return Double.compare(other.scale, scale) == 0 &&
                Double.compare(other.translate, translate) == 0;
    }

    @Override
    public int hashCode() {
        long scaleHash = Double.doubleToLongBits(scale);
        long translateHash = Double.doubleToLongBits(translate);
        return (int) (scaleHash ^ (scaleHash >>> 32) ^ translateHash ^ (translateHash >>> 32));
    }
}
