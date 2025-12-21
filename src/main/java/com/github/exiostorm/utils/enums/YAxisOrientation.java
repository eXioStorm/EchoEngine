package com.github.exiostorm.utils.enums;

//This feels dumb as fuck, however it's the way it's done in the original msdfgen, so what do I know. maybe there's something I don't.
public enum YAxisOrientation {
    TRUE(true),
    FALSE(false);
    private final boolean bool;
    YAxisOrientation(boolean bool) {
        this.bool = bool;
    }
    public boolean getBool() {
        return bool;
    }
    private static final YAxisOrientation DEFAULT = FALSE;
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
