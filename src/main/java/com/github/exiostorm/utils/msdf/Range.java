package com.github.exiostorm.utils.msdf;

/**
 * Represents the range between two real values.
 * For example, the range of representable signed distances.
 */
public class Range {

    private double lower;
    private double upper;

    // Default constructor - creates a range with symmetrical width around 0
    public Range(double symmetricalWidth) {
        this.lower = -0.5 * symmetricalWidth;
        this.upper = 0.5 * symmetricalWidth;
    }

    // Default constructor with width 0
    public Range() {
        this(0.0);
    }

    // Constructor with explicit bounds
    public Range(double lowerBound, double upperBound) {
        this.lower = lowerBound;
        this.upper = upperBound;
    }

    // Copy constructor
    public Range(Range other) {
        this.lower = other.lower;
        this.upper = other.upper;
    }

    // Getters
    public double getLower() {
        return lower;
    }

    public double getUpper() {
        return upper;
    }

    // Setters (for mutability if needed)
    public void setLower(double lower) {
        this.lower = lower;
    }

    public void setUpper(double upper) {
        this.upper = upper;
    }

    public void setBounds(double lower, double upper) {
        this.lower = lower;
        this.upper = upper;
    }

    // Multiply this range by a factor (modifies this range)
    public Range multiplyEquals(double factor) {
        this.lower *= factor;
        this.upper *= factor;
        return this;
    }

    // Divide this range by a divisor (modifies this range)
    public Range divideEquals(double divisor) {
        this.lower /= divisor;
        this.upper /= divisor;
        return this;
    }

    // Multiply and return new range (immutable operation)
    public Range multiply(double factor) {
        return new Range(lower * factor, upper * factor);
    }

    // Divide and return new range (immutable operation)
    public Range divide(double divisor) {
        return new Range(lower / divisor, upper / divisor);
    }

    // Static method for factor * range operation
    public static Range multiply(double factor, Range range) {
        return new Range(factor * range.lower, factor * range.upper);
    }

    // Utility methods
    public double getWidth() {
        return upper - lower;
    }

    public double getCenter() {
        return (lower + upper) * 0.5;
    }

    public boolean contains(double value) {
        return value >= lower && value <= upper;
    }

    public boolean isEmpty() {
        return lower >= upper;
    }

    // Clamp a value to this range
    public double clamp(double value) {
        if (value < lower) return lower;
        if (value > upper) return upper;
        return value;
    }

    // Normalize a value from this range to [0, 1]
    public double normalize(double value) {
        double width = getWidth();
        if (width == 0) return 0.0;
        return (value - lower) / width;
    }

    // Denormalize a value from [0, 1] to this range
    public double denormalize(double normalizedValue) {
        return lower + normalizedValue * getWidth();
    }

    @Override
    public String toString() {
        return String.format("Range[%.6f, %.6f]", lower, upper);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Range other = (Range) obj;
        return Double.compare(other.lower, lower) == 0 &&
                Double.compare(other.upper, upper) == 0;
    }

    @Override
    public int hashCode() {
        long lowerHash = Double.doubleToLongBits(lower);
        long upperHash = Double.doubleToLongBits(upper);
        return (int) (lowerHash ^ (lowerHash >>> 32) ^ upperHash ^ (upperHash >>> 32));
    }
}