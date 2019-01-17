package jgeo;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class StretchLocation extends Location {

    private final List<LatLon> points = new ArrayList<LatLon>();

    private LatLon lowerLeft;
    private LatLon upperRight;

    public StretchLocation() {
    }

    public StretchLocation(String[] points) {
        for (String point : points) {
            addPoint(point);
        }
    }

    public StretchLocation(String points) {
        this(points.split(";"));
    }

    void addPoint(String point) {
        points.add(new LatLon(point));
    }

    void addPoint(double lat, double lon) {
        points.add(new LatLon(lat, lon));
    }

    @Override
    public double getDistanceInMeters(Location location) {
        // multi-dispatch: 'this' is already dispatched
        // so we need to dispatch on the argument only
        return location.getDistanceInMetersToStretch(this);
    }

    @Override
    public double getDistanceInMetersToPoint(PointLocation location) {
        LatLon point = location.latLon;
        if (points.size() == 0) {
            throw new InvalidParameterException();
        }

        // Run over all points and compute the distance to the line between 
        // two neighboring points.
        // If there is only one point we return the distance to this one.
        LatLon prev = points.get(0);
        double minDist = prev.getDistanceInMeters(point);
        for (int i = 1; i < points.size(); ++i) {
            LatLon next = points.get(i);
            double d = computeDistance(point, prev, next);
            if (d < minDist) {
                minDist = d;
            }
        }

        return minDist;
    }

    // compute the distance between a point and a line (p1-p2)
    private double computeDistance(LatLon point, LatLon p1, LatLon p2) {
        final double distp1p2 = p1.getDistanceInMeters(p2);
        final double distp1p = p1.getDistanceInMeters(point);
        if (distp1p2 == 0) {
            return p1.getDistanceInMeters(point);
        }

        // compute height
        final double distp2p = p2.getDistanceInMeters(point);
        final double height = computeHeightOnC(distp1p, distp2p, distp1p2);

        // Compute projection to check if it is between p1 and p2.
        //
        // ....p1_______p2
        //   | /\ |
        //   |/  \|
        //   a    b
        //
        // For a, the distance of the projection to p2 is bigger than the distance p1-p2.

        final double maxDist = Math.max(distp1p, distp2p);
        final double projection = Math.sqrt(maxDist * maxDist - height * height);
        final boolean isBetween = (projection < distp1p2);
        return (isBetween ? height : Math.min(distp1p, distp2p));
    }

    private double computeHeightOnC(double a, double b, double c) {
        // perimeter
        double p = (a + b + c) / 2;

        // area A
        double A = Math.sqrt(p * (p - a) * (p - b) * (p - c));

        // A = bh/2 -> h = 2A/b
        // b is the base which is c in our case
        return 2 * A / c;
    }

    // stretch to stretch is currently not implemented
    @Override
    public double getDistanceInMetersToStretch(StretchLocation location) {
        throw new UnsupportedOperationException("not implemented");
    }

    private void initBoundingBox() {
        LatLon prev = points.get(0);
        double minLat = prev.lat;
        double minLon = prev.lon;
        double maxLat = minLat;
        double maxLon = minLon;
        for (int i = 1; i < points.size(); ++i) {
            LatLon next = points.get(i);

            if (next.lat < minLat) {
                minLat = next.lat;
            } else if (next.lat > maxLat) {
                maxLat = next.lat;
            }

            if (next.lon < minLon) {
                minLon = next.lon;
            } else if (next.lon > maxLon) {
                maxLon = next.lon;
            }
        }

        this.lowerLeft = new LatLon(minLat, minLon);
        this.upperRight = new LatLon(maxLat, maxLon);
    }

    @Override
    public LatLon getLowerLeft() {
        if (this.lowerLeft == null) {
            initBoundingBox();
        }
        return this.lowerLeft;
    }

    @Override
    public LatLon getUpperRight() {
        if (this.upperRight == null) {
            initBoundingBox();
        }
        return this.upperRight;
    }

    @Override
    public LatLon getCenter() {
        if (this.lowerLeft == null) {
            initBoundingBox();
        }

        double lat = (lowerLeft.lat + upperRight.lat) / 2.0;
        double lon = (lowerLeft.lon + upperRight.lon) / 2.0;
        return new LatLon(lat, lon);
    }

    @Override
    public boolean contains(LatLon p) {
        // TODO: check if the point is on the line
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean contains(double lat, double lon) {
        // TODO: check if the point is on the line
        throw new UnsupportedOperationException("not implemented");
    }
}
