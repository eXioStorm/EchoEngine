package com.github.exiostorm.utils.msdf;

public class GeneratorConfig {
    //TODO 20251219 see if we can ask Claude to put yOrientation boolean here.
    /**
     * Whether overlapping contours with the same winding are supported.
     */
    public boolean isOverlapSupport;

    public GeneratorConfig() {
        this(true);
    }

    public GeneratorConfig(boolean overlapSupport) {
        this.isOverlapSupport = overlapSupport;
    }
}
