package com.github.exiostorm.utils.msdf;

public class MSDFGeneratorConfig extends GeneratorConfig {

    public ErrorCorrectionConfig errorCorrection;

    public MSDFGeneratorConfig() {
        super(true);
        this.errorCorrection = new ErrorCorrectionConfig();
    }

    public MSDFGeneratorConfig(boolean overlapSupport, ErrorCorrectionConfig errorCorrection) {
        super(overlapSupport);
        this.errorCorrection = errorCorrection != null ? errorCorrection : new ErrorCorrectionConfig();
    }
}