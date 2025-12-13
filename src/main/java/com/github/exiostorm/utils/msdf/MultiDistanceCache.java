package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

public class MultiDistanceCache {
    public Vector2d point = new Vector2d();
    public double absDistance;
    public double aPerpendicularDistance;
    public double bPerpendicularDistance;
    public double aDomainDistance;
    public double bDomainDistance;
    public SignedDistance[] channelDistances = new SignedDistance[3]; // R, G, B

    public MultiDistanceCache() {
        absDistance = 0;
        aPerpendicularDistance = 0;
        bPerpendicularDistance = 0;
        aDomainDistance = 0;
        bDomainDistance = 0;
        for (int i = 0; i < 3; i++) channelDistances[i] = new SignedDistance();
    }
}
