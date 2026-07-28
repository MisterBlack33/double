package de.duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Haupt-GUI des Duplicate File Finders.
 *
 * Drei Tabs:
 *  1. Duplikate suchen  – Scannen, anzeigen, selektiv löschen
 *  2. Ordner vergleichen – zwei Ordner vergleichen, fehlende Dateien übertragen
 *  3. Log               – Protokoll aller Aktionen
 */
public class DuplicateFinderGUI extends JFrame {

    // ── Design-Tokens ─────────────────────────────────────────────────────────
    static final Color BG       = new Color(13, 17, 23);
    static final Color SURFACE  = new Color(22, 27, 34);
    static final Color CARD     = new Color(30, 37, 48);
    static final Color BORDER   = new Color(48, 54, 61);
    static final Color ACCENT   = new Color(88, 166, 255);
    static final Color ACCENT_A = new Color(88, 166, 255, 45);
    static final Color SUCCESS  = new Color(63, 185, 80);
    static final Color DANGER   = new Color(248, 81, 73);
    static final Color WARNING  = new Color(210, 153, 34);
    static final Color TEXT     = new Color(230, 237, 243);
    static final Color MUTED    = new Color(110, 118, 129);

    static final Font FONT_MONO  = new Font("Monospaced", Font.PLAIN, 12);
    static final Font FONT_UI    = new Font("SansSerif",  Font.PLAIN, 13);
    static final Font FONT_BOLD  = new Font("SansSerif",  Font.BOLD,  13);
    static final Font FONT_SMALL = new Font("SansSerif",  Font.PLAIN, 11);

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<String> logLines = new ArrayList<>();

    // ── UI ────────────────────────────────────────────────────────────────────
    private JTabbedPane tabs;
    private JTextArea   logArea;

    // ─────────────────────────────────────────────────────────────────────────

    public DuplicateFinderGUI() {
        super("Duplicate File Finder");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 760);
        setMinimumSize(new Dimension(820, 580));
        setLocationRelativeTo(null);
        setBackground(BG);
        buildUI();
    }

    // ── Aufbau ────────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = Ui.panel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        root.add(buildHeader(), BorderLayout.NORTH);

        // Tabs
        tabs = buildTabs();
        root.add(tabs, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = Ui.label("Duplicate File Finder",
                new Font("Monospaced", Font.BOLD, 22), TEXT);
        JLabel sub = Ui.label(
                "SHA-256 · Dreistufige Analyse · Direkt löschen · Ordner synchronisieren",
                FONT_SMALL, MUTED);

        JPanel txt = Ui.panel(new GridLayout(2, 1, 0, 3));
        txt.add(title); txt.add(sub);
        p.add(txt, BorderLayout.WEST);
        return p;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tp = new JTabbedPane();
        tp.setBackground(BG);
        tp.setForeground(TEXT);
        tp.setFont(FONT_BOLD);

        tp.addTab("🔍  Duplikate suchen",    new DuplicateTab(this));
        tp.addTab("🔄  Ordner vergleichen",  new SyncTab(this));
        tp.addTab("📋  Log",                 buildLogTab());
        return tp;
    }

    private JPanel buildLogTab() {
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(new EmptyBorder(14, 0, 0, 0));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(SURFACE);
        logArea.setForeground(MUTED);
        logArea.setFont(FONT_MONO);
        logArea.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane sc = new JScrollPane(logArea);
        sc.setBorder(BorderFactory.createLineBorder(BORDER));
        sc.getViewport().setBackground(SURFACE);
        p.add(sc, BorderLayout.CENTER);

        JButton btnClear = Ui.button("Log leeren", CARD, MUTED, 120, 32);
        btnClear.addActionListener(e -> { logLines.clear(); logArea.setText(""); });
        JPanel footer = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        footer.add(btnClear);
        p.add(footer, BorderLayout.SOUTH);
        return p;
    }

    // ── Logging (shared) ─────────────────────────────────────────────────────

    void log(String msg) {
        String line = "[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + msg;
        logLines.add(line);
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(line + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    // ── Entry Points ─────────────────────────────────────────────────────────

    public static void launch() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new DuplicateFinderGUI().setVisible(true));
    }

    public static void main(String[] args) { launch(); }
}
