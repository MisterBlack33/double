package duplicatefinder.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Liest den Änderungszeitpunkt einer Datei für die Original/Duplikat-Sortierung. */
final class FileTimestamps {

    private FileTimestamps() {}

    /** Nicht lesbare Dateien gelten als "neuestmöglich", damit sie nie fälschlich als Original zählen. */
    static long lastModifiedMillis(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}