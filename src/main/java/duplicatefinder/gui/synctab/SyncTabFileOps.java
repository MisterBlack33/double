package duplicatefinder.gui.synctab;

import duplicatefinder.delete.DeletionConfirmationDialog;
import duplicatefinder.delete.DeletionExecutor;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Kapselt den Lösch-Dialog von {@link SyncTab} (Quelle + Ziel), damit dieser schlank bleibt. */
final class SyncTabFileOps {

    private SyncTabFileOps() {}

    /** @return true, wenn tatsächlich gelöscht wurde */
    static boolean deleteMarked(Component parent, Set<Path> markedSource, Set<Path> markedTarget,
                                Consumer<String> logger) {
        boolean confirmed = DeletionConfirmationDialog.confirm(parent, List.of(
                new DeletionConfirmationDialog.Group("[Quelle] ", markedSource),
                new DeletionConfirmationDialog.Group("[Ziel]   ", markedTarget)));
        if (!confirmed) return false;

        DeletionExecutor.Result srcResult = DeletionExecutor.delete(markedSource, " [Quelle]", logger);
        DeletionExecutor.Result tgtResult = DeletionExecutor.delete(markedTarget, " [Ziel]", logger);
        int deleted = srcResult.deleted() + tgtResult.deleted();
        int failed  = srcResult.failed()  + tgtResult.failed();

        String msg = deleted + " Datei(en) gelöscht" + (failed > 0 ? ", " + failed + " Fehler" : "") + ".";
        logger.accept(msg);
        JOptionPane.showMessageDialog(parent, msg, "Abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }
}
