package UnionFind;

import java.io.Serializable;

public interface UnionFind extends Serializable {

    // Creates the partition {{0}, {1}, ..., {domainSize-1}}.
    // UnionFind( int domainSize );

    // Returns the representative of the set that contains 
    // the specified element.
    int find( int element ) throws InvalidElementException;

    // Removes the two distinct sets S1 and S2 whose representatives are 
    // the specified elements, and inserts the set S1 U S2.
    // The representative of the new set S1 U S2 can be any of its members.
    void union( int representative1, int representative2 ) throws 
        InvalidElementException, NotRepresentativeException, 
        EqualSetsException;

}
