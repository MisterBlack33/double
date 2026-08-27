package duplicatefinder.match;

import duplicatefinder.audio.AudioFingerprinter;
import duplicatefinder.scan.AudioDuplicateGroup;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Findet Audiodateien mit demselben Song, aber unterschiedlichem Byte-Inhalt (Format/Bitrate) –
 * unabhängig vom Dateinamen. Pitch-/Tempo-veränderte Versionen (z. B. Nightcore) gelten NICHT
 * als Duplikat, da ihr Lautstärke-Profil deutlich abweicht, auch bei ähnlichem Dateinamen.
 */
public final class AudioDuplicateDetector {

    private static final double MAX_PROFILE_DISTANCE = 0.08;

    private AudioDuplicateDetector() {}

    public static List<AudioDuplicateGroup> detect(Collection<Path> files,
                                                   BiConsumer<Integer, Integer> progressCallback) {
        List<Fingerprint> fingerprints = buildFingerprints(files, progressCallback);
        UnionFind clusters = clusterBySimilarity(fingerprints);
        return buildGroups(fingerprints, clusters);
    }

    private record Fingerprint(Path path, double[] profile) {}

    private static List<Fingerprint> buildFingerprints(Collection<Path> files,
                                                       BiConsumer<Integer, Integer> cb) {
        List<Fingerprint> result = new ArrayList<>();
        int total = files.size(), done = 0;
        for (Path p : files) {
            if (FileKindClassifier.classify(p) == FileKind.AUDIO) tryAdd(p, result);
            if (cb != null) cb.accept(++done, total);
        }
        return result;
    }

    private static void tryAdd(Path p, List<Fingerprint> out) {
        try {
            out.add(new Fingerprint(p, AudioFingerprinter.fingerprint(p)));
        } catch (Exception e) {
            System.err.printf("  Warnung: '%s' übersprungen (Audio-Vergleich) – %s%n",
                    p.getFileName(), e.getMessage());
        }
    }

    private static UnionFind clusterBySimilarity(List<Fingerprint> fingerprints) {
        UnionFind uf = new UnionFind(fingerprints.size());
        for (int i = 0; i < fingerprints.size(); i++) {
            for (int j = i + 1; j < fingerprints.size(); j++) {
                double dist = AudioFingerprinter.distance(fingerprints.get(i).profile(), fingerprints.get(j).profile());
                if (dist <= MAX_PROFILE_DISTANCE) uf.union(i, j);
            }
        }
        return uf;
    }

    private static List<AudioDuplicateGroup> buildGroups(List<Fingerprint> fingerprints, UnionFind uf) {
        Map<Integer, List<Fingerprint>> clusters = new LinkedHashMap<>();
        for (int i = 0; i < fingerprints.size(); i++) {
            clusters.computeIfAbsent(uf.find(i), k -> new ArrayList<>()).add(fingerprints.get(i));
        }
        List<AudioDuplicateGroup> groups = new ArrayList<>();
        for (List<Fingerprint> cluster : clusters.values()) {
            if (cluster.size() > 1) groups.add(toGroup(cluster));
        }
        return groups;
    }

    private static AudioDuplicateGroup toGroup(List<Fingerprint> cluster) {
        List<Path> paths = cluster.stream().map(Fingerprint::path).toList();
        return new AudioDuplicateGroup(paths, maxPairwiseDistance(cluster));
    }

    private static double maxPairwiseDistance(List<Fingerprint> cluster) {
        double max = 0;
        for (int i = 0; i < cluster.size(); i++) {
            for (int j = i + 1; j < cluster.size(); j++) {
                max = Math.max(max, AudioFingerprinter.distance(cluster.get(i).profile(), cluster.get(j).profile()));
            }
        }
        return max;
    }
}