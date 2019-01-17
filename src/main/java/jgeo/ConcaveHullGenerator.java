package jgeo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

// !!!!!!!
// this does currently not support hulls that cross 90deg lat and 180deg lon
public class ConcaveHullGenerator {
    public static SortedSet<LatLon> createSortedSet() {
        return new TreeSet<>(new Comparator<LatLon>() {

            private int compareDouble(double d1, double d2) {
                return d1 < d2 ? -1 : (d1 > d2 ? 1 : 0);
            }

            @Override
            public int compare(LatLon o1, LatLon o2) {
                int cmp = compareDouble(o1.lat, o2.lat);
                if (cmp == 0) {
                    cmp = compareDouble(o1.lon, o2.lon);
                }
                return cmp;
            }
        });
    }

    private final SortedSet<LatLon> points;
    private LatLon prev = null;
    private LatLon direction = null; // direction vector of last inserted element
    private final double maxDistanceMeter;

    public ConcaveHullGenerator(SortedSet<LatLon> points, double maxDistanceMeter) {
        this.points = points;
        this.maxDistanceMeter = maxDistanceMeter;
    }

    public List<ConcaveHull> compute() {
        List<ConcaveHull> res = new ArrayList<>();

        while (points.size() > 0) {
            ConcaveHull hull = computeHull();
            removeAllInHull(hull);
            res.add(hull);
        }

        return res;
    }

    private void removeAllInHull(ConcaveHull hull) {
        ArrayList<LatLon> del = new ArrayList<>();
        for (LatLon p : this.points) {
            if (hull.contains(p)) {
                del.add(p);
            }
        }
        this.points.removeAll(del);
    }

    private ConcaveHull computeHull() {
        if (this.points.size() <= 2) {
            ConcaveHull hull = new ConcaveHull(this.points);

            // first and last point should be the same
            if (this.points.size() == 2) {
                hull.add(this.points.first());
            }
            return hull;
        }

        ConcaveHull hull = new ConcaveHull(this.points.first());
        prev = hull.getLast();
        direction = new LatLon(0, 1);
        do {
            prev = addToHull(hull);
            if (hull.size() % 100 == 0) {
                System.out.println("Hull at " + hull.getBoundingBox().toString() + " with " + hull.size() + " points ");
            }
        } while (prev != null && prev != hull.getFirst());

        return hull;
    }

    private ArrayList<LatLon> getCandidates(LatLon prev) {
        ArrayList<LatLon> res = new ArrayList<>();
        BoundingBox bb = BoundingBox.aroundPoint(prev, maxDistanceMeter);
        SortedSet<LatLon> withinBox = this.points.tailSet(bb.getLowerLeft()).headSet(bb.getUpperRight());
        for (LatLon p : withinBox) {
            if (p != prev && bb.contains(p) && p.getDistanceInMeters(prev) < maxDistanceMeter) {
                res.add(p);
            }
        }

        return res;
    }

    private LatLon addToHull(ConcaveHull hull) {
        ArrayList<LatLon> candidates = getCandidates(hull.getLast());

        // try to find the smallest angle starting at -180deg (-PI)
        double minAngle = Double.MAX_VALUE;
        double minDist = Double.MAX_VALUE;
        LatLon best = null;
        for (LatLon p : candidates) {
            double candidateAngle = computeAngle(p);
            boolean smaller = lt(candidateAngle, minAngle);
            boolean isBetter = smaller || (eq(candidateAngle, minAngle) && dist(prev, p) < minDist);
            if (isBetter) {
                if (candidateAngle < 0 && intersectsHull(hull, p)
                        || hull.containsEdge(prev, p)) {
                    continue;
                }
                minAngle = candidateAngle;
                minDist = dist(prev, p);
                best = p;
            }
        }

        if (best != null) {
            hull.add(best);
            direction = new LatLon(best.lat - prev.lat, best.lon - prev.lon);
            //double angle = computeAngle(new LatLon(prev.lat, prev.lon + 1));
            //System.out.println(prev.toString() + "->" + best.toString() + "; " + minAngle * 180 / Math.PI);
        }
        return best;
    }

    private double dist(LatLon p1, LatLon p2) {
        double dLat = p1.lat - p2.lat;
        double dLon = p1.lon - p2.lon;
        return (dLat * dLat) + (dLon * dLon);
    }

    private boolean lt(double d1, double d2) {
        return ConcaveHull.lt(d1, d2);
    }

    private boolean eq(double d1, double d2) {
        return ConcaveHull.eq(d1, d2);
    }

    private boolean intersectsHull(ConcaveHull hull, LatLon p) {
        // ignore p == first as this is where we try to get
        if (p == hull.getFirst()) {
            return false;
        }

        // for an inverted edge of the hull we get true but this is actually a valid case 
        return (hull.intersects(prev, p, true) || hull.isOnHull(p)) && !hull.containsEdge(p, prev);
    }

    private double computeAngle(LatLon p) {
        // compute angle between direction and prev->p
        double dLon = p.lon - prev.lon;
        double dLat = p.lat - prev.lat;
        double lengthA = Math.sqrt(direction.lat * direction.lat + direction.lon * direction.lon);
        double lengthB = Math.sqrt(dLat * dLat + dLon * dLon);
        double dot = (direction.lat * dLat + direction.lon * dLon) / (lengthA * lengthB);
        double angle = Math.acos(dot);
        if (direction.lon * dLat - direction.lat * dLon < 0)
            angle = -angle;
        return angle;
    }
}
