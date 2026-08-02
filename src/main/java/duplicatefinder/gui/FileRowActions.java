package duplicatefinder.gui;

import duplicatefinder.delete.DeletionConfirmationDialog;
import duplicatefinder.delete.DeletionExecutor;
import duplicatefinder.exclude.ExclusionStore;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wiederverwendbare Zeilen-Aktionen für interaktive Datei-Listen-Dialoge
 * (Namenskollisionen, visuelle Duplikate): Öffnen, Einzeln löschen, Als verschieden markieren.
 */
public final class FileRowActions {

    private FileRowActions() {}

    public static void open(Component parent, Path file) {
        if (!SystemFileOpener.open(file)) {
            JOptionPane.showMessageDialog(parent, "Datei konnte nicht geöffnet werden:\n" + file,
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** @return true, wenn die Datei nach Bestätigung tatsächlich gelöscht wurde */
    public static boolean deleteSingle(Component parent, Path file, Consumer<String> logger) {
        boolean confirmed = DeletionConfirmationDialog.confirm(parent,
                List.of(new DeletionConfirmationDialog.Group("", List.of(file))));
        if (!confirmed) return false;

        return DeletionExecutor.delete(List.of(file), "", logger).deleted() > 0;
    }

    /** Markiert die Datei gegenüber allen übrigen Gruppenmitgliedern dauerhaft als "verschieden". */
    public static void markDifferentFromRest(Path file, List<Path> restOfGroup, ExclusionStore exclusions) {
        for (Path other : restOfGroup) {
            if (!other.equals(file)) exclusions.exclude(file, other);
        }
    }
}