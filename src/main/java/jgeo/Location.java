package jgeo;

public abstract class Location implements BoundingShape, LocationObject {
    @Override
    public LatLon getLatLon() {
        return getCenter();
    }

    public abstract double getDistanceInMeters(Location location);

    public abstract double getDistanceInMetersToPoint(PointLocation location);

    public abstract double getDistanceInMetersToStretch(StretchLocation location);

    public static String formatDistanceKm(double dist) {
        return String.format("%1$.2f", dist / 1000.0);
    }

    public String getDistanceKmAsString(Location location) {
        return formatDistanceKm(getDistanceInMeters(location));
    }

    public String getDistanceMeterAsString(Location location) {
        return String.format("%1$.2f", getDistanceInMeters(location));
    }
}
