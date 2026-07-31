package duplicatefinder.scan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Erkennt Dateien mit gleichem (oder nur durch Kopie-Suffix abweichendem) Namen,
 * die unterschiedlichen Inhalt haben – unabhängig davon, in welchem Unterordner sie liegen.
 *
 * <p>Ergänzt {@link DuplicateDetector}: Während dieser inhaltsgleiche Dateien gruppiert,
 * meldet dieser Detector den umgekehrten Fall (z. B. zwei verschiedene Fotos, die beide
 * "x.jpg" heißen, oder "x.jpg" vs. "x (1).jpg" mit unterschiedlichem Inhalt).
 */
public final class NameCollisionDetector {

    private static final Pattern COPY_SUFFIX = Pattern.compile(
            "^(.+?)(?:\\s\\(\\d+\\)|\\s-\\s[Kk]opie(?:\\s\\(\\d+\\))?|\\s[Cc]opy(?:\\s\\(\\d+\\))?)(\\.[^.]+)$");
    private static final int BUFFER_SIZE = 65_536;

    private NameCollisionDetector() {}

    public static List<NameCollisionGroup> detect(List<Path> files) {
        Map<String, List<Path>> byNormalizedName = groupByNormalizedName(files);
        List<NameCollisionGroup> collisions = new ArrayList<>();

        for (Map.Entry<String, List<Path>> group : byNormalizedName.entrySet()) {
            if (group.getValue().size() < 2) continue;
            NameCollisionGroup collision = buildIfConflicting(group.getKey(), group.getValue());
            if (collision != null) collisions.add(collision);
        }
        return collisions;
    }

    private static Map<String, List<Path>> groupByNormalizedName(List<Path> files) {
        Map<String, List<Path>> byName = new LinkedHashMap<>();
        for (Path file : files) {
            if (IgnoredFiles.shouldIgnore(file)) continue;
            byName.computeIfAbsent(normalize(file), k -> new ArrayList<>()).add(file);
        }
        return byName;
    }

    /** Gibt eine Gruppe nur zurück, wenn mindestens zwei unterschiedliche Inhalte vorkommen. */
    private static NameCollisionGroup buildIfConflicting(String normalizedName, List<Path> paths) {
        Set<String> distinctHashes = new HashSet<>();
        for (Path file : paths) {
            try {
                distinctHashes.add(sha256(file));
            } catch (IOException e) {
                System.err.printf("  Warnung: '%s' übersprungen (Namenskollision) – %s%n",
                        file.getFileName(), e.getMessage());
            }
        }
        return distinctHashes.size() > 1 ? new NameCollisionGroup(normalizedName, paths) : null;
    }

    private static String normalize(Path file) {
        String name = file.getFileName().toString();
        Matcher matcher = COPY_SUFFIX.matcher(name);
        return matcher.matches() ? matcher.group(1) + matcher.group(2) : name;
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 nicht verfügbar", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}