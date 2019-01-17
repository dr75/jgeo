package jgeo;

public interface BoundingShape {
    public LatLon getLowerLeft();

    public LatLon getUpperRight();

    public LatLon getCenter();

    public boolean contains(LatLon p);

    public boolean contains(double lat, double lon);
}
