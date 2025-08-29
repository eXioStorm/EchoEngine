package com.github.exiostorm.utils.msdf;

import org.joml.Vector2d;

import java.awt.geom.Rectangle2D;

import static com.github.exiostorm.utils.msdf.EquationSolver.*;

//TODO hoorah! another class done!
public abstract class EdgeSegment {
    public EdgeSegment(EdgeColor edgeColor) {
        this.edgeColor = edgeColor;
    }
    public static EdgeSegment create(Vector2d p0, Vector2d p1, EdgeColor edgeColor) {
        return new LinearSegment(p0, p1, edgeColor);
    }
    public static EdgeSegment create(Vector2d p0, Vector2d p1, Vector2d p2, EdgeColor edgeColor) {

        Vector2d v1 = new Vector2d(p1).sub(p0);
        Vector2d v2 = new Vector2d(p2).sub(p1);

        if (v1.dot(v2) == 0.0f) {
            return new LinearSegment(p0, p2, edgeColor);
        }

        return new QuadraticSegment(p0, p1, p2, edgeColor);
    }
    public static EdgeSegment create(Vector2d p0, Vector2d p1, Vector2d p2, Vector2d p3, EdgeColor edgeColor) {

        Vector2d p12 = new Vector2d(p2).sub(p1);
        if (new Vector2d(p1).sub(p0).dot(p12) == 0.0f && p12.dot(new Vector2d(p3).sub(p2)) == 0.0f) {
            return new LinearSegment(p0, p3, edgeColor);
        }
        Vector2d newP12 = new Vector2d(p1).mul(1.5f).sub(new Vector2d(p0).mul(0.5f));

        Vector2d rightSide = new Vector2d(p2).mul(1.5f).sub(new Vector2d(p3).mul(0.5f));

        if (newP12.equals(rightSide)) {
            return new QuadraticSegment(p0, newP12, p3, edgeColor);
        }
        return new CubicSegment(p0, p1, p2, p3, edgeColor);
    }
    public void distanceToPerpendicularDistance(SignedDistance distance, Vector2d origin, double param) {
        if (param < 0) {
            Vector2d dir = direction(0).normalize();
            Vector2d aq = new Vector2d(origin).sub(point(0));
            double ts = aq.dot(dir);
            if (ts < 0) {
                double perpendicularDistance = crossProduct(aq, dir);
                if (Math.abs(perpendicularDistance) <= Math.abs(distance.distance)) {
                    distance.distance = perpendicularDistance;
                    distance.dot = 0;
                }
            }
        } else if (param > 1) {
            Vector2d dir = direction(1).normalize();
            Vector2d bq = new Vector2d(origin).sub(point(1));
            double ts = bq.dot(dir);
            if (ts > 0) {
                double perpendicularDistance = crossProduct(bq, dir);
                if (Math.abs(perpendicularDistance) <= Math.abs(distance.distance)) {
                    distance.distance = perpendicularDistance;
                    distance.dot = 0;
                }
            }
        }
    }
    static void pointBounds(Vector2d p, Rectangle2D bounds) {
        double l = bounds.getMinX();
        double b = bounds.getMinY();
        double r = bounds.getMaxX();
        double t = bounds.getMaxY();

        if (p.x < l) l = p.x;
        if (p.y < b) b = p.y;
        if (p.x > r) r = p.x;
        if (p.y > t) t = p.y;

        bounds.setRect(l, b, r - l, t - b);
    }
    public static double crossProduct(Vector2d a, Vector2d b) {
        return a.x * b.y - a.y * b.x;
    }
    public static int nonZeroSign(double n) {
        return (Math.signum(n) >= 0) ? 1 : -1;
    }
    public static int sign(double n) {
        return (n > 0) ? 1 : (n < 0 ? -1 : 0);
    }
    private Vector2d[] p = new Vector2d[2];
    public abstract EdgeSegment clone();
    public EdgeColor edgeColor;
    public abstract int type();
    public int EDGE_TYPE;
    public abstract Vector2d[] controlPoints();
    public abstract Vector2d point(double param);
    public abstract Vector2d direction(double param);
    public abstract Vector2d directionChange(double param);
    public abstract double length();
    public abstract SignedDistance signedDistance(Vector2d origin, double[] param);
    public abstract int scanlineIntersections(double[] x, int[] dy, double y);
    public int MSDFGEN_CUBIC_SEARCH_STARTS = 4;
    public int MSDFGEN_CUBIC_SEARCH_STEPS = 4;
    public abstract void bound(Rectangle2D bounds);
    public abstract void reverse();
    public abstract void moveStartPoint(Vector2d to);
    public abstract void moveEndPoint(Vector2d to);
    public abstract void splitInThirds(EdgeSegment[] parts);
}
class LinearSegment extends EdgeSegment {
    private Vector2d[] p = new Vector2d[2];

    public LinearSegment(Vector2d p0, Vector2d p1, EdgeColor edgeColor) {
        super(edgeColor);
        EDGE_TYPE = 1;
        p[0] = new Vector2d(p0);
        p[1] = new Vector2d(p1);
    }

    @Override
    public LinearSegment clone() {
        return new LinearSegment(p[0], p[1], edgeColor);
    }

    @Override
    public int type() {
        return EDGE_TYPE;
    }

    @Override
    public Vector2d[] controlPoints() {
        return p.clone();
    }

    @Override
    public Vector2d point(double param) {
        return new Vector2d(p[0]).lerp(p[1], param);
    }

    @Override
    public Vector2d direction(double param) {
        return new Vector2d(p[1]).sub(p[0]);
    }

    @Override
    public Vector2d directionChange(double param) {
        return new Vector2d();
    }

    @Override
    public double length() {
        return new Vector2d(p[1]).sub(p[0]).length();
    }

    @Override
    public SignedDistance signedDistance(Vector2d origin, double[] param) {
        Vector2d aq = new Vector2d(origin).sub(p[0]);
        Vector2d ab = new Vector2d(p[1]).sub(p[0]);
        param[0] = aq.dot(ab) / ab.dot(ab);
        Vector2d eq = new Vector2d(p[param[0] > 0.5 ? 1 : 0]).sub(origin);
        double endpointDistance = eq.length();
        if (param[0] > 0 && param[0] < 1) {
            Vector2d abNorm = new Vector2d(ab).normalize();
            Vector2d ortho = new Vector2d(-abNorm.y, abNorm.x); // Perpendicular vector
            double orthoDistance = ortho.dot(aq);
            if (Math.abs(orthoDistance) < endpointDistance)
                return new SignedDistance(orthoDistance, 0);
        }
        return new SignedDistance(nonZeroSign(crossProduct(aq, ab)) * endpointDistance,
                Math.abs(new Vector2d(ab).normalize().dot(new Vector2d(eq).normalize())));
    }
    @Override
    public int scanlineIntersections(double[] x, int[] dy, double y) {
        if ((y >= p[0].y && y < p[1].y) || (y >= p[1].y && y < p[0].y)) {
            double param = (y - p[0].y) / (p[1].y - p[0].y);
            x[0] = p[0].x * (1.0 - param) + p[1].x * param; // manual lerp for scalar
            dy[0] = sign(p[1].y - p[0].y);
            return 1;
        }
        return 0;
    }

    @Override
    public void bound(Rectangle2D bounds) {
        pointBounds(p[0], bounds);
        pointBounds(p[1], bounds);
    }

    @Override
    public void reverse() {
        Vector2d tmp = p[0];
        p[0] = p[1];
        p[1] = tmp;
    }

    @Override
    public void moveStartPoint(Vector2d to) {
        p[0] = new Vector2d(to);
    }

    @Override
    public void moveEndPoint(Vector2d to) {
        p[1] = new Vector2d(to);
    }

    @Override
    public void splitInThirds(EdgeSegment[] parts) {
        parts[0] = new LinearSegment(p[0], point(1.0/3.0), edgeColor);
        parts[1] = new LinearSegment(point(1.0/3.0), point(2.0/3.0), edgeColor);
        parts[2] = new LinearSegment(point(2.0/3.0), p[1], edgeColor);
    }
}

class QuadraticSegment extends EdgeSegment {
    private Vector2d[] p = new Vector2d[3];

    public QuadraticSegment(Vector2d p0, Vector2d p1, Vector2d p2, EdgeColor edgeColor) {
        super(edgeColor);
        EDGE_TYPE = 2;
        p[0] = new Vector2d(p0);
        p[1] = new Vector2d(p1);
        p[2] = new Vector2d(p2);
    }

    @Override
    public QuadraticSegment clone() {
        return new QuadraticSegment(p[0], p[1], p[2], edgeColor);
    }

    @Override
    public int type() {
        return EDGE_TYPE;
    }

    @Override
    public Vector2d[] controlPoints() {
        return p.clone();
    }

    @Override
    public Vector2d point(double param) {
        Vector2d temp1 = new Vector2d(p[0]).lerp(p[1], param);
        Vector2d temp2 = new Vector2d(p[1]).lerp(p[2], param);
        return temp1.lerp(temp2, param);
    }

    @Override
    public Vector2d direction(double param) {
        Vector2d v1 = new Vector2d(p[1]).sub(p[0]);
        Vector2d v2 = new Vector2d(p[2]).sub(p[1]);
        Vector2d tangent = new Vector2d(v1).lerp(v2, param);
        if (tangent.lengthSquared() == 0)
            return new Vector2d(p[2]).sub(p[0]);
        return tangent;
    }

    @Override
    public Vector2d directionChange(double param) {
        Vector2d v1 = new Vector2d(p[2]).sub(p[1]);
        Vector2d v2 = new Vector2d(p[1]).sub(p[0]);
        return v1.sub(v2);
    }

    @Override
    public double length() {
        Vector2d ab = new Vector2d(p[1]).sub(p[0]);
        Vector2d br = new Vector2d(p[2]).sub(p[1]).sub(ab);
        double abab = ab.dot(ab);
        double abbr = ab.dot(br);
        double brbr = br.dot(br);
        double abLen = Math.sqrt(abab);
        double brLen = Math.sqrt(brbr);
        double crs = crossProduct(ab, br);
        double h = Math.sqrt(abab + abbr + abbr + brbr);
        return (brLen * ((abbr + brbr) * h - abbr * abLen) +
                crs * crs * Math.log((brLen * h + abbr + brbr) / (brLen * abLen + abbr))) /
                (brbr * brLen);
    }

    @Override
    public SignedDistance signedDistance(Vector2d origin, double[] param) {
        Vector2d qa = new Vector2d(p[0]).sub(origin);
        Vector2d ab = new Vector2d(p[1]).sub(p[0]);
        Vector2d br = new Vector2d(p[2]).sub(p[1]).sub(ab);
        double a = br.dot(br);
        double b = 3 * ab.dot(br);
        double c = 2 * ab.dot(ab) + qa.dot(br);
        double d = qa.dot(ab);
        double[] t = new double[3];
        int solutions = solveCubic(t, a, b, c, d);

        Vector2d epDir = direction(0);
        double minDistance = nonZeroSign(crossProduct(epDir, qa)) * qa.length();
        param[0] = -qa.dot(epDir) / epDir.dot(epDir);
        {
            double distance = new Vector2d(p[2]).sub(origin).length();
            if (distance < Math.abs(minDistance)) {
                epDir = direction(1);
                minDistance = nonZeroSign(crossProduct(epDir, new Vector2d(p[2]).sub(origin))) * distance;
                param[0] = new Vector2d(origin).sub(p[1]).dot(epDir) / epDir.dot(epDir);
            }
        }
        for (int i = 0; i < solutions; ++i) {
            if (t[i] > 0 && t[i] < 1) {
                Vector2d qe = new Vector2d(qa).add(new Vector2d(ab).mul(2 * t[i])).add(new Vector2d(br).mul(t[i] * t[i]));
                double distance = qe.length();
                if (distance <= Math.abs(minDistance)) {
                    Vector2d tangent = new Vector2d(ab).add(new Vector2d(br).mul(t[i]));
                    minDistance = nonZeroSign(crossProduct(tangent, qe)) * distance;
                    param[0] = t[i];
                }
            }
        }

        if (param[0] >= 0 && param[0] <= 1)
            return new SignedDistance(minDistance, 0);
        if (param[0] < 0.5)
            return new SignedDistance(minDistance, Math.abs(direction(0).normalize().dot(qa.normalize())));
        else
            return new SignedDistance(minDistance, Math.abs(direction(1).normalize().dot(new Vector2d(p[2]).sub(origin).normalize())));
    }

    @Override
    public int scanlineIntersections(double[] x, int[] dy, double y) {
        int total = 0;
        int nextDY = y > p[0].y ? 1 : -1;
        x[total] = p[0].x;
        if (p[0].y == y) {
            if (p[0].y < p[1].y || (p[0].y == p[1].y && p[0].y < p[2].y))
                dy[total++] = 1;
            else
                nextDY = 1;
        }
        {
            Vector2d ab = new Vector2d(p[1]).sub(p[0]);
            Vector2d br = new Vector2d(p[2]).sub(p[1]).sub(ab);
            double[] t = new double[2];
            int solutions = solveQuadratic(t, br.y, 2 * ab.y, p[0].y - y);
            // Sort solutions
            if (solutions >= 2 && t[0] > t[1]) {
                double tmp = t[0]; t[0] = t[1]; t[1] = tmp;
            }
            for (int i = 0; i < solutions && total < 2; ++i) {
                if (t[i] >= 0 && t[i] <= 1) {
                    x[total] = p[0].x + 2 * t[i] * ab.x + t[i] * t[i] * br.x;
                    if (nextDY * (ab.y + t[i] * br.y) >= 0) {
                        dy[total++] = nextDY;
                        nextDY = -nextDY;
                    }
                }
            }
        }
        if (p[2].y == y) {
            if (nextDY > 0 && total > 0) {
                --total;
                nextDY = -1;
            }
            if ((p[2].y < p[1].y || (p[2].y == p[1].y && p[2].y < p[0].y)) && total < 2) {
                x[total] = p[2].x;
                if (nextDY < 0) {
                    dy[total++] = -1;
                    nextDY = 1;
                }
            }
        }
        if (nextDY != (y >= p[2].y ? 1 : -1)) {
            if (total > 0)
                --total;
            else {
                if (Math.abs(p[2].y - y) < Math.abs(p[0].y - y))
                    x[total] = p[2].x;
                dy[total++] = nextDY;
            }
        }
        return total;
    }

    @Override
    public void bound(Rectangle2D bounds) {
        pointBounds(p[0], bounds);
        pointBounds(p[2], bounds);
        Vector2d bot = new Vector2d(p[1]).sub(p[0]).sub(new Vector2d(p[2]).sub(p[1]));
        if (bot.x != 0) {
            double param = (p[1].x - p[0].x) / bot.x;
            if (param > 0 && param < 1)
                pointBounds(point(param), bounds);
        }
        if (bot.y != 0) {
            double param = (p[1].y - p[0].y) / bot.y;
            if (param > 0 && param < 1)
                pointBounds(point(param), bounds);
        }
    }

    @Override
    public void reverse() {
        Vector2d tmp = p[0];
        p[0] = p[2];
        p[2] = tmp;
    }

    @Override
    public void moveStartPoint(Vector2d to) {
        Vector2d origSDir = new Vector2d(p[0]).sub(p[1]);
        Vector2d origP1 = new Vector2d(p[1]);
        Vector2d v1 = new Vector2d(p[0]).sub(p[1]);
        Vector2d v2 = new Vector2d(to).sub(p[0]);
        Vector2d v3 = new Vector2d(p[2]).sub(p[1]);
        p[1].add(new Vector2d(v3).mul(crossProduct(v1, v2) / crossProduct(v1, v3)));
        p[0] = new Vector2d(to);
        if (origSDir.dot(new Vector2d(p[0]).sub(p[1])) < 0)
            p[1] = origP1;
    }

    @Override
    public void moveEndPoint(Vector2d to) {
        Vector2d origEDir = new Vector2d(p[2]).sub(p[1]);
        Vector2d origP1 = new Vector2d(p[1]);
        Vector2d v1 = new Vector2d(p[2]).sub(p[1]);
        Vector2d v2 = new Vector2d(to).sub(p[2]);
        Vector2d v3 = new Vector2d(p[0]).sub(p[1]);
        p[1].add(new Vector2d(v3).mul(crossProduct(v1, v2) / crossProduct(v1, v3)));
        p[2] = new Vector2d(to);
        if (origEDir.dot(new Vector2d(p[2]).sub(p[1])) < 0)
            p[1] = origP1;
    }

    @Override
    public void splitInThirds(EdgeSegment[] parts) {
        Vector2d control1 = new Vector2d(p[0]).lerp(p[1], 1.0/3.0);
        Vector2d midControl = new Vector2d(p[0]).lerp(p[1], 5.0/9.0).lerp(
                new Vector2d(p[1]).lerp(p[2], 4.0/9.0), 0.5);
        Vector2d control2 = new Vector2d(p[1]).lerp(p[2], 2.0/3.0);

        parts[0] = new QuadraticSegment(p[0], control1, point(1.0/3.0), edgeColor);
        parts[1] = new QuadraticSegment(point(1.0/3.0), midControl, point(2.0/3.0), edgeColor);
        parts[2] = new QuadraticSegment(point(2.0/3.0), control2, p[2], edgeColor);
    }

    public EdgeSegment convertToCubic() {
        Vector2d control1 = new Vector2d(p[0]).lerp(p[1], 2.0/3.0);
        Vector2d control2 = new Vector2d(p[1]).lerp(p[2], 1.0/3.0);
        return new CubicSegment(p[0], control1, control2, p[2], edgeColor);
    }
}

class CubicSegment extends EdgeSegment {
    private Vector2d[] p = new Vector2d[4];

    public CubicSegment(Vector2d p0, Vector2d p1, Vector2d p2, Vector2d p3, EdgeColor edgeColor) {
        super(edgeColor);
        EDGE_TYPE = 3;
        p[0] = new Vector2d(p0);
        p[1] = new Vector2d(p1);
        p[2] = new Vector2d(p2);
        p[3] = new Vector2d(p3);
    }

    @Override
    public CubicSegment clone() {
        return new CubicSegment(p[0], p[1], p[2], p[3], edgeColor);
    }

    @Override
    public int type() {
        return EDGE_TYPE;
    }

    @Override
    public Vector2d[] controlPoints() {
        return p.clone();
    }

    @Override
    public Vector2d point(double param) {
        Vector2d p12 = new Vector2d(p[1]).lerp(p[2], param);
        Vector2d temp1 = new Vector2d(p[0]).lerp(p[1], param).lerp(p12, param);
        Vector2d temp2 = p12.lerp(new Vector2d(p[2]).lerp(p[3], param), param);
        return temp1.lerp(temp2, param);
    }

    @Override
    public Vector2d direction(double param) {
        Vector2d v1 = new Vector2d(p[1]).sub(p[0]);
        Vector2d v2 = new Vector2d(p[2]).sub(p[1]);
        Vector2d v3 = new Vector2d(p[3]).sub(p[2]);
        Vector2d tangent = new Vector2d(v1).lerp(v2, param).lerp(
                new Vector2d(v2).lerp(v3, param), param);
        if (tangent.lengthSquared() == 0) {
            if (param == 0) return new Vector2d(p[2]).sub(p[0]);
            if (param == 1) return new Vector2d(p[3]).sub(p[1]);
        }
        return tangent;
    }

    @Override
    public Vector2d directionChange(double param) {
        Vector2d v1 = new Vector2d(p[2]).sub(p[1]).sub(new Vector2d(p[1]).sub(p[0]));
        Vector2d v2 = new Vector2d(p[3]).sub(p[2]).sub(new Vector2d(p[2]).sub(p[1]));
        return new Vector2d(v1).lerp(v2, param);
    }

    //C++ version never has CubicSegment.length()
    @Override
    public double length() {
        return 0;
    }

    @Override
    public SignedDistance signedDistance(Vector2d origin, double[] param) {
        Vector2d qa = new Vector2d(p[0]).sub(origin);
        Vector2d ab = new Vector2d(p[1]).sub(p[0]);
        Vector2d br = new Vector2d(p[2]).sub(p[1]).sub(ab);
        Vector2d as = new Vector2d(p[3]).sub(p[2]).sub(new Vector2d(p[2]).sub(p[1])).sub(br);

        Vector2d epDir = direction(0);
        double minDistance = nonZeroSign(crossProduct(epDir, qa)) * qa.length();
        param[0] = -qa.dot(epDir) / epDir.dot(epDir);
        {
            double distance = new Vector2d(p[3]).sub(origin).length();
            if (distance < Math.abs(minDistance)) {
                epDir = direction(1);
                Vector2d temp = new Vector2d(p[3]).sub(origin);
                minDistance = nonZeroSign(crossProduct(epDir, temp)) * distance;
                param[0] = new Vector2d(epDir).sub(temp).dot(epDir) / epDir.dot(epDir);
            }
        }
        // Iterative minimum distance search
        for (int i = 0; i <= MSDFGEN_CUBIC_SEARCH_STARTS; ++i) {
            double t = (double)i / MSDFGEN_CUBIC_SEARCH_STARTS;
            Vector2d qe = new Vector2d(qa).add(new Vector2d(ab).mul(3 * t)).add(new Vector2d(br).mul(3 * t * t)).add(new Vector2d(as).mul(t * t * t));
            Vector2d d1 = new Vector2d(ab).mul(3).add(new Vector2d(br).mul(6 * t)).add(new Vector2d(as).mul(3 * t * t));
            Vector2d d2 = new Vector2d(br).mul(6).add(new Vector2d(as).mul(6 * t));
            double improvedT = t - qe.dot(d1) / (d1.dot(d1) + qe.dot(d2));
            if (improvedT > 0 && improvedT < 1) {
                int remainingSteps = MSDFGEN_CUBIC_SEARCH_STEPS;
                do {
                    t = improvedT;
                    qe = new Vector2d(qa).add(new Vector2d(ab).mul(3 * t)).add(new Vector2d(br).mul(3 * t * t)).add(new Vector2d(as).mul(t * t * t));
                    d1 = new Vector2d(ab).mul(3).add(new Vector2d(br).mul(6 * t)).add(new Vector2d(as).mul(3 * t * t));
                    if (--remainingSteps == 0)
                        break;
                    d2 = new Vector2d(br).mul(6).add(new Vector2d(as).mul(6 * t));
                    improvedT = t - qe.dot(d1) / (d1.dot(d1) + qe.dot(d2));
                } while (improvedT > 0 && improvedT < 1);
                double distance = qe.length();
                if (distance < Math.abs(minDistance)) {
                    minDistance = nonZeroSign(crossProduct(d1, qe)) * distance;
                    param[0] = t;
                }
            }
        }

        if (param[0] >= 0 && param[0] <= 1)
            return new SignedDistance(minDistance, 0);
        if (param[0] < 0.5)
            return new SignedDistance(minDistance, Math.abs(direction(0).normalize().dot(qa.normalize())));
        else
            return new SignedDistance(minDistance, Math.abs(direction(1).normalize().dot(new Vector2d(p[3]).sub(origin).normalize())));
    }

    @Override
    public int scanlineIntersections(double[] x, int[] dy, double y) {
        int total = 0;
        int nextDY = y > p[0].y ? 1 : -1;
        x[total] = p[0].x;
        if (p[0].y == y) {
            if (p[0].y < p[1].y || (p[0].y == p[1].y && (p[0].y < p[2].y || (p[0].y == p[2].y && p[0].y < p[3].y))))
                dy[total++] = 1;
            else
                nextDY = 1;
        }
        {
            Vector2d ab = new Vector2d(p[1]).sub(p[0]);
            Vector2d br = new Vector2d(p[2]).sub(p[1]).sub(ab);
            Vector2d as = new Vector2d(p[3]).sub(p[2]).sub(new Vector2d(p[2]).sub(p[1])).sub(br);
            double[] t = new double[3];
            int solutions = solveCubic(t, as.y, 3 * br.y, 3 * ab.y, p[0].y - y);
            // Sort solutions
            if (solutions >= 2) {
                if (t[0] > t[1]) {
                    double tmp = t[0]; t[0] = t[1]; t[1] = tmp;
                }
                if (solutions >= 3 && t[1] > t[2]) {
                    double tmp = t[1]; t[1] = t[2]; t[2] = tmp;
                    if (t[0] > t[1]) {
                        tmp = t[0]; t[0] = t[1]; t[1] = tmp;
                    }
                }
            }
            for (int i = 0; i < solutions && total < 3; ++i) {
                if (t[i] >= 0 && t[i] <= 1) {
                    x[total] = p[0].x + 3 * t[i] * ab.x + 3 * t[i] * t[i] * br.x + t[i] * t[i] * t[i] * as.x;
                    if (nextDY * (ab.y + 2 * t[i] * br.y + t[i] * t[i] * as.y) >= 0) {
                        dy[total++] = nextDY;
                        nextDY = -nextDY;
                    }
                }
            }
        }
        if (p[3].y == y) {
            if (nextDY > 0 && total > 0) {
                --total;
                nextDY = -1;
            }
            if ((p[3].y < p[2].y || (p[3].y == p[2].y && (p[3].y < p[1].y || (p[3].y == p[1].y && p[3].y < p[0].y)))) && total < 3) {
                x[total] = p[3].x;
                if (nextDY < 0) {
                    dy[total++] = -1;
                    nextDY = 1;
                }
            }
        }
        if (nextDY != (y >= p[3].y ? 1 : -1)) {
            if (total > 0)
                --total;
            else {
                if (Math.abs(p[3].y - y) < Math.abs(p[0].y - y))
                    x[total] = p[3].x;
                dy[total++] = nextDY;
            }
        }
        return total;
    }

    @Override
    public void bound(Rectangle2D bounds) {
        pointBounds(p[0], bounds);
        pointBounds(p[3], bounds);
        Vector2d a0 = new Vector2d(p[1]).sub(p[0]);
        Vector2d a1 = new Vector2d(p[2]).sub(p[1]).sub(a0).mul(2);
        Vector2d a2 = new Vector2d(p[3]).sub(new Vector2d(p[2]).mul(3)).add(new Vector2d(p[1]).mul(3)).sub(p[0]);
        double[] params = new double[2];
        int solutions;
        solutions = solveQuadratic(params, a2.x, a1.x, a0.x);
        for (int i = 0; i < solutions; ++i)
            if (params[i] > 0 && params[i] < 1)
                pointBounds(point(params[i]), bounds);
        solutions = solveQuadratic(params, a2.y, a1.y, a0.y);
        for (int i = 0; i < solutions; ++i)
            if (params[i] > 0 && params[i] < 1)
                pointBounds(point(params[i]), bounds);
    }

    @Override
    public void reverse() {
        Vector2d tmp = p[0];
        p[0] = p[3];
        p[3] = tmp;
        tmp = p[1];
        p[1] = p[2];
        p[2] = tmp;
    }

    @Override
    public void moveStartPoint(Vector2d to) {
        Vector2d delta = new Vector2d(to).sub(p[0]);
        p[1].add(delta);
        p[0] = new Vector2d(to);
    }

    @Override
    public void moveEndPoint(Vector2d to) {
        Vector2d delta = new Vector2d(to).sub(p[3]);
        p[2].add(delta);
        p[3] = new Vector2d(to);
    }

    @Override
    public void splitInThirds(EdgeSegment[] parts) {
        // De Casteljau's algorithm for cubic curve subdivision
        Vector2d p01 = new Vector2d(p[0]).lerp(p[1], 1.0/3.0);
        Vector2d p12 = new Vector2d(p[1]).lerp(p[2], 1.0/3.0);
        Vector2d p23 = new Vector2d(p[2]).lerp(p[3], 1.0/3.0);
        Vector2d p012 = new Vector2d(p01).lerp(p12, 1.0/3.0);
        Vector2d p123 = new Vector2d(p12).lerp(p23, 1.0/3.0);
        Vector2d p0123 = new Vector2d(p012).lerp(p123, 1.0/3.0);

        parts[0] = new CubicSegment(p[0], p01, p012, p0123, edgeColor);

        // For the middle segment, we need different control points
        Vector2d p01_2 = new Vector2d(p[0]).lerp(p[1], 2.0/3.0);
        Vector2d p12_2 = new Vector2d(p[1]).lerp(p[2], 2.0/3.0);
        Vector2d p23_2 = new Vector2d(p[2]).lerp(p[3], 2.0/3.0);
        Vector2d p012_2 = new Vector2d(p01_2).lerp(p12_2, 2.0/3.0);
        Vector2d p123_2 = new Vector2d(p12_2).lerp(p23_2, 2.0/3.0);
        Vector2d p0123_2 = new Vector2d(p012_2).lerp(p123_2, 2.0/3.0);

        Vector2d mid1 = new Vector2d(p123).lerp(p012_2, 0.5);
        Vector2d mid2 = new Vector2d(p0123_2).lerp(p123, 0.5);

        parts[1] = new CubicSegment(p0123, mid1, mid2, p0123_2, edgeColor);

        parts[2] = new CubicSegment(p0123_2, p123_2, p23_2, p[3], edgeColor);
    }
}
