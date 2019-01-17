package jgeo;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface LocationObject {
    LatLon getLatLon();

    public static <T extends LocationObject> void sortByLat(List<T> data) {
        Collections.sort(data, new Comparator<LocationObject>() {
            @Override
            public int compare(LocationObject o1, LocationObject o2) {
                return Double.compare(o1.getLatLon().lat, o2.getLatLon().lat);
            }
        });
    }

    public static <T extends LocationObject> void sortByLon(List<T> data) {
        Collections.sort(data, new Comparator<LocationObject>() {
            @Override
            public int compare(LocationObject o1, LocationObject o2) {
                return Double.compare(o1.getLatLon().lon, o2.getLatLon().lon);
            }
        });
    }

}
