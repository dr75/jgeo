package jgeo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuadTree<T extends LocationObject> {
    private final int maxLevel;
    private int numNodes = 0;
    private int numObjects = 0;

    // sorted by rank for linear search if too many results
    private final Map<String, T> sortedByRank = new HashMap<String, T>();

    public Collection<T> getAll() {
        return this.sortedByRank.values();
    }

    class Node {

        private final int level;
        private final double x0, y0, x1, y1;
        private final List<T> objects = new ArrayList<T>();

        private Node upperLeftNode = null;
        private Node lowerLeftNode = null;
        private Node upperRightNode = null;
        private Node lowerRightNode = null;

        public Node(double x0, double y0, double x1, double y1, int level) {
            this.x0 = x0;
            this.x1 = x1;
            this.y0 = y0;
            this.y1 = y1;

            this.level = level;
            numNodes++;
        }

        private Node getChild(double x, double y) {
            // has children?
            if (upperRightNode == null) {
                return null;
            }

            final double midX = upperRightNode.x0;
            final double midY = upperRightNode.y0;
            final boolean right = (x >= midX);
            final boolean top = (y >= midY);

            if (right && top) {
                return upperRightNode;
            } else if (right) {
                return lowerRightNode;
            } else if (top) {
                return upperLeftNode;
            } else {
                return lowerLeftNode;
            }
        }

        public Node locate(double x, double y, boolean createIfNotExists) {
            if (level == maxLevel) {
                return this;
            } else if (!createIfNotExists && this.upperLeftNode == null) {
                return null;
            }

            if (this.upperLeftNode == null) {
                split();
            }

            return getChild(x, y).locate(x, y, createIfNotExists);
        }

        private void split() {
            double yMid = (y0 + y1) / 2;
            double xMid = (x0 + x1) / 2;

            final int childLevel = level + 1;
            this.upperLeftNode = new Node(x0, yMid, xMid, y1, childLevel);
            this.upperRightNode = new Node(xMid, yMid, x1, y1, childLevel);
            this.lowerLeftNode = new Node(x0, y0, xMid, yMid, childLevel);
            this.lowerRightNode = new Node(xMid, y0, x1, yMid, childLevel);
        }

        public void add(T obj) {
            objects.add(obj);
            numObjects++;
        }

        public boolean getObjectsInArea(
                TreeSearchData search,
                double x0, double x1,
                double y0, double y1) {
            Node nodeWithLowerLeft = getChild(x0, y0);
            Node nodeWithUpperRight = getChild(x1, y1);

            // If nodes are null we have to return the objects of this area
            // as we are at max level (note: cannot be outside of this area as
            // we only ask for valid coordinates);
            // otherwise get sub nodes to check.

            if (nodeWithLowerLeft == null) {
                for (T poi : this.objects) {
                    if (search.filter.matches(poi)) {
                        search.res.add(poi);
                    }
                }

                //search.tested += this.objects.size()
            } else if (nodeWithLowerLeft == nodeWithUpperRight) {
                // entire search area in same node
                nodeWithLowerLeft.getObjectsInArea(search, x0, x1, y0, y1);
            } else if (nodeWithLowerLeft == this.lowerLeftNode
                    && nodeWithUpperRight == this.upperRightNode) {
                // area across all four nodes
                if (getObjectsInIntersection(search, this.lowerLeftNode, x0, x1, y0, y1)
                        && getObjectsInIntersection(search, this.upperLeftNode, x0, x1, y0, y1)
                        && getObjectsInIntersection(search, this.lowerRightNode, x0, x1, y0, y1)
                        && getObjectsInIntersection(search, this.upperRightNode, x0, x1, y0, y1)) {
                    ;
                }
            } else {
                // area across two nodes
                if (getObjectsInIntersection(search, nodeWithLowerLeft, x0, x1, y0, y1)
                        && getObjectsInIntersection(search, nodeWithUpperRight, x0, x1, y0, y1)) {
                    ;
                }
            }

            return search.res.size() <= search.maxResults;
        }

        private boolean getObjectsInIntersection(TreeSearchData search, QuadTree<T>.Node n, double x0, double x1,
                double y0, double y1) {
            double xMax = x1 > n.x1 ? n.x1 : x1;
            double yMax = y1 > n.y1 ? n.y1 : y1;
            double xMin = x0 < n.x0 ? n.x0 : x0;
            double yMin = y0 < n.y0 ? n.y0 : y0;

            return n.getObjectsInArea(search, xMin, xMax, yMin, yMax);
        }

        private int getNumNodes(int level) {
            if (this.level == level) {
                return 1;
            } else if (this.upperLeftNode == null) {
                return 0;
            }

            return this.lowerLeftNode.getNumNodes(level)
                    + this.lowerRightNode.getNumNodes(level)
                    + this.upperLeftNode.getNumNodes(level)
                    + this.upperRightNode.getNumNodes(level);
        }

        private int getNumNodesWithObjectsOrChilds(int level) {
            if (this.level == level) {
                boolean hasData = (this.upperLeftNode != null || this.objects.size() > 0);
                return (hasData ? 1 : 0);
            } else if (this.upperLeftNode == null) {
                return 0;
            }

            return this.lowerLeftNode.getNumNodesWithObjectsOrChilds(level)
                    + this.lowerRightNode.getNumNodesWithObjectsOrChilds(level)
                    + this.upperLeftNode.getNumNodesWithObjectsOrChilds(level)
                    + this.upperRightNode.getNumNodesWithObjectsOrChilds(level);
        }

        private int getNumObjects() {
            if (this.level == maxLevel) {
                return this.objects.size();
            } else if (this.upperLeftNode == null) {
                return 0;
            }

            return this.lowerLeftNode.getNumObjects()
                    + this.lowerRightNode.getNumObjects()
                    + this.upperLeftNode.getNumObjects()
                    + this.upperRightNode.getNumObjects();
        }

        // max number of objects on a node with givem level
        public int getMaxObjectsAtLevel(int level) {
            if (this.level == level) {
                return this.getNumObjects();
            } else if (this.upperLeftNode == null) {
                return 0;
            }

            int upper = Math.max(this.upperLeftNode.getMaxObjectsAtLevel(level),
                    this.upperRightNode.getMaxObjectsAtLevel(level));
            int lower = Math.max(this.lowerLeftNode.getMaxObjectsAtLevel(level),
                    this.lowerRightNode.getMaxObjectsAtLevel(level));
            return Math.max(upper, lower);
        }

        private Node locateAtLevel(double x, double y, int level) {
            if (this.level == level) {
                // correct level
                return this;
            }

            Node child = getChild(x, y);
            if (child == null) {
                // no children -> no node at that level
                return null;
            }

            return child.locateAtLevel(x, y, level);
        }

        private char getCharForNode(Node n) {
            if (n == null || n.upperLeftNode == null) {
                return ' ';
            }
            int ul = n.upperLeftNode.getNumObjects() > 0 ? 1 : 0;
            int ur = n.upperRightNode.getNumObjects() > 0 ? 1 : 0;
            int ll = n.lowerLeftNode.getNumObjects() > 0 ? 1 : 0;
            int lr = n.lowerRightNode.getNumObjects() > 0 ? 1 : 0;
            int sum = ul + ur + ll + lr;

            if (sum == 4) {
                return 'X';
            } else if (sum == 3) {
                if (ur == 0) {
                    return 'L';
                } else if (ll == 0) {
                    return '7';
                } else if (lr == 0) {
                    return '7';
                } else {
                    return 'A';
                }
            } else if (sum == 2) {
                if (ul == 1 && ur == 1) {
                    return '"';
                } else if (ll == 1 && lr == 1) {
                    return '_';
                } else if (ul == 1 && lr == 1) {
                    return '\\';
                } else if (ll == 1 && ur == 1) {
                    return '/';
                } else {
                    return '|';
                }
            } else if (sum == 1) {
                if (ul == 1 || ur == 1) {
                    return '\'';
                } else {
                    return '.';
                }
            } else {
                return ' ';
            }
        }

        private void printGraphics(int level, boolean invertY, double scaleY) {
            double steps = Math.pow(2, level);
            double stepsY = steps * scaleY;
            double deltaX = (x1 - x0) / steps;
            double deltaY = (y1 - y0) / stepsY;
            double yStart = y0 + deltaY / 2;
            if (invertY) {
                deltaY = -deltaY;
                yStart = y1 + deltaY / 2;
            }
            for (int i = 0; i < stepsY; ++i) {
                double y = yStart + (i * deltaY);
                StringBuffer sb = new StringBuffer();
                for (double x = x0 + deltaX / 2; x < x1; x += deltaX) {
                    Node n = locateAtLevel(x, y, level);
                    char c = getCharForNode(n);
                    sb.append(c);
                }

                System.out.println(sb.toString());
            }
        }
    }

    private final Node root;

    public QuadTree(int maxLevel) {
        this.maxLevel = maxLevel;
        this.root = new Node(-180, -90, +180, +90, 0);
    }

    public void put(T obj, String rank, String uid) {
        LatLon pos = obj.getLatLon();
        Node n = root.locate(pos.lon, pos.lat, true);
        n.add(obj);

        // include the uid to avoid duplicates
        sortedByRank.put(rank + uid, obj);
    }

    private boolean matches(T obj, SearchFilter filter, BoundingShape area) {
        return (area == null || area.contains(obj.getLatLon()))
                && filter.matches(obj);
    }

    public SearchResult<T> linearSearch(SearchFilter filter, BoundingShape area) {
        Collection<T> res = new ArrayList<T>();
        int maxResults = filter.getMaxResults();

        Collection<T> data = sortedByRank.values();

        int tested = 0;
        for (T obj : data) {
            tested++;
            if (matches(obj, filter, area)) {
                res.add(obj);
            }

            if (res.size() == maxResults) {
                break;
            }
        }

        int matched = res.size();

        if (tested < 100) {
            tested = 0;
            matched = 0;
            for (T obj : data) {
                tested++;
                if (matches(obj, filter, area)) {
                    matched++;
                }

                if (tested == 100) {
                    break;
                }
            }

        }

        double ratio = matched / (double) tested;
        int estimatedTotalHits = (int) (ratio * data.size());

        return new SearchResult<T>(res, estimatedTotalHits);
    }

    public SearchResult<T> getInArea(SearchFilter filter, BoundingShape area) {
        int maxResults = getMaxResults(filter.getMaxResults());

        SearchResult<T> res = null;
        if (area != null) {
            TreeSearchData search = new TreeSearchData(filter, maxResults);
            treeSearch(search, area.getLowerLeft(), area.getUpperRight());
            int estimatedTotalHits = search.res.size();
            res = new SearchResult<T>(search.res, estimatedTotalHits);
        }

        // if the search returned null (too many results), we have to do a linear search
        // to get an ordered list of results
        if (res == null || res.data.size() > maxResults) {
            res = linearSearch(filter, area);
        }

        return res;
    }

    class TreeSearchData {
        public final SearchFilter filter;
        public final int maxResults;
        public Collection<T> res = new ArrayList<T>();

        TreeSearchData(SearchFilter filter, int maxResults) {
            this.filter = filter;
            this.maxResults = maxResults;
        }
    }

    private void treeSearch(TreeSearchData search, LatLon lowerLeft, LatLon upperRight) {

        // if the area is empty then return an empty set
        if (lowerLeft.lon == upperRight.lon || lowerLeft.lat == upperRight.lat) {
            // return empty result
        } else if (lowerLeft.lon > upperRight.lon) {
            // search accross +/-180 deg. 
            LatLon lowerLeft1 = lowerLeft;
            LatLon upperRight1 = new LatLon(upperRight.lat, 180);

            LatLon lowerLeft2 = new LatLon(lowerLeft.lat, -180);
            LatLon upperRight2 = upperRight;

            treeSearch(search, lowerLeft1, upperRight1);

            // only search the other part if result set not too big
            if (search.res.size() <= search.maxResults) {
                treeSearch(search, lowerLeft2, upperRight2);
            }
        } else {
            root.getObjectsInArea(search, lowerLeft.lon, upperRight.lon, lowerLeft.lat, upperRight.lat);
        }
    }

    private int getMaxResults(double limit) {

        // if 1% match, then 
        // - linear search for the best 50 requires a scan 
        //   of the 50 * 100 = 5K
        // - tree search returns 1Mio / 100 = 10K in unsorted
        //
        //    1 Mio / x = 50 * x = maxRes
        // -> 1 Mio / 50 = x^2 
        // -> x = sqrt(n / limit) = sqrt(1Mio / 50) = sqrt(20K) = 140
        // -> maxRes = limit * sqrt(n / limit) = sqrt(n * limit)
        // So we set the upper limit of results to sqrt(1Mio * 50) = 7K.
        //
        // More examples: 
        // 0.1% match
        // - linear search requires 50 * 1000 = 50K
        // - tree search returns 1Mio / 1000 = 1K	(i.e., less than 7K)
        // 0.8% match
        // - linear search requires 50 * 125 = 6.250K
        // - tree search returns 1Mio / 125 = 8K	(i.e., more than 7K)
        return (int) Math.sqrt(this.numObjects * limit);
    }

    private static double getEdgeLengthForLevel(int level) {
        // level 0: size of node = 40000x40000km
        // level 1: size of node = 20000x20000km
        // ...
        double fract = Math.pow(2, level);
        return 40000 / fract;
    }

    public void printInfo() {
        double scaleY = 1.0 / 3;
        root.printGraphics(7, true, scaleY);
        //root.printGraphics(8, true, scaleY);

        int avgLevel1 = this.maxLevel - 2;
        int avgLevel2 = this.maxLevel - 4;
        System.out.println("Quadtree with " + numNodes + " nodes and " + numObjects + " objects ");
        System.out.println("  levels                  : " + this.maxLevel);
        System.out.println("  level " + this.maxLevel + " edge size [km] : " + getEdgeLengthForLevel(this.maxLevel));
        System.out.println("  level " + this.maxLevel + " nodes          : " + root.getNumNodes(this.maxLevel));
        System.out.println(
                "  level " + this.maxLevel + " nodes not empty: " + root.getNumNodesWithObjectsOrChilds(this.maxLevel));
        System.out
                .println("  level " + this.maxLevel + " max objects    : " + root.getMaxObjectsAtLevel(this.maxLevel));
        System.out.println("  level " + avgLevel1 + " edge size [km] : " + getEdgeLengthForLevel(avgLevel1));
        System.out.println("  level " + avgLevel1 + " nodes          : " + root.getNumNodes(avgLevel1));
        System.out.println(
                "  level " + avgLevel1 + " nodes not empty: " + root.getNumNodesWithObjectsOrChilds(avgLevel1));
        System.out.println("  level " + avgLevel1 + " max objects    : " + root.getMaxObjectsAtLevel(avgLevel1));
        System.out.println("  level  " + avgLevel2 + " edge size [km] : " + getEdgeLengthForLevel(avgLevel2));
        System.out.println("  level  " + avgLevel2 + " nodes          : " + root.getNumNodes(avgLevel2));
        System.out.println(
                "  level  " + avgLevel2 + " nodes not empty: " + root.getNumNodesWithObjectsOrChilds(avgLevel2));
        System.out.println("  level  " + avgLevel2 + " max objects    : " + root.getMaxObjectsAtLevel(avgLevel2));
    }
}
