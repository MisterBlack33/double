package duplicatefinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Zentrale Filterregel: welche Dateien werden nie als Duplikat gemeldet,
 * weil sie strukturell irrelevant sind oder zu falschen Treffern führen.
 */
public final class IgnoredFiles {

    private static final Set<String> IGNORED_NAMES = Set.of(
            "thumbs.db", "ehthumbs.db", "desktop.ini", ".ds_store", ".directory"
    );

    private IgnoredFiles() {}

    public static boolean shouldIgnore(Path file) {
        return isIgnoredSystemFile(file) || isEmptyFile(file) || isSymbolicLink(file);
    }

    public static boolean isIgnoredSystemFile(Path file) {
        return IGNORED_NAMES.contains(file.getFileName().toString().toLowerCase());
    }

    /** 0-Byte-Dateien haben alle denselben SHA-256 – kein echtes Duplikat. */
    public static boolean isEmptyFile(Path file) {
        try { return Files.size(file) == 0; }
        catch (Exception e) { return false; }
    }

    /** Zwei Symlinks auf dieselbe Datei sind keine zwei Duplikate. */
    public static boolean isSymbolicLink(Path file) {
        return Files.isSymbolicLink(file);
    }
}