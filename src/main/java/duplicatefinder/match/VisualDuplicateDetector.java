package duplicatefinder.match;

import duplicatefinder.hash.PerceptualHasher;
import duplicatefinder.scan.VisualDuplicateGroup;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Findet visuell (fast) identische Bilder innerhalb EINES Scans – unabhängig von
 * Dateiname und Byte-Inhalt (z. B. dasselbe Foto einmal als JPG, einmal als PNG).
 *
 * <p>Ergänzt {@link duplicatefinder.scan.DuplicateDetector}, der nur byte-identische
 * Dateien erkennt. Nutzt dieselben pHash-Schwellen wie der Ordnervergleich
 * ({@link duplicatefinder.folder.FolderVisualMatcher}), gruppiert Treffer aber
 * transitiv über {@link UnionFind}, da mehr als zwei Dateien betroffen sein können.
 */
public final class VisualDuplicateDetector {

    /** Maximale Hamming-Distanz für einen visuellen Treffer (siehe {@link PerceptualHasher}). */
    private static final int MAX_HAMMING_DISTANCE = 10;

    private VisualDuplicateDetector() {}

    public static List<VisualDuplicateGroup> detect(Collection<Path> images,
                                                    BiConsumer<Integer, Integer> progressCallback) {
        List<VisualFingerprint> fingerprints = buildFingerprints(images, progressCallback);
        UnionFind clusters = clusterBySimilarity(fingerprints);
        return buildGroups(fingerprints, clusters);
    }

    private static List<VisualFingerprint> buildFingerprints(Collection<Path> images,
                                                             BiConsumer<Integer, Integer> cb) {
        List<VisualFingerprint> result = new ArrayList<>();
        int total = images.size(), done = 0;
        for (Path p : images) {
            if (PerceptualHasher.isImage(p) && !PerceptualHasher.isAnimatedGif(p)) {
                tryAddFingerprint(p, result);
            }
            if (cb != null) cb.accept(++done, total);
        }
        return result;
    }

    private static void tryAddFingerprint(Path p, List<VisualFingerprint> out) {
        try {
            int[] dim = PerceptualHasher.readDimensions(p);
            out.add(new VisualFingerprint(p, PerceptualHasher.hash(p), dim[0], dim[1],
                    PerceptualHasher.histogram(p)));
        } catch (Exception e) {
            System.err.printf("  Warnung: '%s' übersprungen (visueller Vergleich) – %s%n",
                    p.getFileName(), e.getMessage());
        }
    }

    private static UnionFind clusterBySimilarity(List<VisualFingerprint> fingerprints) {
        UnionFind uf = new UnionFind(fingerprints.size());
        for (int i = 0; i < fingerprints.size(); i++) {
            for (int j = i + 1; j < fingerprints.size(); j++) {
                if (areSimilar(fingerprints.get(i), fingerprints.get(j))) uf.union(i, j);
            }
        }
        return uf;
    }

    /** Drei Gates müssen greifen: Seitenverhältnis, pHash-Distanz, Helligkeitshistogramm. */
    private static boolean areSimilar(VisualFingerprint a, VisualFingerprint b) {
        if (!PerceptualHasher.aspectRatioCompatible(a.width(), a.height(), b.width(), b.height())) return false;
        if (PerceptualHasher.hammingDistance(a.pHash(), b.pHash()) > MAX_HAMMING_DISTANCE) return false;
        return PerceptualHasher.histogramsPlausiblySimilar(a.histogram(), b.histogram());
    }

    private static List<VisualDuplicateGroup> buildGroups(List<VisualFingerprint> fingerprints, UnionFind uf) {
        Map<Integer, List<VisualFingerprint>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < fingerprints.size(); i++) {
            clusters.computeIfAbsent(uf.find(i), k -> new ArrayList<>()).add(fingerprints.get(i));
        }
        List<VisualDuplicateGroup> groups = new ArrayList<>();
        for (List<VisualFingerprint> cluster : clusters.values()) {
            if (cluster.size() > 1) groups.add(toGroup(cluster));
        }
        return groups;
    }

    private static VisualDuplicateGroup toGroup(List<VisualFingerprint> cluster) {
        List<Path> paths = cluster.stream().map(VisualFingerprint::path).toList();
        return new VisualDuplicateGroup(paths, maxPairwiseDistance(cluster));
    }

    private static int maxPairwiseDistance(List<VisualFingerprint> cluster) {
        int max = 0;
        for (int i = 0; i < cluster.size(); i++) {
            for (int j = i + 1; j < cluster.size(); j++) {
                max = Math.max(max, PerceptualHasher.hammingDistance(cluster.get(i).pHash(), cluster.get(j).pHash()));
            }
        }
        return max;
    }
}