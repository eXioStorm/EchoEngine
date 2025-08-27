package com.github.exiostorm.utils.msdf;

/**
 * Represents a signed distance and alignment, which together can be compared
 * to uniquely determine the closest edge segment.
 */
public class SignedDistance implements Comparable<SignedDistance> {
    public double distance;
    public double dot;

    /**
     * Default constructor - initializes distance to negative max value
     */
    public SignedDistance() {
        this.distance = -Double.MAX_VALUE;
        this.dot = 0;
    }

    /**
     * Constructor with distance and dot values
     */
    public SignedDistance(double distance, double dot) {
        this.distance = distance;
        this.dot = dot;
    }

    /**
     * Compare this SignedDistance to another
     * Returns negative if this < other, 0 if equal, positive if this > other
     */
    @Override
    public int compareTo(SignedDistance other) {
        double thisAbsDistance = Math.abs(this.distance);
        double otherAbsDistance = Math.abs(other.distance);

        if (thisAbsDistance < otherAbsDistance) {
            return -1;
        } else if (thisAbsDistance > otherAbsDistance) {
            return 1;
        } else {
            // Absolute distances are equal, compare dot values
            return Double.compare(this.dot, other.dot);
        }
    }

    /**
     * Check if this SignedDistance is less than another
     */
    public boolean isLessThan(SignedDistance other) {
        double thisAbsDistance = Math.abs(this.distance);
        double otherAbsDistance = Math.abs(other.distance);
        return thisAbsDistance < otherAbsDistance ||
                (thisAbsDistance == otherAbsDistance && this.dot < other.dot);
    }

    /**
     * Check if this SignedDistance is greater than another
     */
    public boolean isGreaterThan(SignedDistance other) {
        double thisAbsDistance = Math.abs(this.distance);
        double otherAbsDistance = Math.abs(other.distance);
        return thisAbsDistance > otherAbsDistance ||
                (thisAbsDistance == otherAbsDistance && this.dot > other.dot);
    }

    /**
     * Check if this SignedDistance is less than or equal to another
     */
    public boolean isLessThanOrEqual(SignedDistance other) {
        double thisAbsDistance = Math.abs(this.distance);
        double otherAbsDistance = Math.abs(other.distance);
        return thisAbsDistance < otherAbsDistance ||
                (thisAbsDistance == otherAbsDistance && this.dot <= other.dot);
    }

    /**
     * Check if this SignedDistance is greater than or equal to another
     */
    public boolean isGreaterThanOrEqual(SignedDistance other) {
        double thisAbsDistance = Math.abs(this.distance);
        double otherAbsDistance = Math.abs(other.distance);
        return thisAbsDistance > otherAbsDistance ||
                (thisAbsDistance == otherAbsDistance && this.dot >= other.dot);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SignedDistance that = (SignedDistance) obj;
        return Double.compare(that.distance, distance) == 0 &&
                Double.compare(that.dot, dot) == 0;
    }

    @Override
    public int hashCode() {
        long temp = Double.doubleToLongBits(distance);
        int result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(dot);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("SignedDistance{distance=%.6f, dot=%.6f}", distance, dot);
    }
}
