package duplicatefinder;

import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Führt den Ordnervergleich im Hintergrund aus (SwingWorker) und meldet
 * Fortschritt/Ergebnis über Callbacks zurück an {@link SyncTab}.
 */
final class SyncCompareWorker extends SwingWorker<FolderSyncResult, String> {

    private final File src, tgt;
    private final boolean visualCompare;
    private final Consumer<String> onStatus;
    private final BiConsumer<Integer, Integer> onProgress;
    private final Consumer<FolderSyncResult> onSuccess;
    private final Consumer<Exception> onError;
    private final Runnable onFinished;

    SyncCompareWorker(File src, File tgt, boolean visualCompare,
                      Consumer<String> onStatus, BiConsumer<Integer, Integer> onProgress,
                      Consumer<FolderSyncResult> onSuccess, Consumer<Exception> onError, Runnable onFinished) {
        this.src = src;
        this.tgt = tgt;
        this.visualCompare = visualCompare;
        this.onStatus = onStatus;
        this.onProgress = onProgress;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onFinished = onFinished;
    }

    @Override protected FolderSyncResult doInBackground() throws Exception {
        publish(visualCompare ? "SHA-256 + pHash-Index wird aufgebaut …" : "SHA-256-Index wird aufgebaut …");
        return new FolderComparator().compare(src.toPath(), tgt.toPath(), visualCompare,
                (done, total) -> SwingUtilities.invokeLater(() -> {
                    onProgress.accept(done, total);
                    publish("Analysiere " + done + " / " + total + " …");
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
