package duplicatefinder;

import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Führt den Duplikat-Scan eines Ordners im Hintergrund aus (SwingWorker) und meldet
 * Fortschritt/Ergebnis über Callbacks zurück an {@link DuplicateTab}.
 */
final class DuplicateScanWorker extends SwingWorker<ScanResult, String> {

    private final File folder;
    private final Consumer<String> onStatus;
    private final BiConsumer<Integer, Integer> onProgress;
    private final Consumer<ScanResult> onSuccess;
    private final Consumer<Exception> onError;
    private final Runnable onFinished;

    DuplicateScanWorker(File folder, Consumer<String> onStatus, BiConsumer<Integer, Integer> onProgress,
                        Consumer<ScanResult> onSuccess, Consumer<Exception> onError, Runnable onFinished) {
        this.folder = folder;
        this.onStatus = onStatus;
        this.onProgress = onProgress;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onFinished = onFinished;
    }

    @Override protected ScanResult doInBackground() throws Exception {
        publish("Lese Verzeichnis …");
        List<Path> files = new FileScanner().scan(folder.toPath());
        publish("Analysiere " + files.size() + " Dateien …");
        return new DuplicateDetector().findDuplicates(files, (done, total) ->
                SwingUtilities.invokeLater(() -> {
                    onProgress.accept(done, total);
                    if (done % 30 == 0) publish("Hashing " + done + "/" + total + " …");
                }));
    }

    @Override protected void process(List<String> chunks) {
        onStatus.accept(chunks.get(chunks.size() - 1));
    }

    @Override protected void done() {
        onFinished.run();
        try {
            onSuccess.accept(get());
        } catch (Exception ex) {
            onError.accept(ex);
        }
    }
}
