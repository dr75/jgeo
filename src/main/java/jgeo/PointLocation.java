package jgeo;

public class PointLocation extends Location {

    public final LatLon latLon;

    public PointLocation(double lat, double lon) {
        this.latLon = new LatLon(lat, lon);
    }

    public PointLocation(String strLatLon) {
        this.latLon = new LatLon(strLatLon);
    }

    public PointLocation(String lat, String lon) {
        this.latLon = new LatLon(lat, lon);
    }

    @Override
    public String toString() {
        return latLon.toString();
    }

    public Object toStringOD() {
        return latLon.toStringOD();
    }

    public double getDistanceInMeters(Addressable addressable) {
        return getDistanceInMeters(addressable.getLocation());
    }

    @Override
    public double getDistanceInMeters(Location location) {
        // multi-dispatch: 'this' is already dispatched
        // so we need to dispatch on the argument only
        return location.getDistanceInMetersToPoint(this);
    }

    public double getLat() {
        return latLon.lat;
    }

    public double getLon() {
        return latLon.lon;
    }

    @Override
    public double getDistanceInMetersToPoint(PointLocation location) {
        return latLon.getDistanceInMeters(location.latLon);
    }

    @Override
    public double getDistanceInMetersToStretch(StretchLocation stretch) {
        return stretch.getDistanceInMetersToPoint(this);
    }

    @Override
    public LatLon getLowerLeft() {
        return latLon;
    }

    @Override
    public LatLon getUpperRight() {
        return latLon;
    }

    @Override
    public LatLon getCenter() {
        return latLon;
    }

    @Override
    public boolean contains(LatLon p) {
        return contains(p.lat, p.lon);
    }

    @Override
    public boolean contains(double lat, double lon) {
        return lat == latLon.lat && lon == latLon.lon;
    }
}
