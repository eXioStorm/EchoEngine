package com.github.exiostorm.utils.msdf.enums;

public enum EdgeColorEnum {
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7);
    private final int value;

    EdgeColorEnum(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
    public static EdgeColorEnum fromValue(int value) {
        for (EdgeColorEnum color : EdgeColorEnum.values()) {
            if (color.value == value) {
                return color;
            }
        }
        throw new IllegalArgumentException("Invalid EdgeColor value: " + value);
    }
}