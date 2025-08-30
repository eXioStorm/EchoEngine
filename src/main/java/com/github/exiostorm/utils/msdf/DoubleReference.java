package com.github.exiostorm.utils.msdf;

public class DoubleReference {
    private double value;

    public DoubleReference() { this.value = 0.0; }
    public DoubleReference(double value) { this.value = value; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}
