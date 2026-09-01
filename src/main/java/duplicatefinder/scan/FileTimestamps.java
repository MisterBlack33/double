package duplicatefinder.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Liest den Änderungszeitpunkt einer Datei für die Original/Duplikat-Sortierung. */
public final class FileTimestamps {

    private FileTimestamps() {}

    /** Nicht lesbare Dateien gelten als "neuestmöglich", damit sie nie fälschlich als Original zählen. */
    public static long lastModifiedMillis(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}