package com.github.exiostorm.utils.msdf;

public final class Arithmetics {

    private Arithmetics() {}

    public static float min(float a, float b) {
        return Math.min(b, a);
    }

    public static float max(float a, float b) {
        return Math.max(a, b);
    }

    public static float median(float a, float b, float c) {
        return max(min(a, b), min(max(a, b), c));
    }

    /**
     * Returns the weighted average of a and b
     */
    public static float mix(float a, float b, double weight) {
        return (float) ((1.0 - weight) * a + weight * b);
    }

    public static double mix(double a, double b, double weight) {
        return (1.0 - weight) * a + weight * b;
    }

    public static float clamp01(float n) {
        return (n >= 0f && n <= 1f) ? n : ((n > 0f) ? 1f : 0f);
    }

    /**
     * Clamps the number to the interval from 0 to 1
     */
    public static float clamp(float n) {
        return n >= 0.0f && n <= 1.0f ? n : (n > 0.0f ? 1.0f : 0.0f);
    }

    public static double clamp(double n) {
        return n >= 0.0 && n <= 1.0 ? n : (n > 0.0 ? 1.0 : 0.0);
    }

    /**
     * Clamps the number to the interval from 0 to b
     */
    public static float clamp(float n, float b) {
        return n >= 0.0f && n <= b ? n : (n > 0.0f ? b : 0.0f);
    }

    public static double clamp(double n, double b) {
        return n >= 0.0 && n <= b ? n : (n > 0.0 ? b : 0.0);
    }

    public static int clamp(int n, int b) {
        return n >= 0 && n <= b ? n : (n > 0 ? b : 0);
    }

    /**
     * Clamps the number to the interval from a to b
     */
    public static float clamp(float n, float a, float b) {
        return n >= a && n <= b ? n : (n < a ? a : b);
    }

    public static double clamp(double n, double a, double b) {
        return n >= a && n <= b ? n : (n < a ? a : b);
    }

    public static int clamp(int n, int a, int b) {
        return n >= a && n <= b ? n : (n < a ? a : b);
    }

    public static int sign(float n) {
        return (0f < n ? 1 : 0) - (n < 0f ? 1 : 0);
    }

    public static int nonZeroSign(float n) {
        return (n > 0f) ? 1 : -1;
    }
}
