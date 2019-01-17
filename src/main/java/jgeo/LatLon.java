package jgeo;

import org.json.JSONObject;

public class LatLon {
    public final double lat;
    public final double lon;

    public LatLon(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public LatLon(String strLatLon) {
        this(strLatLon.split(","));
    }

    public LatLon(String[] strLatLon) {
        this(strLatLon[0], strLatLon[1]);
    }

    public LatLon(String lat, String lon) {
        this(Double.parseDouble(lat), Double.parseDouble(lon));
    }

    @Override
    public String toString() {
        return toString(6);
    }

    public String toString(int precision) {
        return String.format("%1$." + precision + "f,%2$." + precision + "f", lat, lon);
    }

    public double getDistanceInMeters(LatLon other) {
        return getDistanceInMeters(other.lat, other.lon);
    }

    public double getDistanceInMeters(double lat, double lon) {

        double dLatDeg = lat - this.lat;
        double dLonDeg = lon - this.lon;

        double radiusMeters = 6371000;
        double dLat = Math.toRadians(dLatDeg);
        double dLng = Math.toRadians(dLonDeg);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(this.lat)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = radiusMeters * c;

        return dist;
    }

    public Object toStringOD() {
        return "" + (long) (lon * 100000) + " " + (long) (lat * 100000);
    }

    // returns a new LatLon moved by the given distance in meters
    public LatLon getMovedByMeter(double dLat, double dLon) {
        double distDegLat = getDistanceInMeters(new LatLon(this.lat + 1, this.lon));
        double distDegLon = getDistanceInMeters(new LatLon(this.lat, this.lon + 1));

        double newLat = this.lat + dLat / distDegLat;
        double newLon = this.lon + dLon / distDegLon;

        return new LatLon(newLat, newLon);
    }

    public boolean isInBounds(LatLon lowerLeft, LatLon upperRight) {
        return this.lat >= lowerLeft.lat
                && this.lat <= upperRight.lat
                && this.lon >= lowerLeft.lon
                && this.lon <= upperRight.lon;
    }

    public boolean isReachable(LatLon p, double distance) {
        return this.getDistanceInMeters(p) < distance;
    }

    public JSONObject toJson() {
        JSONObject res = new JSONObject();
        res.put("lat", lat);
        res.put("lon", lon);
        return res;
    }
}
