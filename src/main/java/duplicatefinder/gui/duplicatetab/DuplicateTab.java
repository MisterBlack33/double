package duplicatefinder.gui.duplicatetab;

import duplicatefinder.gui.DropZonePanel;
import duplicatefinder.gui.DuplicateFinderGUI;
import duplicatefinder.gui.FilePreviewPanel;
import duplicatefinder.gui.Ui;
import duplicatefinder.report.ResultPrinter;
import duplicatefinder.scan.ScanResult;
import duplicatefinder.scan.NameCollisionGroup;
import duplicatefinder.scan.VisualDuplicateGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static duplicatefinder.gui.UiTheme.*;

/**
 * Tab 1: Duplikate suchen, anzeigen, per Klick in der Vorschau prüfen und gruppenübergreifend löschen.
 *
 * <p>Workflow: Ordner wählen → Scan starten → Gruppen durchklicken und markieren → gesammelt löschen.
 * UI-Aufbau siehe {@link DuplicateTabUi}, Hintergrund-Scan siehe {@link DuplicateScanWorker},
 * Detailansicht siehe {@link DuplicateGroupDetailBuilder}, Export siehe {@link DuplicateResultExporter}.
 */
public class DuplicateTab extends JPanel {

    private final DuplicateFinderGUI root;

    private ScanResult      lastResult;
    private final Set<Path> markedForDeletion = new LinkedHashSet<>();
    private int             currentGroupRow   = -1;

    private DropZonePanel     dropZone;
    private JTable             groupTable;
    private DefaultTableModel  groupModel;
    private JPanel              detailPanel;
    private final JLabel        lblDetailHeader = Ui.label("  Gruppe auswählen …", FONT_BOLD, MUTED);
    private JCheckBox[]         detailCheckboxes;
    private List<Path>          currentGroupPaths;
    private final FilePreviewPanel previewPanel = new FilePreviewPanel();
    private DuplicateTabUi.Footer footer;
    private List<NameCollisionGroup>   nameCollisions   = List.of();
    private List<VisualDuplicateGroup> visualDuplicates = List.of();

    public DuplicateTab(DuplicateFinderGUI root) {
        this.root = root;
        setBackground(BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(14, 0, 0, 0));
        buildUI();
    }

    private void buildUI() {
        dropZone = new DropZonePanel(root, this::onFolderSelected);
        dropZone.setPreferredSize(new Dimension(0, 120));
        add(dropZone, BorderLayout.NORTH);

        groupModel = DuplicateTabUi.buildGroupModel();
        groupTable = DuplicateTabUi.buildGroupTable(groupModel);
        groupTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onGroupSelected(groupTable.getSelectedRow());
        });
        detailPanel = DuplicateTabUi.buildDetailSkeleton(lblDetailHeader);

        JPanel rightPanel = Ui.panel(new BorderLayout(10, 0));
        rightPanel.add(detailPanel, BorderLayout.CENTER);
        previewPanel.setPreferredSize(new Dimension(280, 0));
        rightPanel.add(previewPanel, BorderLayout.EAST);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, Ui.scrollPane(groupTable), rightPanel);
        split.setDividerLocation(500);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setBackground(BG);
        add(split, BorderLayout.CENTER);

        footer = DuplicateTabUi.buildFooter();
        wireFooterActions();
        add(footer.panel(), BorderLayout.SOUTH);
    }

    private void wireFooterActions() {
        footer.export().addActionListener(e    -> exportResults());
        footer.markAll().addActionListener(e   -> markAllDuplicates());
        footer.deleteAll().addActionListener(e -> deleteAllMarked());
        footer.clear().addActionListener(e     -> resetAll());
        footer.scan().addActionListener(e      -> startScan());
        footer.nameCollisions().addActionListener(e -> NameCollisionDialog.show(this, nameCollisions));
        footer.visualDuplicates().addActionListener(e -> VisualDuplicateDialog.show(this, visualDuplicates));
    }

    private void onFolderSelected(File folder) {
        footer.scan().setEnabled(true);
        setStatus("Ordner: " + folder.getAbsolutePath());
        root.log("Ordner gewählt: " + folder.getAbsolutePath());
    }

    private void startScan() {
        prepareScanState();
        File folder = dropZone.getFolder();
        root.log("Scan gestartet: " + folder.getAbsolutePath());

        new DuplicateScanWorker(folder,
                this::setStatus,
                (done, total) -> {
                    footer.progress().setIndeterminate(false);
                    footer.progress().setValue((int) (done * 100.0 / Math.max(total, 1)));
                },
                this::onScanSuccess,
                this::onScanError,
                this::onScanFinished
        ).execute();
    }

    private void prepareScanState() {
        groupModel.setRowCount(0);
        clearDetail();
        lastResult = null;
        markedForDeletion.clear();
        currentGroupRow = -1;
        updateMarkedLabel();
        footer.scan().setEnabled(false);
        footer.clear().setEnabled(false);
        footer.export().setEnabled(false);
        footer.markAll().setEnabled(false);
        footer.deleteAll().setEnabled(false);
        footer.progress().setValue(0);
        footer.progress().setIndeterminate(true);
        dropZone.setScanning(true);
        resetCollisionState();
        resetVisualDuplicateState();
    }

    private void resetCollisionState() {
        footer.nameCollisions().setEnabled(false);
        footer.nameCollisions().setText("Namenskollisionen (0)");
        nameCollisions = List.of();
    }

    private void resetVisualDuplicateState() {
        footer.visualDuplicates().setEnabled(false);
        footer.visualDuplicates().setText("Visuelle Duplikate (0)");
        visualDuplicates = List.of();
    }

    private void onScanFinished() {
        dropZone.setScanning(false);
        footer.progress().setIndeterminate(false);
        footer.progress().setValue(100);
        footer.scan().setEnabled(true);
        footer.clear().setEnabled(true);
    }

    private void onScanSuccess(ScanResult result) {
        lastResult = result;
        applyNameCollisions(result);
        applyVisualDuplicates(result);
        populateGroupTable();
        root.log("Scan fertig: " + result.getDuplicateGroupCount()
                + " Gruppe(n), " + result.getRedundantFileCount() + " redundante Datei(en)");
    }

    private void applyNameCollisions(ScanResult result) {
        nameCollisions = result.getNameCollisions();
        footer.nameCollisions().setText("Namenskollisionen (" + nameCollisions.size() + ")");
        footer.nameCollisions().setEnabled(!nameCollisions.isEmpty());
        if (!nameCollisions.isEmpty()) {
            root.log(nameCollisions.size() + " Namenskollision(en): gleicher Name, unterschiedlicher Inhalt");
        }
    }

    /** Bilder mit unterschiedlichem Byte-Inhalt, aber visuell (fast) identisch (pHash-Vergleich). */
    private void applyVisualDuplicates(ScanResult result) {
        visualDuplicates = result.getVisualDuplicates();
        footer.visualDuplicates().setText("Visuelle Duplikate (" + visualDuplicates.size() + ")");
        footer.visualDuplicates().setEnabled(!visualDuplicates.isEmpty());
        if (!visualDuplicates.isEmpty()) {
            root.log(visualDuplicates.size() + " visuelle Duplikat-Gruppe(n): gleiches Motiv, anderer Byte-Inhalt");
        }
    }

    private void onScanError(Exception ex) {
        setStatus("Fehler: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
        root.log("Fehler beim Scan: " + ex.getMessage());
    }

    private void populateGroupTable() {
        if (!lastResult.hasDuplicates()) {
            setStatus("✓ Keine Duplikate. " + lastResult.getTotalFilesScanned() + " Dateien geprüft.");
            return;
        }
        int g = 1;
        for (ScanResult.DuplicateGroup grp : lastResult.getGroups()) {
            groupModel.addRow(new Object[]{
                    g++, grp.getPaths().size(),
                    ResultPrinter.formatSize(grp.getFileSize()),
                    ResultPrinter.formatSize(grp.wastedBytes()),
                    grp.getHash().substring(0, 18) + "…"
            });
        }
        setStatus(String.format("✓ %d Gruppe(n) · %d redundante Datei(en) · Einsparpotenzial: %s",
                lastResult.getDuplicateGroupCount(), lastResult.getRedundantFileCount(),
                ResultPrinter.formatSize(lastResult.getTotalWastedBytes())));
        footer.export().setEnabled(true);
        footer.markAll().setEnabled(true);
        if (groupModel.getRowCount() > 0) groupTable.setRowSelectionInterval(0, 0);
    }

    private void onGroupSelected(int viewRow) {
        if (viewRow < 0 || lastResult == null) return;
        saveCurrentGroupCheckboxes();
        int modelRow = groupTable.convertRowIndexToModel(viewRow);
        currentGroupRow = modelRow;
        buildDetailView(lastResult.getGroups().get(modelRow));
    }

    private void buildDetailView(ScanResult.DuplicateGroup grp) {
        currentGroupPaths = grp.getPaths();
        detailCheckboxes  = new JCheckBox[currentGroupPaths.size()];
        previewPanel.preview(null);

        lblDetailHeader.setText(String.format("  Gruppe: %d Dateien · %s · Einsparpotenzial: %s",
                currentGroupPaths.size(), ResultPrinter.formatSize(grp.getFileSize()),
                ResultPrinter.formatSize(grp.wastedBytes())));

        JPanel center = new DuplicateGroupDetailBuilder(this, currentGroupPaths, markedForDeletion,
                detailCheckboxes, previewPanel).build();

        if (detailPanel.getComponentCount() > 1) detailPanel.remove(1);
        detailPanel.add(center, BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    void onCheckboxChanged() { updateMarkedLabel(); }

    void saveCurrentGroupCheckboxes() {
        if (detailCheckboxes == null || currentGroupPaths == null) return;
        for (int i = 0; i < detailCheckboxes.length; i++) {
            if (detailCheckboxes[i].isSelected()) markedForDeletion.add(currentGroupPaths.get(i));
            else                                  markedForDeletion.remove(currentGroupPaths.get(i));
        }
        updateMarkedLabel();
    }

    private void updateMarkedLabel() {
        int n = markedForDeletion.size();
        footer.marked().setText(n == 0 ? "" : n + " Datei(en) zum Löschen markiert");
        footer.deleteAll().setText("Markierte löschen (" + n + ")");
        footer.deleteAll().setEnabled(n > 0);
    }

    private void markAllDuplicates() {
        if (lastResult == null || !lastResult.hasDuplicates()) return;
        for (ScanResult.DuplicateGroup grp : lastResult.getGroups()) {
            List<Path> paths = grp.getPaths();
            for (int i = 1; i < paths.size(); i++) markedForDeletion.add(paths.get(i));
        }
        updateMarkedLabel();
        if (currentGroupRow >= 0) buildDetailView(lastResult.getGroups().get(currentGroupRow));
        root.log("Alle Duplikate markiert: " + markedForDeletion.size() + " Datei(en)");
    }

    private void deleteAllMarked() {
        saveCurrentGroupCheckboxes();
        if (DuplicateTabFileOps.deleteMarked(root, markedForDeletion, root::log)) startScan();
    }

    private void clearDetail() {
        detailCheckboxes  = null;
        currentGroupPaths = null;
        previewPanel.preview(null);
        if (detailPanel.getComponentCount() > 1) detailPanel.remove(1);
        detailPanel.add(DuplicateTabUi.emptyStatePlaceholder(), BorderLayout.CENTER);
        lblDetailHeader.setText("  Gruppe auswählen …");
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void exportResults() {
        if (lastResult == null || !lastResult.hasDuplicates()) return;
        DuplicateTabFileOps.exportResults(root, lastResult, root::log);
    }

    private void resetAll() {
        lastResult = null;
        currentGroupRow = -1;
        markedForDeletion.clear();
        groupModel.setRowCount(0);
        clearDetail();
        updateMarkedLabel();
        footer.progress().setValue(0);
        footer.scan().setEnabled(false);
        footer.export().setEnabled(false);
        footer.markAll().setEnabled(false);
        footer.deleteAll().setEnabled(false);
        resetCollisionState();
        resetVisualDuplicateState();
        setStatus("Bereit.");
        dropZone.reset();
    }

    private void setStatus(String msg) { footer.status().setText(msg); }
}