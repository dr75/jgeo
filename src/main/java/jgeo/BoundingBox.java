package jgeo;

import java.util.Collection;

public class BoundingBox implements BoundingShape {
    private final LatLon lowerLeft;
    private final LatLon upperRight;

    public BoundingBox(LatLon lowerLeft, LatLon upperRight) {
        this.lowerLeft = lowerLeft;
        this.upperRight = upperRight;
    }

    public BoundingBox(BoundingShape b1, BoundingShape b2) {
        this(computeLowerLeft(b1.getLowerLeft(), b2.getLowerLeft()),
                computeUpperRight(b1.getUpperRight(), b2.getUpperRight()));
    }

    public static BoundingBox aroundPoint(LatLon where, double maxDistance) {
        LatLon lowerLeft = where.getMovedByMeter(-maxDistance, -maxDistance);
        LatLon upperRight = where.getMovedByMeter(maxDistance, maxDistance);

        return new BoundingBox(lowerLeft, upperRight);
    }

    private static LatLon computeLowerLeft(LatLon lowerLeft1, LatLon lowerLeft2) {
        double latMin = Math.min(lowerLeft1.lat, lowerLeft2.lat);
        double lonMin = Math.min(lowerLeft1.lon, lowerLeft2.lon);
        return new LatLon(latMin, lonMin);
    }

    private static LatLon computeUpperRight(LatLon upperRight1, LatLon upperRight2) {
        double latMax = Math.max(upperRight1.lat, upperRight2.lat);
        double lonMax = Math.max(upperRight1.lon, upperRight2.lon);
        return new LatLon(latMax, lonMax);
    }

    public <E extends LocationObject> BoundingBox(Collection<E> data) {
        double latMin = Double.MAX_VALUE;
        double latMax = -Double.MAX_VALUE;
        double lonMin = Double.MAX_VALUE;
        double lonMax = -Double.MAX_VALUE;
        for (E e : data) {
            LatLon p = e.getLatLon();
            if (p.lat < latMin) {
                latMin = p.lat;
            }
            if (p.lat > latMax) {
                latMax = p.lat;
            }
            if (p.lon < lonMin) {
                lonMin = p.lon;
            }
            if (p.lon > lonMax) {
                lonMax = p.lon;
            }
        }

        this.lowerLeft = new LatLon(latMin, lonMin);
        this.upperRight = new LatLon(latMax, lonMax);
    }

    @Override
    public LatLon getLowerLeft() {
        return lowerLeft;
    }

    @Override
    public LatLon getUpperRight() {
        return upperRight;
    }

    @Override
    public LatLon getCenter() {
        double lat = (lowerLeft.lat + upperRight.lat) / 2.0;
        double lon = (lowerLeft.lon + upperRight.lon) / 2.0;
        return new LatLon(lat, lon);
    }

    public BoundingBox includePoint(LatLon p) {

        if (p.lat >= this.lowerLeft.lat
                && p.lon >= this.lowerLeft.lon
                && p.lat <= this.upperRight.lat
                && p.lon <= this.upperRight.lon) {
            return this;
        }

        double ll_lat = Math.min(p.lat, this.lowerLeft.lat);
        double ll_lon = Math.min(p.lon, this.lowerLeft.lon);
        double ur_lat = Math.max(p.lat, this.upperRight.lat);
        double ur_lon = Math.max(p.lon, this.upperRight.lon);
        return new BoundingBox(new LatLon(ll_lat, ll_lon), new LatLon(ur_lat, ur_lon));
    }

    public static BoundingBox aroundPoint(LatLon where, long maxDistance) {
        LatLon lowerLeft = where.getMovedByMeter(-maxDistance, -maxDistance);
        LatLon upperRight = where.getMovedByMeter(maxDistance, maxDistance);

        return new BoundingBox(lowerLeft, upperRight);
    }

    public static BoundingBox intersect(BoundingBox area1, BoundingBox area2) {
        LatLon area1LowerLeft = area1.getLowerLeft();
        LatLon area1UpperRight = area1.getUpperRight();

        LatLon area2LowerLeft = area2.getLowerLeft();
        LatLon area2UpperRight = area2.getUpperRight();

        // Use the intersection only
        double latLo = Math.max(area2LowerLeft.lat, area1LowerLeft.lat);
        double lonLo = Math.max(area2LowerLeft.lon, area1LowerLeft.lon);

        double latHi = Math.min(area2UpperRight.lat, area1UpperRight.lat);
        double lonHi = Math.min(area2UpperRight.lon, area1UpperRight.lon);

        LatLon lowerLeft = new LatLon(latLo, lonLo);
        LatLon upperRight = new LatLon(latHi, lonHi);
        return new BoundingBox(lowerLeft, upperRight);
    }

    public BoundingBox growBy(double range) {
        LatLon ll = this.lowerLeft.getMovedByMeter(-range, -range);
        LatLon ur = this.upperRight.getMovedByMeter(range, range);
        return new BoundingBox(ll, ur);
    }

    @Override
    public boolean contains(LatLon p) {
        return contains(p.lat, p.lon);
    }

    @Override
    public boolean contains(double lat, double lon) {
        return this.lowerLeft.lat <= lat
                && this.lowerLeft.lon <= lon
                && this.upperRight.lat >= lat
                && this.upperRight.lon >= lon;
    }

    @Override
    public String toString() {
        return this.lowerLeft.toString() + ":" + this.upperRight.toString();
    }
}
