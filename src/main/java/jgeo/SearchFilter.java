package jgeo;

public interface SearchFilter {
    boolean matches(LocationObject location);

    int getMaxResults();
}
