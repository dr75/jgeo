package jgeo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * |8.9..9.0 -> 2
 * |8.4..8.8 -> 1
 * |3.0..8.3 -> 0
 * 
 * min  : 3.0
 * max  : 9.0
 * delta: 9.0 - 3.0 = 6.0
 * lt-size: 3
 * scale: 3/6.0 = 0.5
 * h(x) = x <= min      : 0
 *        min < x < max : (int)((x - 3.0) * 0.5)
 *        x >= max      : k-1
 *        
 *      h(3.0) = (int)0.0	 0 -> 0
 *      h(4.0) = (int)0.5	 0 -> 0
 *      h(5.0) = (int)1.0	 1 -> 0 
 *      h(6.0) = (int)1.5	 1 -> 0
 *      h(7.0) = (int)2.0	 2 -> 0
 *      h(8.0) = (int)2.5	 2 -> 0
 *      h(8.9) = (int)2.95	 2 -> 2
 *      h(9.0) = 9 >= max ->   -> 2
 *      
 */
public class SpatialHash<E extends LocationObject> implements Iterable<E> {
	
	private static interface SpatialHashValue {
		double getValueToHash();
	}
	
	private static class HashFunction {
		private final double hMin;
		private final double hMax;
		private final double hScale;
		private final int lookupTableSize;
		private final int[] lookupTable;
		
		public <T extends SpatialHashValue> HashFunction(List<T> data, double min, double max, int lookupTableSize) {
			this.hMin = min;
			this.hMax = max;
			double delta = hMax - hMin;
			this.lookupTableSize = lookupTableSize;
			this.hScale = lookupTableSize / delta;
			this.lookupTable = createLookupTable(data);
		}
		
		private int computeHashValue(double value) {
			int key = 0;	// return first bucket if too small
			
			if (value >= this.hMin) {
				int idx = (int)((value - this.hMin) * this.hScale);
				
				// we have to not check before here as we might get above the boundaries 
				// due to numerical imprecision if we check before
				if (idx < this.lookupTableSize) {
					key = idx;
				} else if (value >= this.hMax) {	// this value has to be treated as in
					key = this.lookupTableSize - 1;
				}
			}
			
			return key;
		}
		
		public int hash(double d) {
			int h = computeHashValue(d);
			int bucket = lookupTable[h];
			return bucket;
		}
		
		private <T extends SpatialHashValue> int[] createLookupTable(List<T> data) {
			int[] res = new int[this.lookupTableSize];
			for (int rowIdx = 0; rowIdx < data.size(); ++rowIdx) {
				T row = data.get(rowIdx);
				T next = rowIdx+1 < data.size() ? data.get(rowIdx + 1) : null;
				int hRow = this.computeHashValue(row.getValueToHash());
				int hNext = next != null ? this.computeHashValue(next.getValueToHash()) : res.length;
				if (hRow == hNext) {
					hRow = hNext;
				}
				for (int h = hRow; h < hNext; ++h) {
					res[h] = rowIdx;
				}
			}
			
			return res;
		}

		public <T extends SpatialHashValue> int lookup(List<T> data, double value) {
			int bucket = hash(value);
			
			// the candidate is either the correct one or above
			// go back until correct one found if needed
			while (bucket > 0
				&& value < data.get(bucket - 1).getValueToHash()) {
				bucket--;
			};
			
			return bucket;
		}
	}
	
	private static class DataCell<E extends LocationObject> implements SpatialHashValue, LocationObject {

		E data;
		
		DataCell(E data) {
			this.data = data;
		}

		@Override
		public double getValueToHash() {
			return getLatLon().lon;
		}

		@Override
		public LatLon getLatLon() {
			return this.data.getLatLon();
		}
	}
	
	private class DataRow implements SpatialHashValue {
		private final List<DataCell<E>> data = new ArrayList<>();
		private final int gridSize;
		private HashFunction hashFunction;
		private double latMin = Double.MAX_VALUE;
		private double latMax = -Double.MAX_VALUE;

		public DataRow(int gridSize) {
			this.gridSize = gridSize;
		}

		public void add(E e) {
			LatLon p = e.getLatLon();
			if (p.lat < this.latMin) {
				this.latMin = p.lat;
			}
			if (p.lat > this.latMax) {
			    this.latMax = p.lat;
			}
			this.data.add(new DataCell<>(e));
		}

		@Override
		public double getValueToHash() {
			return latMin;
		}

        public void init() {
            LocationObject.sortByLon(data);
            double min = this.data.get(0).data.getLatLon().lon;
            double max = this.data.get(this.data.size() - 1).data.getLatLon().lon;
            this.hashFunction = new HashFunction(this.data, min, max, this.gridSize);
        }

        public void get(List<E> res, BoundingShape bb) {
            LatLon ll = bb.getLowerLeft();
            LatLon ur = bb.getUpperRight();
            
            int bucketMin = this.hashFunction.lookup(this.data, ll.lon);
            int bucketMax = this.hashFunction.lookup(this.data, ur.lon);
            for (int col = bucketMin; col <= bucketMax; col++) {
                E e = this.data.get(col).data;
                if (bb.contains(e.getLatLon())) {
                    res.add(e);
                }
            }
        }

        public E get(LatLon at) {
            int bucket = this.hashFunction.lookup(this.data, at.lon);
            int bucketMin = Math.max(bucket - 1, 0);
            int bucketMax = Math.min(bucket + 1, this.data.size() - 1);
            
            E res = null;
            double distMin = Double.MAX_VALUE;
            for (int col = bucketMin; col <= bucketMax; col++) {
                E e = this.data.get(col).data;
                double dist = e.getLatLon().getDistanceInMeters(at);
                if (dist < distMin) {
                    distMin = dist;
                    res = e;
                }
            }
            
            return res;
        }
	}
	
	private final int length;
	private final BoundingShape bb;
	private final int gridSize;
	private final List<DataRow> rows = new ArrayList<>();
	private HashFunction hashFunction;
	
	public SpatialHash(List<E> data) {
		this.length = data.size();
		this.bb = new BoundingBox(data);
		this.gridSize = ((int)Math.sqrt(this.length - 1)) + 1;	// -1 / +1 to round up
		
		init(data);
	}
	
	private void init(List<E> data) {
		LocationObject.sortByLat(data);
		store(data);
		
		double min = bb.getLowerLeft().lat;
		double max = bb.getUpperRight().lat;
		this.hashFunction = new HashFunction(this.rows, min, max, this.gridSize * 10);
	}
	
	public List<E> get(BoundingShape bb) {
		int rowMin = this.hashFunction.lookup(this.rows, bb.getLowerLeft().lat);
		int rowMax = this.hashFunction.lookup(this.rows, bb.getUpperRight().lat);
		
		List<E> res = new ArrayList<>();
		for (int r = rowMin; r <= rowMax; r++) {
			this.rows.get(r).get(res, bb);
		}
		return res;
	}
	
	// get the value closest to at 
    public E get(LatLon at) {
        // This method first checks the row this coordinate would be stored in.
        // As this value might have a big horizontal distance, we then expand  
        // the search to neighbor rows that may contain other values that are 
        // closer due to a small horizontal distance.
        //
        // So in the worst case, this means we have to go through all rows, which
        // is O(sqrt(n)).
        
        if (this.rows.size() == 0) {
            return null;
        }
        
        // Compute the vertical distance between two points on the same longitude
        // with a distance of one degree so that we can use that conversion later
        // to speed up measurements.
        double latToMeter = at.getDistanceInMeters(new LatLon(at.lat + 1, at.lon));

        // get the closest row
        int row_idx = this.hashFunction.lookup(this.rows, at.lat);
        
        // get the best value in this row
        E res = this.rows.get(row_idx).get(at);
        double distMin = res.getLatLon().getDistanceInMeters(at);
        
        // now look for closer values in the neighbor-rows until they can't be any closer
        for (int r = row_idx + 1; r < this.rows.size(); r++) {
            // check if the value with the smallest latitude in that row could be closer than res
            DataRow row = this.rows.get(r);
            double distRowLat = row.latMin - at.lat;
            double distRow = distRowLat * latToMeter;
            if (distRow >= distMin) {
                break;
            }

            // might be closer, so check the closest value of that row
            E e = row.get(at);
            double dist = e.getLatLon().getDistanceInMeters(at);
            if (dist < distMin) {
                distMin = dist;
                res = e;
            }
        }
        
        // same for the rows below
        for (int r = row_idx - 1; r >= 0; r--) {
            // check if the value with the biggest latitude in that row could be closer than res
            DataRow row = this.rows.get(r);
            double distRowLat = at.lat - row.latMax;
            double distRow = distRowLat * latToMeter;
            if (distRow >= distMin) {
                break;
            }

            // might be closer, so check the closest value of that row
            E e = row.get(at);
            double dist = e.getLatLon().getDistanceInMeters(at);
            if (dist < distMin) {
                distMin = dist;
                res = e;
            }
        }
        
        return res;
    }

	private void store(List<E> data) {
		int i = 0;
		DataRow currentRow = null;
		for (E e : data) {
			if (i % gridSize == 0) {
				currentRow = new DataRow(gridSize);
				this.rows.add(currentRow);
			}
			
			currentRow.add(e);
			i++;
		}
		
		for (DataRow row : this.rows) {
			row.init();
		}
	}

	public int size() {
		return this.length;
	}
	
	private E get(int idx) {
		int row = idx / this.gridSize;
		int col = idx % this.gridSize;
		return this.rows.get(row).data.get(col).data;
	}
	
	class ElemIter implements Iterator<E> {
		
		int idx = 0;

		@Override
		public boolean hasNext() {
			return idx < size();
		}

		@Override
		public E next() {
			E res = get(idx);
			idx++;
			return res;
		}
		
	}

	@Override
	public Iterator<E> iterator() {
		return new ElemIter();
	}
}
