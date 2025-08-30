package com.github.exiostorm.utils.msdf.enums;

import com.github.exiostorm.utils.msdf.EdgeColor;

public enum EdgeColorEnum {
    BLACK(new EdgeColor(0)),
    RED(new EdgeColor(1)),
    GREEN(new EdgeColor(2)),
    YELLOW(new EdgeColor(3)),
    BLUE(new EdgeColor(4)),
    MAGENTA(new EdgeColor(5)),
    CYAN(new EdgeColor(6)),
    WHITE(new EdgeColor(7));
    private final EdgeColor value;

    EdgeColorEnum(EdgeColor value) {
        this.value = value;
    }
    public EdgeColor getValue() {
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