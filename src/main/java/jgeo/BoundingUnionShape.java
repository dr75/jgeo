package jgeo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//Union of multiple bounding shapes using OR
public class BoundingUnionShape implements BoundingShape {

    private BoundingBox bb;
    private final List<BoundingShape> shapes = new ArrayList<>();

    public BoundingUnionShape(List<? extends BoundingShape> shapes) {
        Iterator<? extends BoundingShape> iter = shapes.iterator();
        BoundingShape first = iter.next();
        this.bb = new BoundingBox(first.getLowerLeft(), first.getUpperRight());

        while (iter.hasNext()) {
            add(iter.next());
        }
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
            if (shape.contains(lat, lon)) {
                return true;
            }
        }

        return false;
    }
}
