package com.github.exiostorm.utils.msdf;

public class MathUtils {
    public static double median(double a, double b, double c) {
        if ((a <= b && b <= c) || (c <= b && b <= a)) {
            return b;
        } else if ((b <= a && a <= c) || (c <= a && a <= b)) {
            return a;
        } else {
            return c;
        }
    }
}
