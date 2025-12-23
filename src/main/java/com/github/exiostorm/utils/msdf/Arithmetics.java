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

    public static float mix(float a, float b, float weight) {
        return ((1.0f - weight) * a) + (weight * b);
    }

    public static float clamp01(float n) {
        return (n >= 0f && n <= 1f) ? n : ((n > 0f) ? 1f : 0f);
    }

    public static float clamp(float n, float b) {
        return (n >= 0f && n <= b) ? n : ((n > 0f) ? b : 0f);
    }

    public static float clamp(float n, float a, float b) {
        return (n >= a && n <= b) ? n : (n < a ? a : b);
    }

    public static int sign(float n) {
        return (0f < n ? 1 : 0) - (n < 0f ? 1 : 0);
    }

    public static int nonZeroSign(float n) {
        return (n > 0f) ? 1 : -1;
    }
}
