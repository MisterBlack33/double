package duplicatefinder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/** Baut Datei- und Hash-Indizes für den Ordnervergleich auf ({@link FolderComparator}). */
final class FolderFileIndexer {

    private static final int BUF = 65_536;

    private FolderFileIndexer() {}

    static Map<Path, Path> collectFiles(Path dir) throws IOException {
        Map<Path, Path> map = new LinkedHashMap<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(Files::isRegularFile)
                    .filter(abs -> !IgnoredFiles.shouldIgnore(abs))
                    .forEach(abs -> map.put(dir.relativize(abs), abs));
        }
        return map;
    }

    static Map<String, List<Path>> buildHashIndex(Collection<Path> files, BiConsumer<Integer, Integer> cb) {
        Map<String, List<Path>> index = new HashMap<>();
        int total = files.size(), done = 0;
        for (Path p : files) {
            try { index.computeIfAbsent(sha256(p), k -> new ArrayList<>()).add(p); }
            catch (IOException e) {
                System.err.printf("  Warnung: '%s' übersprungen (Hash) – %s%n", p.getFileName(), e.getMessage());
            }
            if (cb != null) cb.accept(++done, total);
        }
        return index;
    }

    /**
     * Baut pHash-Index nur für Bilddateien auf (pHash → Liste von Pfaden).
     * Animierte GIFs werden ausgeschlossen, da ihr pHash nur das erste Frame abbildet.
     */
    static Map<Long, List<Path>> buildPHashIndex(Collection<Path> files, BiConsumer<Integer, Integer> cb) {
        Map<Long, List<Path>> index = new HashMap<>();
        int total = files.size(), done = 0;
        for (Path p : files) {
            if (PerceptualHasher.isImage(p) && !PerceptualHasher.isAnimatedGif(p)) {
                try { index.computeIfAbsent(PerceptualHasher.hash(p), k -> new ArrayList<>()).add(p); }
                catch (Exception e) {
                    System.err.printf("  Warnung: '%s' übersprungen (pHash) – %s%n", p.getFileName(), e.getMessage());
                }
            }
            if (cb != null) cb.accept(++done, total);
        }
        return index;
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buf = new byte[BUF]; int n;
                while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 nicht verfügbar", e);
        }
    }

    static long safeSize(Path p) {
        try { return Files.size(p); } catch (IOException e) { return -1; }
    }
}
