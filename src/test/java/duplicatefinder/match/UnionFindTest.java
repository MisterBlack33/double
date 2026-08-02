package duplicatefinder.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UnionFindTest {

    @Test
    void elementsStartInSeparateSets() {
        UnionFind uf = new UnionFind(3);
        assertNotEquals(uf.find(0), uf.find(1));
    }

    @Test
    void unionMergesTwoSets() {
        UnionFind uf = new UnionFind(3);
        uf.union(0, 1);
        assertEquals(uf.find(0), uf.find(1));
    }

    @Test
    void unionIsTransitive() {
        UnionFind uf = new UnionFind(4);
        uf.union(0, 1);
        uf.union(1, 2);
        assertEquals(uf.find(0), uf.find(2));
        assertNotEquals(uf.find(0), uf.find(3));
    }

    @Test
    void unionOfAlreadyMergedElementsIsNoOp() {
        UnionFind uf = new UnionFind(2);
        uf.union(0, 1);
        uf.union(0, 1);
        assertEquals(uf.find(0), uf.find(1));
    }
}