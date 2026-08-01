package duplicatefinder.match;

/** Minimale Union-Find-Struktur zum Gruppieren visuell ähnlicher Bilder. */
final class UnionFind {

    private final int[] parent;

    UnionFind(int size) {
        parent = new int[size];
        for (int i = 0; i < size; i++) parent[i] = i;
    }

    int find(int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    void union(int a, int b) {
        int rootA = find(a);
        int rootB = find(b);
        if (rootA != rootB) parent[rootA] = rootB;
    }
}