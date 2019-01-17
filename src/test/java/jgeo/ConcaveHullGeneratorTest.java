package jgeo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class ConcaveHullGeneratorTest {

    @Rule
    public TestWatcher watcher = new TestWatcher() {
        @Override
        protected void starting(Description description) {
            System.out.println("* Test starting: " + description.getMethodName());
        }

        @Override
        protected void finished(Description description) {
            System.out.println("* Test finished: " + description.getMethodName());
            System.out.println();
        }
    };

    private SortedSet<LatLon> sortedSet(List<LatLon> list) {
        SortedSet<LatLon> res = ConcaveHullGenerator.createSortedSet();
        res.addAll(list);
        return res;
    }

    List<List<LatLon>> compute(LatLon[] arr) {
        return compute(arr, MAX_DIST);
    }

    List<List<LatLon>> compute(LatLon[] arr, double maxDist) {
        return compute(Arrays.asList(arr), maxDist);
    }

    List<List<LatLon>> compute(List<LatLon> list, double maxDist) {
        SortedSet<LatLon> set = sortedSet(list);
        List<ConcaveHull> hulls = new ConcaveHullGenerator(set, maxDist).compute();
        List<List<LatLon>> res = new ArrayList<>();
        for (ConcaveHull hull : hulls) {
            res.add(hull.getList());
        }
        return res;
    }

    //        P41-------P42
    //            3917  |
    //                  |
    //                  |
    //                  |
    //             3518 |
    //                  |
    //                  |
    //                  |
    //                  |
    //              3019|
    // P30----P31-------P32
    //  |     |         |
    //  |     |         |
    // P20----P21-------P22
    final static LatLon P21 = new LatLon(1.9, 1);
    final static LatLon P22 = new LatLon(2, 2);
    final static LatLon P32 = new LatLon(3, 2);
    final static LatLon P42 = new LatLon(4, 2);
    final static LatLon P31 = new LatLon(3, 1);
    final static LatLon P41 = new LatLon(4, 1);
    final static LatLon P3917 = new LatLon(3.9, 1.7);
    final static LatLon P3518 = new LatLon(3.5, 1.8);
    final static LatLon P3019 = new LatLon(3.01, 1.9);
    final static double MAX_DIST = P21.getDistanceInMeters(P32) * 1.2;

    @Test
    public void testOnePoint() {
        LatLon[] list = { P21 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());
        assertArrayEquals(list, hull.get(0).toArray());
    }

    @Test
    public void testTwoPoints() {
        LatLon[] list = { P21, P22 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());

        LatLon[] expected = { P21, P22, P21 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testThreePoints_a() {
        LatLon[] list = { P21, P22, P32 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());

        LatLon[] expected = { P21, P22, P32, P21 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testThreePoints_b() {
        LatLon[] list = { P22, P32, P31 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());

        LatLon[] expected = { P22, P32, P31, P22 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testThreePoints_c() {
        LatLon[] list = { P22, P32, P42 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());

        LatLon[] expected = { P22, P32, P42, P32, P22 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testThreePoints_d() {
        LatLon[] list = { P3917, P42, P41 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());

        LatLon[] expected = { P3917, P42, P41, P3917 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testFourPoints_a() {
        LatLon[] list = { P21, P22, P32, P31 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());

        LatLon[] expected = { P21, P22, P32, P31, P21 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testFourPoints_b() {
        LatLon[] list = { P22, P32, P42, P41 };
        List<List<LatLon>> hull = compute(list);
        assertEquals(1, hull.size());

        LatLon[] expected = { P22, P32, P42, P41, P32, P22 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testConcave() {
        LatLon[] list = { P21, P22, P32, P42, P41, P3019 };
        double maxDist = P21.getDistanceInMeters(P3019) + 1;
        List<List<LatLon>> hull = compute(list, maxDist);
        assertEquals(1, hull.size());

        LatLon[] expected = { P21, P22, P32, P42, P41, P3019, P21 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testConcaveCycle() {
        LatLon[] list = { P32, P42, P41, P3917, P3518, P3019 };
        double maxDist = P3019.getDistanceInMeters(P41) - 1;
        List<List<LatLon>> hull = compute(list, maxDist);
        assertEquals(1, hull.size());

        LatLon[] expected = { P32, P42, P41, P3518, P3019, P32 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testRealData() {
        //     p4
        //    /
        //  p3
        //    \
        //     p2
        //      \
        //       p1
        //
        // dist p2 -> p4 is bigger than the accepted dist so the hull is p1,p2,p3,p4,p3,p2,p1

        LatLon p1 = new LatLon(7.300000, -61.500000);
        LatLon p2 = new LatLon(10.600000, -61.333333);
        LatLon p3 = new LatLon(14.600000, -61.000000);
        LatLon p4 = new LatLon(16.266667, -61.516667);
        LatLon[] list = { p1, p2, p3, p4 };

        List<List<LatLon>> hull = compute(list, 500 * 1000);
        assertEquals(1, hull.size());

        LatLon[] expected = { p1, p2, p3, p4, p3, p2, p1 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testInnerNodes() {

        LatLon[] arr = { P21, P22, P32, P31 };
        List<LatLon> list = new ArrayList<>();
        list.addAll(Arrays.asList(arr));

        for (int i = 0; i < 100; ++i) {
            double minLat = P21.lat + 0.11;
            double minLon = P21.lon + 0.11;
            double maxLat = P32.lat - 0.1;
            double maxLon = P32.lon - 0.1;
            double maxDeltaLat = maxLat - minLat;
            double maxDeltaLon = maxLon - minLon;

            double lat = Math.random() * maxDeltaLat + minLat;
            double lon = Math.random() * maxDeltaLon + minLon;
            LatLon p = new LatLon(lat, lon);
            list.add(p);
        }

        List<List<LatLon>> hull = compute(list, MAX_DIST);
        assertEquals(1, hull.size());

        LatLon[] expected = { P21, P22, P32, P31, P21 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }

    @Test
    public void testGrid() {
        //testGridParam(-90, -179, 90, 180);

        testGridParam(-1, -1, 1, 1);

        testGridParam(-90, -180, 90, -177);
        testGridParam(-90, +177, 90, +180);
        testGridParam(-90, -2, 90, 2);

        testGridParam(-90, -180, -87, 180);
        testGridParam(+87, -180, +90, 180);
        testGridParam(-1, -180, -1, 180);

        testGridParam(-1, -1, 1, 1, 250 * 1000);
    }

    private void testGridParam(int minLat, int minLon, int maxLat, int maxLon) {
        testGridParam(minLat, minLon, maxLat, maxLon, MAX_DIST * 2);
    }

    private void testGridParam(int minLat, int minLon, int maxLat, int maxLon, double maxDist) {
        List<LatLon> list = new ArrayList<>();
        List<LatLon> expected = new ArrayList<>();

        for (int lat = minLat; lat <= maxLat; ++lat) {
            for (int lon = minLon; lon <= maxLon; ++lon) {
                LatLon p = new LatLon(lat, lon);
                list.add(p);

                if (lat == minLat || lat == maxLat || lon == minLon || lon == maxLon) {
                    expected.add(p);
                }
            }
        }

        expected.add(expected.get(0));

        List<List<LatLon>> hull = compute(list, maxDist);
        assertEquals(1, hull.size());

        for (LatLon p : hull.get(0)) {
            double lat = p.lat;
            double lon = p.lon;
            boolean onHull = lat == minLat || lat == maxLat || lon == minLon || lon == maxLon;
            assertTrue(onHull);
        }
    }

    @Test
    public void testNodesOnNedge() {
        //               P41--------P42
        //                |          |
        //                |          |
        //                |          |
        // P30-----------P31    Pi   |
        // |                         |
        // |                         |
        // |                         |
        // P20-----------P21        P22
        //                  \        | 
        //                   -------P12
        //
        // MAXDIST = 2 * edge.length
        // 22->42->41->21->20->30->(!31)->20->21->22

        LatLon P12 = new LatLon(1, 2);
        LatLon P20 = new LatLon(2, -1);
        LatLon P30 = new LatLon(3, -1);
        LatLon P315 = new LatLon(3, 1.5);

        double maxDist = P21.getDistanceInMeters(P41) * 1.05;

        LatLon[] list = { P12, P22, P42, P41, P31, P21, P20, P30, P315 };
        List<List<LatLon>> hull = compute(list, maxDist);
        assertEquals(1, hull.size());

        LatLon[] expected = { P12, P22, P42, P41, P31, P30, P20, P21, P12 };
        assertArrayEquals(expected, hull.get(0).toArray());
    }
}
