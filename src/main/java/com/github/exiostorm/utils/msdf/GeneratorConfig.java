package com.github.exiostorm.utils.msdf;

public class GeneratorConfig {

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
