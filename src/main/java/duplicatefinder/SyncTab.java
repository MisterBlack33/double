package duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import static duplicatefinder.DuplicateFinderGUI.*;

/**
 * Tab 2: Zwei Ordner vergleichen – byte-basiert + optionaler pHash-Bildvergleich.
 * Ein Klick auf eine Ergebniszeile zeigt die Quelldatei in der Dateivorschau.
 *
 * <h3>Byte-Vergleich</h3>
 * <pre>
 *  Fall  Name  Inhalt  Größe  Status         Farbe
 *   1     =      =      =     DUPLICATE      Rot
 *   2     =      =      ≠     NEEDS_REVIEW   Gelb
 *   3     =      ≠      =     CONFLICT       Orange
 *   5     ≠      =      =     NEEDS_REVIEW   Gelb
 *   6     ≠      =      ≠     NEEDS_REVIEW   Gelb
 * </pre>
 *
 * <h3>Visueller Vergleich (Bilder, pHash)</h3>
 * <pre>
 *  VISUAL_IDENTICAL       Hamming 0    Lila
 *  VISUAL_NEAR_IDENTICAL  Hamming 1–5  Blauviolett
 *  VISUAL_SIMILAR         Hamming 6–10 Blau
 *  VISUAL_POSSIBLY        Hamming 11–15 Hellblau
 * </pre>
 */
public class SyncTab extends JPanel {

    private final DuplicateFinderGUI root;

    // State
    private FolderSyncResult lastResult;
    private final Set<Path>  markedSource = new LinkedHashSet<>();
    private final Set<Path>  markedTarget = new LinkedHashSet<>();

    // UI
    private DropZonePanel     dropSource, dropTarget;
    private JButton           btnCompare, btnClear, btnDeleteMarked, btnMarkAllDuplicates;
    private JCheckBox         cbVisual;
    private JProgressBar      progressBar;
    private JLabel            lblStatus, lblSummary, lblMarked;
    private JTable            resultTable;
    private DefaultTableModel resultModel;
    private final FilePreviewPanel previewPanel = new FilePreviewPanel();

    // Farben für visuelle Status
    private static final Color VISUAL_1 = new Color(180,  80, 220);
    private static final Color VISUAL_2 = new Color(130,  80, 200);
    private static final Color VISUAL_3 = new Color( 88, 130, 255);
    private static final Color VISUAL_4 = new Color( 88, 166, 255);

    public SyncTab(DuplicateFinderGUI root) {
        this.root = root;
        setBackground(BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(14, 0, 0, 0));
        buildUI();
    }

    // ── Aufbau ────────────────────────────────────────────────────────────────

    private void buildUI() {
        add(buildDropRow(),  BorderLayout.NORTH);
        add(buildCenter(),   BorderLayout.CENTER);
        add(buildFooter(),   BorderLayout.SOUTH);
    }

    private JPanel buildDropRow() {
        JPanel p = Ui.panel(new GridLayout(1, 3, 12, 0));
        p.setPreferredSize(new Dimension(0, 140));
        p.add(buildDropBox("Quellordner (Referenz)",  true));
        p.add(buildArrow());
        p.add(buildDropBox("Zielordner (Vergleich)", false));
        return p;
    }

    private JPanel buildDropBox(String title, boolean isSource) {
        JPanel box = Ui.panel(new BorderLayout(0, 6));
        box.add(Ui.label(title, FONT_BOLD, isSource ? ACCENT : SUCCESS), BorderLayout.NORTH);
        DropZonePanel dz = new DropZonePanel(root, f -> {
            root.log((isSource ? "Quelle: " : "Ziel:   ") + f.getAbsolutePath());
            btnCompare.setEnabled(dropSource.getFolder() != null && dropTarget.getFolder() != null);
        });
        dz.setPreferredSize(new Dimension(0, 110));
        if (isSource) dropSource = dz; else dropTarget = dz;
        box.add(dz, BorderLayout.CENTER);
        return box;
    }

    private JPanel buildArrow() {
        JPanel p = Ui.panel(new BorderLayout());
        p.add(new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(MUTED);
                g2.setFont(new Font("SansSerif", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                String a = "⟷"; int cx = getWidth()/2, cy = getHeight()/2;
                g2.drawString(a, cx - fm.stringWidth(a)/2, cy + fm.getAscent()/2 - 6);
                g2.setFont(FONT_SMALL);
                String s = "vergleichen";
                g2.drawString(s, cx - fm.stringWidth(s)/2, cy + 18);
                g2.dispose();
            }
        }, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildCenter() {
        String[] cols = {"✓ Quelle", "✓ Ziel", "Status", "Fall",
                "Quelldatei (relativ)", "Zieldatei (relativ)",
                "Gr. Quelle", "Gr. Ziel", "Hamming"};
        resultModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 0 || c == 1; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 1) ? Boolean.class : String.class;
            }
        };
        resultTable = new JTable(resultModel);
        Ui.styleTable(resultTable);
        resultTable.setAutoCreateRowSorter(true);
        resultTable.setRowHeight(34);

        int[] w = {72, 62, 115, 44, 200, 200, 82, 82, 70};
        for (int i = 0; i < w.length; i++)
            resultTable.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        resultTable.getColumnModel().getColumn(3).setMaxWidth(50);
        resultTable.getColumnModel().getColumn(8).setMaxWidth(80);

        resultTable.getColumnModel().getColumn(2).setCellRenderer(statusRenderer());

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

        JPanel legend = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        legend.add(legendDot(DANGER,   "Duplikat"));
        legend.add(legendDot(WARNING,  "Zu prüfen"));
        legend.add(legendDot(new Color(210,120,40), "Konflikt"));
        legend.add(legendDot(VISUAL_1, "Vis. identisch"));
        legend.add(legendDot(VISUAL_2, "Vis. fast identisch"));
        legend.add(legendDot(VISUAL_3, "Vis. ähnlich"));
        legend.add(legendDot(VISUAL_4, "Vis. mögl. ähnlich"));
        legend.add(legendDot(new Color(46, 160, 130), "Text ähnlich"));

        lblSummary = Ui.label("", FONT_MONO, MUTED);
        JPanel topBar = Ui.panel(new BorderLayout());
        topBar.add(opts,   BorderLayout.WEST);
        topBar.add(legend, BorderLayout.EAST);

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

    private JPanel buildFooter() {
        JPanel p = Ui.panel(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel left = Ui.panel(new GridLayout(3, 1, 0, 3));
        lblStatus   = Ui.label("Quell- und Zielordner auswählen.", FONT_MONO, MUTED);
        lblMarked   = Ui.label("", FONT_MONO, WARNING);
        progressBar = Ui.progressBar();
        left.add(lblStatus); left.add(lblMarked); left.add(progressBar);
        p.add(left, BorderLayout.CENTER);

        JPanel btns = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnClear             = Ui.button("Zurücksetzen",              BORDER, MUTED,  130, 34);
        btnCompare           = Ui.button("Vergleich starten ▶",      ACCENT, BG,     165, 34);
        btnMarkAllDuplicates = Ui.button("Alle Duplikate markieren", WARNING, BG,    205, 34);
        btnDeleteMarked      = Ui.button("Markierte löschen (0)",    DANGER,  TEXT,  190, 34);

        btnCompare.setEnabled(false);
        btnMarkAllDuplicates.setEnabled(false);
        btnDeleteMarked.setEnabled(false);

        btnClear.addActionListener(e             -> resetAll());
        btnCompare.addActionListener(e           -> startCompare());
        btnMarkAllDuplicates.addActionListener(e -> markAllDuplicates());
        btnDeleteMarked.addActionListener(e      -> deleteMarked());

        btns.add(btnClear); btns.add(btnCompare);
        btns.add(btnMarkAllDuplicates); btns.add(btnDeleteMarked);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    // ── Vergleich ─────────────────────────────────────────────────────────────

    private void startCompare() {
        resultModel.setRowCount(0);
        lastResult = null;
        markedSource.clear(); markedTarget.clear();
        updateMarkedLabel();
        previewPanel.preview(null);
        btnCompare.setEnabled(false);
        btnMarkAllDuplicates.setEnabled(false);
        btnDeleteMarked.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        dropSource.setScanning(true);
        lblSummary.setText("");

        File src    = dropSource.getFolder();
        File tgt    = dropTarget.getFolder();
        boolean vis = cbVisual.isSelected();

        root.log("Vergleich: " + src.getName() + " ↔ " + tgt.getName()
                + (vis ? " [+ pHash]" : ""));

        SwingWorker<FolderSyncResult, String> w = new SwingWorker<>() {
            @Override protected FolderSyncResult doInBackground() throws Exception {
                publish(vis ? "SHA-256 + pHash-Index wird aufgebaut …"
                        : "SHA-256-Index wird aufgebaut …");
                return new FolderComparator().compare(src.toPath(), tgt.toPath(), vis,
                        (done, total) -> SwingUtilities.invokeLater(() -> {
                            progressBar.setIndeterminate(false);
                            progressBar.setValue((int)(done * 100.0 / Math.max(total, 1)));
                            publish("Analysiere " + done + " / " + total + " …");
                        }));
            }
            @Override protected void process(List<String> c) { setStatus(c.get(c.size()-1)); }
            @Override protected void done() {
                dropSource.setScanning(false);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                btnCompare.setEnabled(true);
                try {
                    lastResult = get();
                    populateTable();
                } catch (Exception ex) {
                    setStatus("Fehler: " + (ex.getCause() != null
                            ? ex.getCause().getMessage() : ex.getMessage()));
                }
            }
        };
        w.execute();
    }

    private void populateTable() {
        long dupes    = lastResult.countByStatus(FolderSyncResult.MatchStatus.DUPLICATE);
        long review   = lastResult.countByStatus(FolderSyncResult.MatchStatus.NEEDS_REVIEW);
        long conflict = lastResult.countByStatus(FolderSyncResult.MatchStatus.CONFLICT);
        long visTotal = lastResult.countVisual();

        for (FolderSyncResult.FileEntry fe : lastResult.getEntries()) {
            Path srcRel = dropSource.getFolder().toPath().relativize(fe.getSourcePath());
            Path tgtRel = dropTarget.getFolder().toPath().relativize(fe.getTargetPath());
            String hamming = fe.getHammingDistance() >= 0
                    ? fe.getHammingDistance() + "/64" : "";

            resultModel.addRow(new Object[]{
                    fe.isDuplicate(),
                    false,
                    statusLabel(fe.getStatus()),
                    caseLabel(fe),
                    srcRel.toString(),
                    tgtRel.toString(),
                    Ui.fmtSize(fe.getSourceSize()),
                    Ui.fmtSize(fe.getTargetSize()),
                    hamming
            });
        }

        setStatus(String.format(
                "✓ Fertig – %d Duplikate · %d zu prüfen · %d Konflikte · %d visuelle Treffer · %d ignoriert",
                dupes, review, conflict, visTotal, lastResult.getDifferentCount()));
        lblSummary.setText(String.format(
                "Duplikate: %d   |   Zu prüfen: %d   |   Konflikte: %d   |   Visuell: %d   |   Ignoriert: %d",
                dupes, review, conflict, visTotal, lastResult.getDifferentCount()));

        root.log(lblSummary.getText());
        btnMarkAllDuplicates.setEnabled(dupes > 0);
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
        lblMarked.setText(n == 0 ? "" :
                markedSource.size() + " Quelldatei(en) + " +
                        markedTarget.size() + " Zieldatei(en) markiert");
        btnDeleteMarked.setText("Markierte löschen (" + n + ")");
        btnDeleteMarked.setEnabled(n > 0);
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
        progressBar.setValue(0);
        btnCompare.setEnabled(false);
        btnMarkAllDuplicates.setEnabled(false);
        btnDeleteMarked.setEnabled(false);
        setStatus("Quell- und Zielordner auswählen.");
        dropSource.reset(); dropTarget.reset();
    }

    private void setStatus(String msg) { lblStatus.setText(msg); }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private String statusLabel(FolderSyncResult.MatchStatus s) {
        return switch (s) {
            case DUPLICATE               -> "Duplikat";
            case NEEDS_REVIEW            -> "Zu prüfen";
            case CONFLICT                -> "Konflikt";
            case NEAR_DUPLICATE_TEXT     -> "Text ähnlich";
            case VISUAL_IDENTICAL        -> "Vis. identisch";
            case VISUAL_NEAR_IDENTICAL   -> "Vis. fast id.";
            case VISUAL_SIMILAR          -> "Vis. ähnlich";
            case VISUAL_POSSIBLY_SIMILAR -> "Vis. mögl.";
            default                      -> "";
        };
    }

    private String caseLabel(FolderSyncResult.FileEntry fe) {
        boolean nameEq = fe.getSourcePath().getFileName().equals(fe.getTargetPath().getFileName());
        boolean sizeEq = fe.getSourceSize() == fe.getTargetSize();
        return switch (fe.getStatus()) {
            case DUPLICATE               -> "#1";
            case NEEDS_REVIEW            -> nameEq ? "#2" : (sizeEq ? "#5" : "#6");
            case CONFLICT                -> "#3";
            case NEAR_DUPLICATE_TEXT     -> "T1";
            case VISUAL_IDENTICAL        -> "V0";
            case VISUAL_NEAR_IDENTICAL   -> "V1";
            case VISUAL_SIMILAR          -> "V2";
            case VISUAL_POSSIBLY_SIMILAR -> "V3";
            default                      -> "";
        };
    }

    private Color statusColor(String label) {
        return switch (label) {
            case "Duplikat"      -> DANGER;
            case "Zu prüfen"     -> WARNING;
            case "Konflikt"      -> new Color(210, 120, 40);
            case "Text ähnlich"  -> new Color(46, 160, 130);
            case "Vis. identisch"-> VISUAL_1;
            case "Vis. fast id." -> VISUAL_2;
            case "Vis. ähnlich"  -> VISUAL_3;
            case "Vis. mögl."    -> VISUAL_4;
            default              -> TEXT;
        };
    }

    private TableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setForeground(sel ? TEXT : statusColor(v == null ? "" : v.toString()));
                setBackground(sel ? ACCENT_A : SURFACE);
                setFont(FONT_BOLD);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                return this;
            }
        };
    }

    private JPanel legendDot(Color c, String label) {
        JPanel p = Ui.panel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.add(new JComponent() {
            { setPreferredSize(new Dimension(9, 9)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c); g2.fillOval(0, 0, 9, 9); g2.dispose();
            }
        });
        p.add(Ui.label(label, FONT_SMALL, MUTED));
        return p;
    }
}