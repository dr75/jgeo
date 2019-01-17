package jgeo;

import java.util.Collection;

public class SearchResult<T extends LocationObject> {

    final public Collection<T> data;
    final public int totalHits;

    public SearchResult(Collection<T> data, int totalHits) {
        this.data = data;
        this.totalHits = totalHits;
    }
}
