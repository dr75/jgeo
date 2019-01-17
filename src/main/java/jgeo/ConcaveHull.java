package jgeo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

public class ConcaveHull implements BoundingShape {

    private final List<LatLon> list = new ArrayList<>();
    private BoundingBox bb = null;

    private static double EPSILON = 1E-12;

    public ConcaveHull(LatLon latLon) {
        bb = new BoundingBox(latLon, latLon);
        add(latLon);
    }

    public ConcaveHull(SortedSet<LatLon> all) {
        bb = new BoundingBox(all.first(), all.first());
        for (LatLon p : all) {
            add(p);
        }
    }

    public List<LatLon> getList() {
        return list;
    }

    public void add(LatLon p) {
        this.list.add(p);
        this.bb = this.bb.includePoint(p);
    }

    public LatLon getLast() {
        return this.list.get(this.list.size() - 1);
    }

    public BoundingBox getBoundingBox() {
        return this.bb;
    }

    public LatLon getFirst() {
        return this.list.get(0);
    }

    private boolean inside(LatLon p) {
        if (this.list.contains(p)) {
            return true;
        }

        // iterate over all edges and count how often a vector (1,0) cuts through an edge
        Iterator<LatLon> iter = this.list.iterator();
        LatLon prev = iter.next();
        int intersections = 0;
        while (iter.hasNext()) {
            LatLon pNext = iter.next();

            // special case for points on the hull -> consider inside
            if (isOnLine(p, prev, pNext)) {
                return true;
            }
            int intersects = intersectsHorizontally(p, prev, pNext);

            // if parallel, test again with slightly shifted point
            if (intersects == -1) {
                intersects = intersectsHorizontally(new LatLon(p.lat + EPSILON, p.lon), prev, pNext);
            }

            if (intersects == 1) {
                intersections++;
            }

            prev = pNext;
        }

        return intersections % 2 == 1;
    }

    private boolean isOnLine(LatLon p, LatLon prev, LatLon pNext) {
        // first check if in bb
        if (smaller(p.lat, prev.lat, pNext.lat)
                || smaller(p.lon, prev.lon, pNext.lon)
                || greater(p.lat, prev.lat, pNext.lat)
                || greater(p.lon, prev.lon, pNext.lon)) {
            return false;
        }

        // p = prev + s * dir
        double dirLat = pNext.lat - prev.lat;
        double dirLon = pNext.lon - prev.lon;

        double s;
        if (dirLat != 0) {
            // p.lat = prev.lat + s * dir.lat
            s = (p.lat - prev.lat) / dirLat;
        } else {
            // p.lon = prev.lon + s * dir.lon
            s = (p.lon - prev.lon) / dirLon;
        }

        if (inIntervalInclusive(s, 0.0, 1.0)) {
            double lat = prev.lat + s * dirLat;
            double lon = prev.lon + s * dirLon;

            return eq(lat, p.lat) && eq(lon, p.lon);
        } else {
            return false;
        }
    }

    // check if d is in between d1 and d2 but excluding the smaller one
    private boolean inBetweenExcludingLower(double d, double d1, double d2) {
        return d > d1 && d <= d2 || d > d2 && d <= d1;
    }

    private int intersectsHorizontally(LatLon p, LatLon prev, LatLon pNext) {
        if (!inBetweenExcludingLower(p.lat, prev.lat, pNext.lat)) {
            return 0;
        }

        // check on which side...

        // both points to the left
        if (p.lon > prev.lon && p.lon > pNext.lon) {
            return 0;
        }

        // one point to the left, the other to the right
        int intersects = 1;
        if (p.lon > prev.lon || p.lon > pNext.lon) {
            // compute intersection
            // prev + r*dir = p + s*(0,1)
            // if x > 0, the point if left of the line and intersects
            double dirLat = pNext.lat - prev.lat;
            double dirLon = pNext.lon - prev.lon;

            // horizontal -> parallel
            if (dirLat == 0) {
                // check if left of line or on line
                if (p.lon < prev.lon && p.lon < pNext.lon) {
                    // left of line
                    intersects = -1;
                } else {
                    // on line
                    intersects = 1;
                }
            } else {
                //prev.lat + r * dirLat = p.lat;
                //prev.lon + r * dirLon = p.lon + s;
                double r = (p.lat - prev.lat) / dirLat;
                double s = prev.lon + r * dirLon - p.lon;
                intersects = (s > 0 ? 1 : 0);
            }
        }

        // remaining case is both points on the right

        return intersects;
    }

    // check whether p0->p1 intersects any of the edges
    public boolean intersects(LatLon p0, LatLon p1, boolean ignoreLastPoint) {
        Iterator<LatLon> iter = this.list.iterator();
        LatLon prev = iter.next();
        LatLon last = this.getLast();
        while (iter.hasNext()) {
            LatLon pNext = iter.next();
            boolean ignoreQ1 = ignoreLastPoint && pNext == last;
            if (intersects(p0, p1, prev, pNext, ignoreQ1)) {
                return true;
            }
            prev = pNext;
        }
        return false;
    }

    private boolean intersects(LatLon p0, LatLon p1, LatLon q0, LatLon q1, boolean ignoreQ1) {
        if (!canIntersect(p0, p1, q0, q1)) {
            return false;
        }

        // actual intersection
        // p0 + r * dp = q0 + s * dq
        // p0.x + r * dp.x = q0.x + s * dq.x
        // p0.y + r * dp.y = q0.y + s * dq.y
        LatLon dp = new LatLon(p1.lat - p0.lat, p1.lon - p0.lon);
        LatLon dq = new LatLon(q1.lat - q0.lat, q1.lon - q0.lon);

        // zero length vector
        if (dp.lat == 0 && dp.lon == 0
                || dq.lat == 0 && dq.lon == 0) {
            return false;
        }

        // parallel
        if (dp.lat == 0 && dq.lat == 0
                || dp.lon == 0 && dq.lon == 0) {
            return false;
        }

        // parallel
        // dq.lat / dq.lon == dp.lat / dp.lon
        // dq.lat * dp.lon == dp.lat * dq.lon 
        if (dq.lat * dp.lon == dp.lat * dq.lon) {
            return false;
        }

        double r, s;
        if (dp.lat == 0) {
            // p0.y + r * dp.y = q0.y + s * dq.y
            // (p0.y - q0.y) / dq.y = s
            s = (p0.lat - q0.lat) / dq.lat;

            // p0.x + r * dp.x = q0.x + s * dq.x
            // r = (q0.x + s * dq.x - p0.x) / dp.x
            r = (q0.lon + s * dq.lon - p0.lon) / dp.lon;
        } else if (dp.lon == 0) {
            // p0.x + r * dp.x = q0.x + s * dq.x
            // (p0.x - q0.x) / dq.x = s
            s = (p0.lon - q0.lon) / dq.lon;

            // p0.y + r * dp.y = q0.y + s * dq.y
            // r = (q0.y + s * dq.y - p0.y) / dp.y
            r = (q0.lat + s * dq.lat - p0.lat) / dp.lat;
        } else if (dq.lat == 0) {
            // p0.y + r * dp.y = q0.y + s * dq.y
            // r = (q0.y - p0.y) / dp.y
            r = (q0.lat - p0.lat) / dp.lat;

            // p0.x + r * dp.x = q0.x + s * dq.x
            // (p0.x + r * dp.x - q0.x) / dq.x = s
            s = (p0.lon + r * dp.lon - q0.lon) / dq.lon;
        } else {
            // p0.x + r * dp.x = q0.x + s * dq.x
            // p0.y + r * dp.y = q0.y + s * dq.y
            // r = (q0.x + s * dq.x - p0.x) / dp.x
            // p0.y + (q0.x + s * dq.x - p0.x) / dp.x * dp.y = q0.y + s * dq.y
            // a = dp.y / dp.x
            // p0.y + (q0.x - p0.x) * a + s * dq.x * a = q0.y + s * dq.y
            // p0.y + (q0.x - p0.x) * a - q0.y = + s * dq.y - s * dq.x * a
            // p0.y + (q0.x - p0.x) * a - q0.y = + s * (dq.y - dq.x * a)
            // (p0.y + (q0.x - p0.x) * a - q0.y) / (dq.y - dq.x * a) = s
            double a = dp.lat / dp.lon;
            double b = dq.lat - dq.lon * a;

            // divide by zero below if dq.lat / dq.lon == dp.lat / dp.lon, 
            // i.e., dp is a scaled version of dq, so both are parallel.
            // this is checked above.

            s = (p0.lat + (q0.lon - p0.lon) * a - q0.lat) / b;
            r = (q0.lon + s * dq.lon - p0.lon) / dp.lon;
        }

        if (ignoreQ1 && Math.abs(s - 1) < EPSILON) {
            return false;
        } else if (inIntervalInclusive(r, 0.0, 1.0) && inIntervalInclusive(s, 0.0, 1.0)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean inIntervalInclusive(double d, double lowerBound, double upperBound) {
        return d >= (lowerBound - EPSILON) && d <= (upperBound + EPSILON);
    }

    private boolean canIntersect(LatLon p0, LatLon p1, LatLon q0, LatLon q1) {
        if (smaller(p0.lat, p1.lat, q0.lat, q1.lat)
                || smaller(q0.lat, q1.lat, p0.lat, p1.lat)
                || smaller(p0.lon, p1.lon, q0.lon, q1.lon)
                || smaller(q0.lon, q1.lon, p0.lon, p1.lon)) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean lt(double d1, double d2) {
        return d1 < (d2 - EPSILON);
    }

    public static boolean eq(double d1, double d2) {
        return Math.abs(d1 - d2) < EPSILON;
    }

    // check if all x are smaller than all y
    private boolean smaller(double x, double y0, double y1) {
        return lt(x, y0) && lt(x, y1);
    }

    private boolean greater(double x, double y0, double y1) {
        return lt(y0, x) && lt(y1, x);
    }

    // check if all x are smaller than all y
    private boolean smaller(double x0, double x1, double y0, double y1) {
        return lt(x0, y0) && lt(x0, y1) && lt(x1, y0) && lt(x1, y1);
    }

    public boolean containsEdge(LatLon p0, LatLon p1) {
        for (int i = 0; i < this.list.size() - 1; i++) {
            LatLon p = this.list.get(i);
            if (p == p0 && this.list.get(i + 1) == p1) {
                return true;
            }
        }

        return false;
    }

    public boolean isOnHull(LatLon p) {
        Iterator<LatLon> iter = this.list.iterator();
        LatLon prev = iter.next();
        while (iter.hasNext()) {
            LatLon pNext = iter.next();
            if (isOnLine(p, prev, pNext)) {
                return true;
            }
            prev = pNext;
        }

        return false;
    }

    public int size() {
        return this.list.size();
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
        return this.bb.getCenter();
    }

    @Override
    public boolean contains(LatLon p) {
        return this.bb.contains(p) && this.inside(p);
    }

    @Override
    public boolean contains(double lat, double lon) {
        return this.bb.contains(lat, lon) && this.inside(new LatLon(lat, lon));
    }

}
