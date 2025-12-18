package com.github.exiostorm.utils.msdf.enums;

import com.github.exiostorm.utils.msdf.ColorHolder;

public enum EdgeColorEnum {
    BLACK(new ColorHolder(0)),
    CYAN(new ColorHolder(6)),
    MAGENTA(new ColorHolder(5)),
    YELLOW(new ColorHolder(3)),
    WHITE(new ColorHolder(7)),
    RED(new ColorHolder(1)),
    GREEN(new ColorHolder(2)),
    BLUE(new ColorHolder(4));
    private final ColorHolder value;

    EdgeColorEnum(ColorHolder value) {
        this.value = value;
    }
    public ColorHolder getValue() {
        return value;
    }
    public static EdgeColorEnum fromValue(int value) {
        for (EdgeColorEnum color : EdgeColorEnum.values()) {
            if (color.getValue().color == value) {
                return color;
            }
        }
        throw new IllegalArgumentException("Invalid EdgeColor value: " + value);
    }
}

/*
    BLACK(new ColorHolder(0)),
    CYAN(new ColorHolder(1)),
    MAGENTA(new ColorHolder(4)),
    YELLOW(new ColorHolder(2)),
    WHITE(new ColorHolder(7)),
    RED(new ColorHolder(6)),
    GREEN(new ColorHolder(3)),
    BLUE(new ColorHolder(5));
     */