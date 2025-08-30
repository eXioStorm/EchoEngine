package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

/**
 * Full signed distance field transformation specifies both spatial transformation (Projection)
 * as well as distance value transformation (DistanceMapping).
 */
public class SDFTransformation extends Projection {

    public DistanceMapping distanceMapping;

    public SDFTransformation() {
        super();
        this.distanceMapping = new DistanceMapping();
    }

    public SDFTransformation(Projection projection, DistanceMapping distanceMapping) {
        super(projection.getScale(), projection.getTranslate());
        this.distanceMapping = distanceMapping;
    }

    public SDFTransformation(Vector2d scale, Vector2d translate, DistanceMapping distanceMapping) {
        super(scale, translate);
        this.distanceMapping = distanceMapping;
    }

    // Getter and setter for distanceMapping
    public DistanceMapping getDistanceMapping() {
        return distanceMapping;
    }

    public void setDistanceMapping(DistanceMapping distanceMapping) {
        this.distanceMapping = distanceMapping;
    }
}
