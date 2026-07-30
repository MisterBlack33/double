package duplicatefinder.gui.duplicatetab;

import duplicatefinder.delete.DeletionConfirmationDialog;
import duplicatefinder.delete.DeletionExecutor;
import duplicatefinder.report.DuplicateResultExporter;
import duplicatefinder.scan.ScanResult;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Kapselt Export- und Lösch-Dialoge von {@link DuplicateTab}, damit dieser schlank bleibt. */
final class DuplicateTabFileOps {

    private DuplicateTabFileOps() {}

    static void exportResults(Component parent, ScanResult result, Consumer<String> logger) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Ergebnisse exportieren");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Text (*.txt)", "txt"));
        fc.setFileFilter(fc.getChoosableFileFilters()[1]);
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;

        try {
            File out = DuplicateResultExporter.export(result, fc.getSelectedFile(), fc.getFileFilter().getDescription());
            logger.accept("Exportiert: " + out.getAbsolutePath());
            JOptionPane.showMessageDialog(parent, "Exportiert:\n" + out.getAbsolutePath(),
                    "Export erfolgreich", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Fehler: " + ex.getMessage(),
                    "Export fehlgeschlagen", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** @return true, wenn tatsächlich gelöscht wurde (Nutzer hat bestätigt und Auswahl war nicht leer) */
    static boolean deleteMarked(Component parent, Set<Path> marked, Consumer<String> logger) {
        if (marked.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Keine Dateien markiert.",
                    "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        boolean confirmed = DeletionConfirmationDialog.confirm(parent,
                List.of(new DeletionConfirmationDialog.Group("", marked)));
        if (!confirmed) return false;

        DeletionExecutor.Result result = DeletionExecutor.delete(marked, "", logger);
        String summary = result.deleted() + " Datei(en) gelöscht"
                + (result.failed() > 0 ? ", " + result.failed() + " fehlgeschlagen" : "") + ".";
        logger.accept(summary);
        JOptionPane.showMessageDialog(parent, summary, "Löschen abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }
}
