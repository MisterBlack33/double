package duplicatefinder.gui;

import duplicatefinder.gui.duplicatetab.DuplicateTab;
import duplicatefinder.gui.synctab.SyncTab;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

import static duplicatefinder.gui.UiTheme.*;

/**
 * Haupt-GUI des Duplicate File Finders.
 * Drei Tabs: Duplikate suchen, Ordner vergleichen, Log.
 */
public class DuplicateFinderGUI extends JFrame {

    private final List<String> logLines = new ArrayList<>();

    private JTabbedPane tabs;
    private JTextArea logArea;

    public DuplicateFinderGUI() {
        super("Duplicate File Finder");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1080, 760);
        setMinimumSize(new Dimension(820, 580));
        setLocationRelativeTo(null);
        setBackground(BG);
        buildUI();
    }

    private void buildUI() {
        JPanel root = Ui.panel(new BorderLayout());
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.add(buildHeader(), BorderLayout.NORTH);
        tabs = buildTabs();
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel p = Ui.panel(new BorderLayout());
        p.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = Ui.label("Duplicate File Finder", new Font("Monospaced", Font.BOLD, 22), TEXT);
        JLabel sub = Ui.label(
                "SHA-256 · Dreistufige Analyse · Direkt löschen · Ordner synchronisieren",
                FONT_SMALL, MUTED);

        JPanel txt = Ui.panel(new GridLayout(2, 1, 0, 3));
        txt.add(title);
        txt.add(sub);
        p.add(txt, BorderLayout.WEST);
        return p;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tp = new JTabbedPane();
        tp.setBackground(BG);
        tp.setForeground(TEXT);
        tp.setFont(FONT_BOLD);

        tp.addTab("🔍  Duplikate suchen", new DuplicateTab(this));
        tp.addTab("🔄  Ordner vergleichen", new SyncTab(this));
        tp.addTab("📋  Log", buildLogTab());
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
        btnClear.addActionListener(e -> {
            logLines.clear();
            logArea.setText("");
        });
        JPanel footer = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        footer.add(btnClear);
        p.add(footer, BorderLayout.SOUTH);
        return p;
    }

    /** Öffentlich, da die Tab-Panels (anderes Package) hierhin loggen. */
    public void log(String msg) {
        String line = "[" + java.time.LocalTime.now().toString().substring(0, 8) + "] " + msg;
        logLines.add(line);
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(line + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    public static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fallback auf Standard-Look-and-Feel ist unkritisch.
        }
        SwingUtilities.invokeLater(() -> new DuplicateFinderGUI().setVisible(true));
    }
}
