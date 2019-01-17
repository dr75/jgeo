package jgeo;

public class BoundingCircle implements BoundingShape {
    private final LatLon center;
    private final double radiusMeter;
    private final BoundingBox bb;
    private final BoundingBox innerBox;

    public BoundingCircle(LatLon center, double radiusMeter) {
        this.center = center;
        this.radiusMeter = radiusMeter;
        this.bb = BoundingBox.aroundPoint(center, radiusMeter);

        //double innerBoxRadius = Math.sin(45.0 / 180 * Math.PI) * radiusMeter;
        double innerBoxRadius = radiusMeter / Math.sqrt(2);
        this.innerBox = BoundingBox.aroundPoint(center, innerBoxRadius);
    }

    @Override
    public LatLon getLowerLeft() {
        return this.bb.getLowerLeft();
    }

    @Override
    public LatLon getUpperRight() {
        return this.bb.getUpperRight();
    }

    @Override
    public LatLon getCenter() {
        return this.center;
    }

    @Override
    public boolean contains(LatLon p) {
        if (this.innerBox.contains(p)) {
            return true;
        }

        return this.bb.contains(p) && center.getDistanceInMeters(p) <= this.radiusMeter;
    }

    @Override
    public boolean contains(double lat, double lon) {
        if (this.innerBox.contains(lat, lon)) {
            return true;
        }

        return this.bb.contains(lat, lon) && center.getDistanceInMeters(lat, lon) <= this.radiusMeter;
    }
}
