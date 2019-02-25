package jgeo;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

public class SpatialHashTest extends TestBase {
    static class TestObject implements LocationObject {
        private final LatLon latLon;
        TestObject(double lat, double lon) {
            this.latLon = new LatLon(lat, lon);
        }

        @Override
        public LatLon getLatLon() {
            return latLon;
        }
    }
    
    private SpatialHash<TestObject> hash;
    
    private void testGet(LatLon key, LatLon expected) {
        LatLon value = hash.get(key).latLon;
        assertEquals(expected.lat, value.lat, 0.0);
        assertEquals(expected.lon, value.lon, 0.0);
    }

    @Test
    public void testGetLatLon() {
        ArrayList<TestObject> values = new ArrayList<>();
        values.add(new TestObject(0, 0));
        values.add(new TestObject(0, 1));
        values.add(new TestObject(1, 0));
        values.add(new TestObject(1, 1));
        
        this.hash = new SpatialHash<>(values);
        testGet(new LatLon(0,0), new LatLon(0,0));
        testGet(new LatLon(0,1), new LatLon(0,1));
        testGet(new LatLon(1,0), new LatLon(1,0));
        testGet(new LatLon(1,1), new LatLon(1,1));

        testGet(new LatLon(1.1, 1.0), new LatLon(1,1));
        testGet(new LatLon(1.1, 1.1), new LatLon(1,1));
        testGet(new LatLon(1.0, 1.1), new LatLon(1,1));
        testGet(new LatLon(0.9, 1.0), new LatLon(1,1));
        testGet(new LatLon(0.9, 1.1), new LatLon(1,1));
        testGet(new LatLon(1.1, 0.9), new LatLon(1,1));
        testGet(new LatLon(1.0, 0.9), new LatLon(1,1));

        testGet(new LatLon(0.1, 0.0), new LatLon(0,0));
        testGet(new LatLon(0.1, 0.1), new LatLon(0,0));
        testGet(new LatLon(0.0, 0.1), new LatLon(0,0));
        testGet(new LatLon(-0.1, 0.0), new LatLon(0,0));
        testGet(new LatLon(-0.1, 0.1), new LatLon(0,0));
        testGet(new LatLon(0.1, -0.1), new LatLon(0,0));
        testGet(new LatLon(0.0, -0.1), new LatLon(0,0));

        testGet(new LatLon(0.49, 0.49), new LatLon(0,0));
        testGet(new LatLon(0.49, 0.51), new LatLon(0,1));
        testGet(new LatLon(0.51, 0.49), new LatLon(1,0));
        testGet(new LatLon(0.51, 0.51), new LatLon(1,1));
    }

    @Test
    public void testGetLatLonBigHorizontalDistance() {
        // Test that if we have points in the same row with 
        // big vertical distances, then we also find values 
        // in neighbor rows. 
        ArrayList<TestObject> values = new ArrayList<>();
        values.add(new TestObject(-1,  100));
        values.add(new TestObject(-1, -100));
        values.add(new TestObject( 0,  100));
        
        // a lot values in the same row
        int i_max = 100;
        for (int i = 0; i < i_max; i++) {
            double lat = (2.0 * i / i_max) - 1.0;
            values.add(new TestObject(lat, 100));
        }
        
        values.add(new TestObject( 1,  100));
        values.add(new TestObject( 1, -100));
        values.add(new TestObject(-2,    0));
        values.add(new TestObject( 2,    0));

        
        this.hash = new SpatialHash<>(values);

        // test values in rows below
        testGet(new LatLon(0.1,0), new LatLon(2,0));
        
        // test values in rows above
        testGet(new LatLon(-0.1,0), new LatLon(-2,0));
    }

}
