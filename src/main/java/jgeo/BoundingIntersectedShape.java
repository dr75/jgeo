package jgeo;

import java.util.ArrayList;
import java.util.List;

// Combine multiple bounding shapes using boolean AND
public class BoundingIntersectedShape implements BoundingShape {
    private BoundingBox bb;
    private final List<BoundingShape> shapes = new ArrayList<>();

    public BoundingIntersectedShape(BoundingShape s1) {
        this.bb = new BoundingBox(s1.getLowerLeft(), s1.getUpperRight());
        add(s1);
    }

    public BoundingIntersectedShape(BoundingShape s1, BoundingShape s2) {
        this(s1);
        add(s2);
    }

    public void add(BoundingShape s) {
        this.bb = new BoundingBox(this.bb, s);
        shapes.add(s);
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
        return contains(p.lat, p.lon);
    }

    @Override
    public boolean contains(double lat, double lon) {
        if (!this.bb.contains(lat, lon)) {
            return false;
        }

        for (BoundingShape shape : this.shapes) {
            if (!shape.contains(lat, lon)) {
                return false;
            }
        }

        return true;
    }
}