package duplicatefinder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;

/** Löscht eine Menge von Dateien und protokolliert Erfolg/Misserfolg je Datei. */
public final class DeletionExecutor {

    public record Result(int deleted, int failed) {}

    private DeletionExecutor() {}

    /**
     * @param paths       zu löschende Dateien
     * @param labelPrefix wird an "Gelöscht" angehängt, z. B. " [Quelle]" (darf leer sein)
     * @param logger      optionaler Empfänger für Log-Zeilen (darf null sein)
     */
    public static Result delete(Collection<Path> paths, String labelPrefix, Consumer<String> logger) {
        int deleted = 0;
        int failed = 0;

        for (Path path : paths) {
            try {
                Files.delete(path);
                deleted++;
                log(logger, "Gelöscht" + labelPrefix + ": " + path);
            } catch (Exception e) {
                failed++;
                log(logger, "FEHLER: " + path.getFileName() + " – " + e.getMessage());
            }
        }
        return new Result(deleted, failed);
    }

    private static void log(Consumer<String> logger, String message) {
        if (logger != null) logger.accept(message);
    }
}
