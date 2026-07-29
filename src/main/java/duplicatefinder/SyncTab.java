package duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static duplicatefinder.DuplicateFinderGUI.*;

/**
 * Tab 2: Zwei Ordner vergleichen – byte-basiert + optionaler pHash-Bildvergleich.
 * UI-Aufbau siehe {@link SyncTabUi}, Tabellen-Formatierung siehe {@link SyncTabFormatting},
 * Hintergrund-Vergleich siehe {@link SyncCompareWorker}.
 */
public class SyncTab extends JPanel {

    private final DuplicateFinderGUI root;

    private FolderSyncResult lastResult;
    private final Set<Path>  markedSource = new LinkedHashSet<>();
    private final Set<Path>  markedTarget = new LinkedHashSet<>();

    private DropZonePanel     dropSource, dropTarget;
    private JCheckBox         cbVisual;
    private JLabel            lblSummary;
    private JTable            resultTable;
    private DefaultTableModel resultModel;
    private final FilePreviewPanel previewPanel = new FilePreviewPanel();
    private SyncTabUi.Footer footer;

    public SyncTab(DuplicateFinderGUI root) {
        this.root = root;
        setBackground(BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(14, 0, 0, 0));
        buildUI();
    }

    private void buildUI() {
        var dropRow = SyncTabUi.buildDropRow(root,
                f -> onFolderPicked(true, f),
                f -> onFolderPicked(false, f));
        dropSource = dropRow.source();
        dropTarget = dropRow.target();
        add(dropRow.panel(), BorderLayout.NORTH);

        add(buildCenter(), BorderLayout.CENTER);

        footer = SyncTabUi.buildFooter();
        wireFooterActions();
        add(footer.panel(), BorderLayout.SOUTH);
    }

    private void onFolderPicked(boolean isSource, File folder) {
        root.log((isSource ? "Quelle: " : "Ziel:   ") + folder.getAbsolutePath());
        footer.compare().setEnabled(dropSource.getFolder() != null && dropTarget.getFolder() != null);
    }

    private void wireFooterActions() {
        footer.clear().addActionListener(e             -> resetAll());
        footer.compare().addActionListener(e           -> startCompare());
        footer.markAllDuplicates().addActionListener(e -> markAllDuplicates());
        footer.deleteMarked().addActionListener(e      -> deleteMarked());
    }

    private JPanel buildCenter() {
        resultModel = SyncTabFormatting.buildResultModel();
        resultTable = new JTable(resultModel);
        Ui.styleTable(resultTable);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setRowHeight(34);
        SyncTabFormatting.applyColumnLayout(resultTable);
        resultTable.getColumnModel().getColumn(2).setCellRenderer(SyncTabFormatting.statusRenderer());

        resultModel.addTableModelListener(e -> {
            if (e.getColumn() == 0 || e.getColumn() == 1) syncMarkedSets();
        });
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });

        JPanel opts = Ui.panel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        cbVisual = new JCheckBox("Visuellen Bildvergleich aktivieren (pHash, langsamer)");
        cbVisual.setBackground(BG);
        cbVisual.setForeground(MUTED);
        cbVisual.setFont(FONT_UI);
        opts.add(cbVisual);

        lblSummary = Ui.label("", FONT_MONO, MUTED);
        JPanel topBar = Ui.panel(new BorderLayout());
        topBar.add(opts, BorderLayout.WEST);
        topBar.add(SyncTabFormatting.buildLegend(), BorderLayout.EAST);

        JPanel tableWrap = Ui.panel(new BorderLayout(10, 0));
        tableWrap.add(Ui.scrollPane(resultTable), BorderLayout.CENTER);
        previewPanel.setPreferredSize(new Dimension(280, 0));
        tableWrap.add(previewPanel, BorderLayout.EAST);

        JPanel center = Ui.panel(new BorderLayout(0, 6));
        center.add(topBar,     BorderLayout.NORTH);
        center.add(tableWrap,  BorderLayout.CENTER);
        center.add(lblSummary, BorderLayout.SOUTH);
        return center;
    }

    // ── Vergleich ─────────────────────────────────────────────────────────────

    private void startCompare() {
        resultModel.setRowCount(0);
        lastResult = null;
        markedSource.clear(); markedTarget.clear();
        updateMarkedLabel();
        previewPanel.preview(null);
        footer.compare().setEnabled(false);
        footer.markAllDuplicates().setEnabled(false);
        footer.deleteMarked().setEnabled(false);
        footer.progress().setValue(0);
        footer.progress().setIndeterminate(true);
        dropSource.setScanning(true);
        lblSummary.setText("");

        File src = dropSource.getFolder();
        File tgt = dropTarget.getFolder();
        boolean vis = cbVisual.isSelected();
        root.log("Vergleich: " + src.getName() + " ↔ " + tgt.getName() + (vis ? " [+ pHash]" : ""));

        new SyncCompareWorker(src, tgt, vis,
                this::setStatus,
                (done, total) -> {
                    footer.progress().setIndeterminate(false);
                    footer.progress().setValue((int) (done * 100.0 / Math.max(total, 1)));
                },
                this::onCompareSuccess,
                this::onCompareError,
                this::onCompareFinished
        ).execute();
    }

    private void onCompareFinished() {
        dropSource.setScanning(false);
        footer.progress().setIndeterminate(false);
        footer.progress().setValue(100);
        footer.compare().setEnabled(true);
    }

    private void onCompareSuccess(FolderSyncResult result) {
        lastResult = result;
        populateTable();
    }

    private void onCompareError(Exception ex) {
        setStatus("Fehler: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
    }

    private void populateTable() {
        long dupes    = lastResult.countByStatus(FolderSyncResult.MatchStatus.DUPLICATE);
        long review   = lastResult.countByStatus(FolderSyncResult.MatchStatus.NEEDS_REVIEW);
        long conflict = lastResult.countByStatus(FolderSyncResult.MatchStatus.CONFLICT);
        long visTotal = lastResult.countVisual();

        for (FolderSyncResult.FileEntry fe : lastResult.getEntries()) {
            Path srcRel = dropSource.getFolder().toPath().relativize(fe.getSourcePath());
            Path tgtRel = dropTarget.getFolder().toPath().relativize(fe.getTargetPath());
            String hamming = fe.getHammingDistance() >= 0 ? fe.getHammingDistance() + "/64" : "";

            resultModel.addRow(new Object[]{
                    fe.isDuplicate(), false,
                    SyncTabFormatting.statusLabel(fe.getStatus()),
                    SyncTabFormatting.caseLabel(fe),
                    srcRel.toString(), tgtRel.toString(),
                    Ui.fmtSize(fe.getSourceSize()), Ui.fmtSize(fe.getTargetSize()), hamming
            });
        }

        setStatus(String.format(
                "✓ Fertig – %d Duplikate · %d zu prüfen · %d Konflikte · %d visuelle Treffer · %d ignoriert",
                dupes, review, conflict, visTotal, lastResult.getDifferentCount()));
        lblSummary.setText(String.format(
                "Duplikate: %d   |   Zu prüfen: %d   |   Konflikte: %d   |   Visuell: %d   |   Ignoriert: %d",
                dupes, review, conflict, visTotal, lastResult.getDifferentCount()));

        root.log(lblSummary.getText());
        footer.markAllDuplicates().setEnabled(dupes > 0);
        syncMarkedSets();
    }

    /** Zeigt die Quelldatei der aktuell selektierten Zeile in der Vorschau. */
    private void onRowSelected() {
        int viewRow = resultTable.getSelectedRow();
        if (viewRow < 0 || lastResult == null) {
            previewPanel.preview(null);
            return;
        }
        int modelRow = resultTable.convertRowIndexToModel(viewRow);
        List<FolderSyncResult.FileEntry> entries = lastResult.getEntries();
        if (modelRow >= entries.size()) return;
        previewPanel.preview(entries.get(modelRow).getSourcePath());
    }

    // ── Checkbox-Synchronisation ──────────────────────────────────────────────

    private void syncMarkedSets() {
        markedSource.clear(); markedTarget.clear();
        if (lastResult == null) return;
        List<FolderSyncResult.FileEntry> entries = lastResult.getEntries();
        for (int i = 0; i < resultModel.getRowCount() && i < entries.size(); i++) {
            int modelRow = resultTable.convertRowIndexToModel(i);
            if (modelRow >= entries.size()) continue;
            FolderSyncResult.FileEntry fe = entries.get(modelRow);
            if ((Boolean) resultModel.getValueAt(i, 0)) markedSource.add(fe.getSourcePath());
            if ((Boolean) resultModel.getValueAt(i, 1)) markedTarget.add(fe.getTargetPath());
        }
        updateMarkedLabel();
    }

    private void updateMarkedLabel() {
        int n = markedSource.size() + markedTarget.size();
        footer.marked().setText(n == 0 ? "" :
                markedSource.size() + " Quelldatei(en) + " + markedTarget.size() + " Zieldatei(en) markiert");
        footer.deleteMarked().setText("Markierte löschen (" + n + ")");
        footer.deleteMarked().setEnabled(n > 0);
    }

    private void markAllDuplicates() {
        if (lastResult == null) return;
        List<FolderSyncResult.FileEntry> entries = lastResult.getEntries();
        for (int i = 0; i < resultModel.getRowCount(); i++) {
            int modelRow = resultTable.convertRowIndexToModel(i);
            if (modelRow >= entries.size()) continue;
            FolderSyncResult.FileEntry fe = entries.get(modelRow);
            resultModel.setValueAt(fe.isDuplicate(), i, 0);
            resultModel.setValueAt(false,            i, 1);
        }
        syncMarkedSets();
    }

    // ── Löschen ───────────────────────────────────────────────────────────────

    private void deleteMarked() {
        syncMarkedSets();

        boolean confirmed = DeletionConfirmationDialog.confirm(root, List.of(
                new DeletionConfirmationDialog.Group("[Quelle] ", markedSource),
                new DeletionConfirmationDialog.Group("[Ziel]   ", markedTarget)));
        if (!confirmed) return;

        DeletionExecutor.Result srcResult = DeletionExecutor.delete(markedSource, " [Quelle]", root::log);
        DeletionExecutor.Result tgtResult = DeletionExecutor.delete(markedTarget, " [Ziel]", root::log);
        int deleted = srcResult.deleted() + tgtResult.deleted();
        int failed  = srcResult.failed()  + tgtResult.failed();

        String msg = deleted + " Datei(en) gelöscht" + (failed > 0 ? ", " + failed + " Fehler" : "") + ".";
        root.log(msg);
        JOptionPane.showMessageDialog(root, msg, "Abgeschlossen", JOptionPane.INFORMATION_MESSAGE);
        startCompare();
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    private void resetAll() {
        lastResult = null;
        resultModel.setRowCount(0);
        markedSource.clear(); markedTarget.clear();
        updateMarkedLabel();
        previewPanel.preview(null);
        lblSummary.setText("");
        footer.progress().setValue(0);
        footer.compare().setEnabled(false);
        footer.markAllDuplicates().setEnabled(false);
        footer.deleteMarked().setEnabled(false);
        setStatus("Quell- und Zielordner auswählen.");
        dropSource.reset(); dropTarget.reset();
    }

    private void setStatus(String msg) { footer.status().setText(msg); }
}
