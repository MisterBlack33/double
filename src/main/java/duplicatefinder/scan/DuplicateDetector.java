package duplicatefinder.scan;

import duplicatefinder.exclude.ExclusionStore;
import duplicatefinder.exclude.NoOpExclusionStore;
import duplicatefinder.match.VisualDuplicateDetector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Erkennt Byte-Duplikate (dreistufiges SHA-256-Verfahren) sowie ergänzend
 * visuell (fast) identische Bilder mit unterschiedlichem Byte-Inhalt.
 */
public class DuplicateDetector {

    private static final int BUFFER_SIZE = 65_536;
    private static final int QUICK_BYTES = 8_192;

    public ScanResult findDuplicates(List<Path> files, BiConsumer<Integer, Integer> progressCallback,
                                     ExclusionStore exclusions) throws IOException {
        List<NameCollisionGroup> nameCollisions = NameCollisionDetector.detect(files, exclusions);
        List<ScanResult.DuplicateGroup> byteGroups = detectByteDuplicates(files, progressCallback);
        List<VisualDuplicateGroup> visualDuplicates = detectVisualDuplicates(files, byteGroups);

        return new ScanResult(byteGroups, files.size(), nameCollisions, visualDuplicates);
    }

    public ScanResult findDuplicates(List<Path> files, BiConsumer<Integer, Integer> progressCallback)
            throws IOException {
        return findDuplicates(files, progressCallback, NoOpExclusionStore.INSTANCE);
    }

    public ScanResult findDuplicates(List<Path> files) throws IOException {
        return findDuplicates(files, null, NoOpExclusionStore.INSTANCE);
    }

    private List<ScanResult.DuplicateGroup> detectByteDuplicates(
            List<Path> files, BiConsumer<Integer, Integer> progressCallback) {
        List<Path> candidates = filterBySize(files);
        if (candidates.isEmpty()) return Collections.emptyList();

        candidates = filterByHash(candidates, true, null, candidates.size());
        if (candidates.isEmpty()) return Collections.emptyList();

        Map<String, List<Path>> byFullHash = new LinkedHashMap<>();
        int total = candidates.size(), processed = 0;
        for (Path file : candidates) {
            try {
                String hash = computeHash(file, false);
                byFullHash.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
            } catch (IOException e) {
                System.err.printf("  Warnung: '%s' übersprungen – %s%n", file.getFileName(), e.getMessage());
            }
            processed++;
            if (progressCallback != null) progressCallback.accept(processed, total);
        }

        return byFullHash.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .sorted(Comparator.comparingLong(e -> -getSize(e.getValue().get(0))))
                .map(e -> new ScanResult.DuplicateGroup(e.getKey(), e.getValue(), getSize(e.getValue().get(0))))
                .collect(Collectors.toList());
    }

    /** Bilder mit gleichem Motiv, aber unterschiedlichem Byte-Inhalt; bereits als Byte-Duplikat
     *  erkannte Dateien werden ausgeschlossen, damit sie nicht doppelt gemeldet werden. */
    private List<VisualDuplicateGroup> detectVisualDuplicates(
            List<Path> files, List<ScanResult.DuplicateGroup> byteGroups) {
        Set<Path> alreadyByteMatched = byteGroups.stream()
                .flatMap(g -> g.getPaths().stream())
                .collect(Collectors.toSet());

        List<Path> remaining = files.stream()
                .filter(f -> !alreadyByteMatched.contains(f))
                .collect(Collectors.toList());

        return VisualDuplicateDetector.detect(remaining, null);
    }

    private List<Path> filterBySize(List<Path> files) {
        Map<Long, List<Path>> bySize = new HashMap<>();
        for (Path f : files) {
            if (IgnoredFiles.shouldIgnore(f)) continue;
            long size = getSize(f);
            if (size >= 0) bySize.computeIfAbsent(size, k -> new ArrayList<>()).add(f);
        }
        return bySize.values().stream()
                .filter(g -> g.size() > 1)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<Path> filterByHash(List<Path> files, boolean quick,
                                    BiConsumer<Integer, Integer> cb, int total) {
        Map<String, List<Path>> byHash = new LinkedHashMap<>();
        int n = 0;
        for (Path f : files) {
            try {
                byHash.computeIfAbsent(computeHash(f, quick), k -> new ArrayList<>()).add(f);
            } catch (IOException e) {
                System.err.printf("  Warnung: '%s' übersprungen%n", f.getFileName());
            }
            if (cb != null) cb.accept(++n, total);
        }
        return byHash.values().stream()
                .filter(g -> g.size() > 1)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private String computeHash(Path file, boolean quick) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                long remaining = quick ? QUICK_BYTES : Long.MAX_VALUE;
                int bytesRead;
                while (remaining > 0 &&
                        (bytesRead = is.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    digest.update(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 nicht verfügbar", e);
        }
    }

    private long getSize(Path file) {
        try { return Files.size(file); } catch (IOException e) { return -1; }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}