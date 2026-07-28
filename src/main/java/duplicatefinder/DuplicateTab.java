package duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;

import static duplicatefinder.DuplicateFinderGUI.*;

/**
 * Tab 1: Duplikate suchen, anzeigen und gruppenübergreifend löschen.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Ordner per Drag &amp; Drop oder Klick wählen</li>
 *   <li>"Scan starten" → dreistufige Analyse</li>
 *   <li>Gruppen durchklicken, Dateien in der Detailansicht markieren</li>
 *   <li>Markierungen bleiben gruppenübergreifend erhalten</li>
 *   <li>"X markierte löschen" → einmalige Bestätigung → alle auf einmal löschen → ein Neustart</li>
 * </ol>
 */
public class DuplicateTab extends JPanel {

    private final DuplicateFinderGUI root;

    // ── State ─────────────────────────────────────────────────────────────────
    private ScanResult        lastResult;
    /** Alle Dateien, die gruppenübergreifend zum Löschen markiert wurden. */
    private final Set<Path>   markedForDeletion = new LinkedHashSet<>();
    private int               currentGroupRow   = -1;

    // ── UI ────────────────────────────────────────────────────────────────────
    private DropZonePanel     dropZone;
    private JButton           btnScan, btnClear, btnExport, btnDeleteAll, btnMarkAll;
    private JLabel            lblMarked;        // "X Dateien markiert"
    private JProgressBar      progressBar;
    private JLabel            lblStatus;
    private JTable            groupTable;
    private DefaultTableModel groupModel;
    private JPanel            detailPanel;
    private JLabel            lblDetailHeader;
    private JCheckBox[]       detailCheckboxes;
    private List<Path>        currentGroupPaths; // Pfade der aktuell sichtbaren Gruppe

    // ─────────────────────────────────────────────────────────────────────────

    public DuplicateTab(DuplicateFinderGUI root) {
        this.root = root;
        setBackground(BG);
        setLayout(new BorderLayout(0, 14));
        setBorder(new EmptyBorder(14, 0, 0, 0));
        buildUI();
    }

    // ── Aufbau ────────────────────────────────────────────────────────────────

    private void buildUI() {
        dropZone = new DropZonePanel(root, this::onFolderSelected);
        dropZone.setPreferredSize(new Dimension(0, 120));
        add(dropZone, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildGroupPanel(), buildDetailPanel());
        split.setDividerLocation(580);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setBackground(BG);
        add(split, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JScrollPane buildGroupPanel() {
        String[] cols = {"#", "Dateien", "Größe", "Einsparpotenzial", "SHA-256"};
        groupModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 1) ? Integer.class : String.class;
            }
        };

        groupTable = new JTable(groupModel);
        Ui.styleTable(groupTable);
        groupTable.setAutoCreateRowSorter(true);
        groupTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onGroupSelected(groupTable.getSelectedRow());
        });

        int[] widths = {36, 56, 76, 120, 170};
        for (int i = 0; i < widths.length; i++)
            groupTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Einsparpotenzial rot
        groupTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setForeground(sel ? TEXT : DANGER);
                setBackground(sel ? ACCENT_A : SURFACE);
                setHorizontalAlignment(RIGHT);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                return this;
            }
        });

        return Ui.scrollPane(groupTable);
    }

    private JPanel buildDetailPanel() {
        detailPanel = Ui.panel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createLineBorder(BORDER));

        lblDetailHeader = Ui.label("  Gruppe auswählen …", FONT_BOLD, MUTED);
        lblDetailHeader.setOpaque(true);
        lblDetailHeader.setBackground(CARD);
        lblDetailHeader.setBorder(new EmptyBorder(8, 10, 8, 10));
        lblDetailHeader.setPreferredSize(new Dimension(0, 34));
        detailPanel.add(lblDetailHeader, BorderLayout.NORTH);

        JLabel ph = Ui.label("← Duplikat-Gruppe in der Tabelle auswählen", FONT_UI, MUTED);
        ph.setHorizontalAlignment(SwingConstants.CENTER);
        detailPanel.add(ph, BorderLayout.CENTER);
        return detailPanel;
    }

    private JPanel buildFooter() {
        JPanel p = Ui.panel(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Links: Status + Progress
        JPanel left = Ui.panel(new GridLayout(3, 1, 0, 3));
        lblStatus = Ui.label("Bereit.", FONT_MONO, MUTED);
        lblMarked = Ui.label("", FONT_MONO, new Color(210, 153, 34)); // WARNING-gelb
        progressBar = Ui.progressBar();
        left.add(lblStatus);
        left.add(lblMarked);
        left.add(progressBar);
        p.add(left, BorderLayout.CENTER);

        // Rechts: Buttons
        JPanel buttons = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnExport    = Ui.button("Exportieren",              CARD,    MUTED,  120, 34);
        btnMarkAll   = Ui.button("Alle Duplikate markieren", WARNING, BG,     210, 34);
        btnDeleteAll = Ui.button("Markierte löschen (0)",    DANGER,  TEXT,   190, 34);
        btnClear     = Ui.button("Zurücksetzen",             BORDER,  MUTED,  120, 34);
        btnScan      = Ui.button("Scan starten ▶",           ACCENT,  BG,     150, 34);

        btnExport.setEnabled(false);
        btnMarkAll.setEnabled(false);
        btnDeleteAll.setEnabled(false);
        btnScan.setEnabled(false);

        btnExport.addActionListener(e    -> exportResults());
        btnMarkAll.addActionListener(e   -> markAllDuplicates());
        btnDeleteAll.addActionListener(e -> deleteAllMarked());
        btnClear.addActionListener(e     -> resetAll());
        btnScan.addActionListener(e      -> startScan());

        buttons.add(btnExport);
        buttons.add(btnMarkAll);
        buttons.add(btnDeleteAll);
        buttons.add(btnClear);
        buttons.add(btnScan);
        p.add(buttons, BorderLayout.EAST);
        return p;
    }

    // ── Logik ─────────────────────────────────────────────────────────────────

    private void onFolderSelected(File folder) {
        btnScan.setEnabled(true);
        setStatus("Ordner: " + folder.getAbsolutePath());
        root.log("Ordner gewählt: " + folder.getAbsolutePath());
    }

    private void startScan() {
        groupModel.setRowCount(0);
        clearDetail();
        lastResult = null;
        markedForDeletion.clear();
        currentGroupRow = -1;
        updateMarkedLabel();
        btnScan.setEnabled(false);
        btnClear.setEnabled(false);
        btnExport.setEnabled(false);
        btnMarkAll.setEnabled(false);
        btnDeleteAll.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);
        dropZone.setScanning(true);

        File folder = dropZone.getFolder();
        root.log("Scan gestartet: " + folder.getAbsolutePath());

        SwingWorker<ScanResult, String> w = new SwingWorker<>() {
            @Override protected ScanResult doInBackground() throws Exception {
                publish("Lese Verzeichnis …");
                List<Path> files = new FileScanner().scan(folder.toPath());
                publish("Analysiere " + files.size() + " Dateien …");
                return new DuplicateDetector().findDuplicates(files, (done, total) ->
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setIndeterminate(false);
                            progressBar.setValue((int) (done * 100.0 / Math.max(total, 1)));
                            if (done % 30 == 0) publish("Hashing " + done + "/" + total + " …");
                        }));
            }
            @Override protected void process(List<String> c) { setStatus(c.get(c.size() - 1)); }
            @Override protected void done() {
                dropZone.setScanning(false);
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                btnScan.setEnabled(true);
                btnClear.setEnabled(true);
                try {
                    lastResult = get();
                    populateGroupTable();
                    root.log("Scan fertig: " + lastResult.getDuplicateGroupCount()
                            + " Gruppe(n), " + lastResult.getRedundantFileCount()
                            + " redundante Datei(en)");
                } catch (Exception ex) {
                    setStatus("Fehler: " + (ex.getCause() != null
                            ? ex.getCause().getMessage() : ex.getMessage()));
                    root.log("Fehler beim Scan: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void populateGroupTable() {
        if (!lastResult.hasDuplicates()) {
            setStatus("✓ Keine Duplikate. " + lastResult.getTotalFilesScanned() + " Dateien geprüft.");
            return;
        }
        int g = 1;
        for (ScanResult.DuplicateGroup grp : lastResult.getGroups()) {
            groupModel.addRow(new Object[]{
                    g++,
                    grp.getPaths().size(),
                    ResultPrinter.formatSize(grp.getFileSize()),
                    ResultPrinter.formatSize(grp.wastedBytes()),
                    grp.getHash().substring(0, 18) + "…"
            });
        }
        setStatus(String.format("✓ %d Gruppe(n) · %d redundante Datei(en) · Einsparpotenzial: %s",
                lastResult.getDuplicateGroupCount(),
                lastResult.getRedundantFileCount(),
                ResultPrinter.formatSize(lastResult.getTotalWastedBytes())));
        btnExport.setEnabled(true);
        btnMarkAll.setEnabled(true);
        if (groupModel.getRowCount() > 0) groupTable.setRowSelectionInterval(0, 0);
    }

    private void onGroupSelected(int viewRow) {
        if (viewRow < 0 || lastResult == null) return;
        // Checkbox-Zustand der aktuellen Gruppe erst sichern, bevor wir wechseln
        saveCurrentGroupCheckboxes();
        int modelRow = groupTable.convertRowIndexToModel(viewRow);
        currentGroupRow = modelRow;
        buildDetailView(lastResult.getGroups().get(modelRow));
    }

    // ── Detail-Ansicht ────────────────────────────────────────────────────────

    private void buildDetailView(ScanResult.DuplicateGroup grp) {
        currentGroupPaths = grp.getPaths();
        detailCheckboxes  = new JCheckBox[currentGroupPaths.size()];

        lblDetailHeader.setText(String.format("  Gruppe: %d Dateien · %s · Einsparpotenzial: %s",
                currentGroupPaths.size(),
                ResultPrinter.formatSize(grp.getFileSize()),
                ResultPrinter.formatSize(grp.wastedBytes())));

        JPanel listPanel = Ui.panel(new GridBagLayout());
        listPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.gridx = 0; gbc.insets = new Insets(2, 0, 2, 0);

        for (int i = 0; i < currentGroupPaths.size(); i++) {
            gbc.gridy = i;
            listPanel.add(buildFileRow(i, currentGroupPaths.get(i)), gbc);
        }
        gbc.gridy = currentGroupPaths.size(); gbc.weighty = 1;
        listPanel.add(Box.createVerticalGlue(), gbc);

        JScrollPane sc = new JScrollPane(listPanel);
        sc.setBorder(null);
        sc.getViewport().setBackground(SURFACE);

        // Schnellauswahl-Buttons
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.setBackground(CARD);
        JButton btnAll  = Ui.button("Alle",      CARD, TEXT,    60, 28);
        JButton btnDups = Ui.button("Duplikate", CARD, WARNING, 90, 28);
        JButton btnNone = Ui.button("Keine",     CARD, MUTED,   60, 28);
        btnAll.addActionListener(e  -> { setGroupCheckboxes(true);  saveCurrentGroupCheckboxes(); });
        btnDups.addActionListener(e -> { selectDuplicates();        saveCurrentGroupCheckboxes(); });
        btnNone.addActionListener(e -> { setGroupCheckboxes(false); saveCurrentGroupCheckboxes(); });
        actions.add(Ui.label("Markieren:", FONT_SMALL, MUTED));
        actions.add(btnAll); actions.add(btnDups); actions.add(btnNone);

        JPanel center = Ui.panel(new BorderLayout());
        center.add(sc,      BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);

        if (detailPanel.getComponentCount() > 1) detailPanel.remove(1);
        detailPanel.add(center, BorderLayout.CENTER);
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private JPanel buildFileRow(int idx, Path path) {
        boolean alreadyMarked = markedForDeletion.contains(path);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(idx == 0 ? new Color(63, 185, 80, 15) : SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(6, 8, 6, 8)));

        JCheckBox cb = new JCheckBox();
        cb.setBackground(row.getBackground());
        cb.setSelected(alreadyMarked);
        // Jede Änderung sofort in markedForDeletion übernehmen
        cb.addActionListener(e -> {
            if (cb.isSelected()) markedForDeletion.add(path);
            else                 markedForDeletion.remove(path);
            updateMarkedLabel();
        });
        detailCheckboxes[idx] = cb;

        JPanel info = Ui.panel(new GridLayout(2, 1, 0, 2));
        info.setBackground(row.getBackground());
        JLabel name = Ui.label(path.getFileName().toString(), FONT_BOLD,
                idx == 0 ? SUCCESS : TEXT);
        JLabel loc  = Ui.label(path.getParent() != null ? path.getParent().toString() : "",
                new Font("Monospaced", Font.PLAIN, 11), MUTED);
        info.add(name); info.add(loc);

        JLabel badge = Ui.label(idx == 0 ? "Original" : "Duplikat",
                FONT_SMALL, idx == 0 ? SUCCESS : DANGER);
        badge.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(cb,    BorderLayout.WEST);
        row.add(info,  BorderLayout.CENTER);
        row.add(badge, BorderLayout.EAST);
        return row;
    }

    /** Sichert den aktuellen Checkbox-Zustand in markedForDeletion (Sicherheitsnetz). */
    private void saveCurrentGroupCheckboxes() {
        if (detailCheckboxes == null || currentGroupPaths == null) return;
        for (int i = 0; i < detailCheckboxes.length; i++) {
            if (detailCheckboxes[i].isSelected()) markedForDeletion.add(currentGroupPaths.get(i));
            else                                  markedForDeletion.remove(currentGroupPaths.get(i));
        }
        updateMarkedLabel();
    }

    private void setGroupCheckboxes(boolean b) {
        if (detailCheckboxes == null) return;
        for (JCheckBox cb : detailCheckboxes) cb.setSelected(b);
    }

    private void selectDuplicates() {
        if (detailCheckboxes == null) return;
        detailCheckboxes[0].setSelected(false);
        for (int i = 1; i < detailCheckboxes.length; i++) detailCheckboxes[i].setSelected(true);
    }

    private void updateMarkedLabel() {
        int n = markedForDeletion.size();
        lblMarked.setText(n == 0 ? "" : n + " Datei(en) zum Löschen markiert");
        btnDeleteAll.setText("Markierte löschen (" + n + ")");
        btnDeleteAll.setEnabled(n > 0);
    }

    /** Markiert in jeder Gruppe alle Dateien außer dem ersten Eintrag (Original). */
    private void markAllDuplicates() {
        if (lastResult == null || !lastResult.hasDuplicates()) return;
        for (ScanResult.DuplicateGroup grp : lastResult.getGroups()) {
            List<Path> paths = grp.getPaths();
            for (int i = 1; i < paths.size(); i++) {   // Index 0 = Original → überspringen
                markedForDeletion.add(paths.get(i));
            }
        }
        updateMarkedLabel();
        // Detailansicht aktualisieren, damit Checkboxen den neuen Zustand zeigen
        if (currentGroupRow >= 0) buildDetailView(lastResult.getGroups().get(currentGroupRow));
        root.log("Alle Duplikate markiert: " + markedForDeletion.size() + " Datei(en)");
    }

    // ── Löschen (alle auf einmal) ─────────────────────────────────────────────

    private void deleteAllMarked() {
        saveCurrentGroupCheckboxes(); // sicherstellen, dass aktuelle Gruppe erfasst ist

        if (markedForDeletion.isEmpty()) {
            JOptionPane.showMessageDialog(root,
                    "Keine Dateien markiert.", "Hinweis", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Bestätigungsdialog
        StringBuilder msg = new StringBuilder();
        msg.append(markedForDeletion.size()).append(" Datei(en) werden permanent gelöscht:\n\n");
        int shown = 0;
        for (Path p : markedForDeletion) {
            msg.append("  ").append(p.toAbsolutePath()).append("\n");
            if (++shown == 20 && markedForDeletion.size() > 20) {
                msg.append("  … und ").append(markedForDeletion.size() - 20)
                        .append(" weitere\n");
                break;
            }
        }
        msg.append("\nDiese Aktion kann NICHT rückgängig gemacht werden!");

        int confirm = JOptionPane.showConfirmDialog(root, msg.toString(),
                "Löschen bestätigen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Alle auf einmal löschen
        int deleted = 0, failed = 0;
        for (Path p : new ArrayList<>(markedForDeletion)) {
            try {
                Files.delete(p);
                deleted++;
                root.log("Gelöscht: " + p.toAbsolutePath());
            } catch (IOException ex) {
                failed++;
                root.log("FEHLER: " + p.getFileName() + " – " + ex.getMessage());
            }
        }

        String summary = deleted + " Datei(en) gelöscht"
                + (failed > 0 ? ", " + failed + " fehlgeschlagen" : "") + ".";
        root.log(summary);
        JOptionPane.showMessageDialog(root, summary,
                "Löschen abgeschlossen", JOptionPane.INFORMATION_MESSAGE);

        // Erst jetzt: einmalig neu scannen
        startScan();
    }

    // ── Detail leeren ─────────────────────────────────────────────────────────

    private void clearDetail() {
        detailCheckboxes  = null;
        currentGroupPaths = null;
        if (detailPanel.getComponentCount() > 1) detailPanel.remove(1);
        JLabel ph = Ui.label("← Duplikat-Gruppe in der Tabelle auswählen", FONT_UI, MUTED);
        ph.setHorizontalAlignment(SwingConstants.CENTER);
        detailPanel.add(ph, BorderLayout.CENTER);
        lblDetailHeader.setText("  Gruppe auswählen …");
        detailPanel.revalidate();
        detailPanel.repaint();
    }

    // ── Export ────────────────────────────────────────────────────────────────

    private void exportResults() {
        if (lastResult == null || !lastResult.hasDuplicates()) return;
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Ergebnisse exportieren");
        fc.addChoosableFileFilter(new FileNameExtensionFilter("CSV (*.csv)", "csv"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("Text (*.txt)", "txt"));
        fc.setFileFilter(fc.getChoosableFileFilters()[1]);
        if (fc.showSaveDialog(root) != JFileChooser.APPROVE_OPTION) return;

        File out = fc.getSelectedFile();
        boolean csv = fc.getFileFilter().getDescription().contains("CSV")
                || out.getName().endsWith(".csv");
        if (!out.getName().contains("."))
            out = new File(out.getAbsolutePath() + (csv ? ".csv" : ".txt"));

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(out),
                        java.nio.charset.StandardCharsets.UTF_8))) {
            if (csv) {
                pw.println("Gruppe,Hash,Dateiname,Pfad,Groesse_B");
                int g = 1;
                for (ScanResult.DuplicateGroup grp : lastResult.getGroups()) {
                    for (Path p : grp.getPaths())
                        pw.printf("%d,%s,\"%s\",\"%s\",%d%n",
                                g, grp.getHash(), p.getFileName(), p.getParent(), grp.getFileSize());
                    g++;
                }
            } else {
                new ResultPrinter().print(lastResult);
            }
            root.log("Exportiert: " + out.getAbsolutePath());
            JOptionPane.showMessageDialog(root, "Exportiert:\n" + out.getAbsolutePath(),
                    "Export erfolgreich", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(root, "Fehler: " + ex.getMessage(),
                    "Export fehlgeschlagen", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    private void resetAll() {
        lastResult = null;
        currentGroupRow = -1;
        markedForDeletion.clear();
        groupModel.setRowCount(0);
        clearDetail();
        updateMarkedLabel();
        progressBar.setValue(0);
        btnScan.setEnabled(false);
        btnExport.setEnabled(false);
        btnMarkAll.setEnabled(false);
        btnDeleteAll.setEnabled(false);
        setStatus("Bereit.");
        dropZone.reset();
    }

    private void setStatus(String msg) { lblStatus.setText(msg); }
}