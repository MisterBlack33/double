package duplicatefinder.delete;

import duplicatefinder.scan.CorruptedFileScanner;

import java.awt.Component;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** Sucht nicht lesbare Dateien und löscht sie nach Bestätigung – Schritt vor jedem Inhaltsvergleich. */
public final class CorruptedFileCleaner {

    private CorruptedFileCleaner() {}

    /** Synchron: Suche + Bestätigung + Löschung in einem Aufruf (z. B. für CLI). */
    public static int cleanUnreadable(Component parent, Collection<Path> files, Consumer<String> logger) {
        List<Path> unreadable = CorruptedFileScanner.findUnreadable(files);
        return deleteWithConfirmation(parent, unreadable, logger);
    }

    /** Nur Bestätigung + Löschung einer bereits ermittelten Liste (für GUI-Worker, muss auf dem EDT laufen). */
    public static int deleteWithConfirmation(Component parent, List<Path> unreadable, Consumer<String> logger) {
        if (unreadable.isEmpty()) return 0;

        boolean confirmed = DeletionConfirmationDialog.confirm(parent,
                List.of(new DeletionConfirmationDialog.Group("[Korrupt] ", unreadable)));
        if (!confirmed) return 0;

        return DeletionExecutor.delete(unreadable, " [korrupt/nicht lesbar]", logger).deleted();
    }
}