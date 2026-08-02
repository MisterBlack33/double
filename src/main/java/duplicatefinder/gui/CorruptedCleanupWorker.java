package duplicatefinder.gui;

import duplicatefinder.delete.CorruptedFileCleaner;
import duplicatefinder.scan.CorruptedFileScanner;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Sucht im Hintergrund nicht lesbare Dateien und löscht sie (nach Bestätigung),
 * bevor ein eigentlicher Duplikat-/Ordnervergleich startet. Meldet Fortschritt via {@code onStatus}.
 */
public final class CorruptedCleanupWorker extends SwingWorker<List<Path>, Void> {

    private final Component parent;
    private final Collection<Path> files;
    private final Consumer<String> onStatus;
    private final IntConsumer onFinished;

    public CorruptedCleanupWorker(Component parent, Collection<Path> files,
                                  Consumer<String> onStatus, IntConsumer onFinished) {
        this.parent = parent;
        this.files = files;
        this.onStatus = onStatus;
        this.onFinished = onFinished;
    }

    @Override protected List<Path> doInBackground() {
        onStatus.accept("Suche nicht lesbare Dateien …");
        return CorruptedFileScanner.findUnreadable(files);
    }

    @Override protected void done() {
        List<Path> unreadable = readResultSafely();

        if (unreadable.isEmpty()) {
            onStatus.accept("Keine nicht lesbaren Dateien gefunden.");
            onFinished.accept(0);
            return;
        }

        onStatus.accept(unreadable.size() + " nicht lesbare Datei(en) gefunden – Bestätigung ausstehend …");
        int deleted = CorruptedFileCleaner.deleteWithConfirmation(parent, unreadable, null);

        onStatus.accept(deleted > 0
                ? deleted + " nicht lesbare Datei(en) gelöscht."
                : unreadable.size() + " nicht lesbare Datei(en) gefunden, Löschung abgebrochen.");
        onFinished.accept(deleted);
    }

    private List<Path> readResultSafely() {
        try { return get(); } catch (Exception e) { return List.of(); }
    }
}