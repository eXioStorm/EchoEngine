package com.github.exiostorm.utils.enums;

//This feels dumb as fuck, however it's the way it's done in the original msdfgen, so what do I know. maybe there's something I don't.
public enum YAxisOrientation {
    Y_UPWARD(true),
    Y_DOWNWARD(false);
    private final boolean bool;
    YAxisOrientation(boolean bool) {
        this.bool = bool;
    }
    public boolean getBool() {
        return bool;
    }
    private static YAxisOrientation DEFAULT = Y_UPWARD;
    public static YAxisOrientation getDefault() {
        return DEFAULT;
    }
    public static YAxisOrientation fromBool(boolean bool) {
        for (YAxisOrientation orientation : values()) {
            if (orientation.bool == bool) {
                return orientation;
            }
        }
        throw new IllegalArgumentException("Invalid YAxisOrientation bool: " + bool);
    }
}
