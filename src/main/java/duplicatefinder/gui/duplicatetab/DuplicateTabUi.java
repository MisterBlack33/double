package duplicatefinder.gui.duplicatetab;

import duplicatefinder.gui.Ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

import static duplicatefinder.gui.UiTheme.*;

/**
 * Baut die wiederkehrenden Swing-Komponenten von {@link DuplicateTab} (Gruppentabelle,
 * Detail-Gerüst, Fußzeile), damit die Tab-Klasse selbst überschaubar bleibt.
 */
final class DuplicateTabUi {

    private DuplicateTabUi() {}

    static DefaultTableModel buildGroupModel() {
        String[] cols = {"#", "Dateien", "Größe", "Einsparpotenzial", "SHA-256"};
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 1) ? Integer.class : String.class;
            }
        };
    }

    static JTable buildGroupTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        Ui.styleTable(table);
        table.setAutoCreateRowSorter(true);

        int[] widths = {36, 56, 76, 120, 170};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setForeground(sel ? TEXT : DANGER);
                setBackground(sel ? ACCENT_A : SURFACE);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setBorder(new EmptyBorder(0, 6, 0, 6));
                return this;
            }
        });
        return table;
    }

    static JPanel buildDetailSkeleton(JLabel headerLabel) {
        JPanel detailPanel = Ui.panel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createLineBorder(BORDER));

        headerLabel.setOpaque(true);
        headerLabel.setBackground(CARD);
        headerLabel.setBorder(new EmptyBorder(8, 10, 8, 10));
        headerLabel.setPreferredSize(new Dimension(0, 34));
        detailPanel.add(headerLabel, BorderLayout.NORTH);
        detailPanel.add(emptyStatePlaceholder(), BorderLayout.CENTER);
        return detailPanel;
    }

    static JLabel emptyStatePlaceholder() {
        JLabel ph = Ui.label("← Duplikat-Gruppe in der Tabelle auswählen", FONT_UI, MUTED);
        ph.setHorizontalAlignment(SwingConstants.CENTER);
        return ph;
    }

    /** Bündelt die Footer-Komponenten, damit {@link DuplicateTab} sie mit einem Aufruf verdrahten kann. */
    record Footer(JPanel panel, JLabel status, JLabel marked, JProgressBar progress,
                  JButton export, JButton markAll, JButton deleteAll, JButton clear, JButton scan,
                  JButton nameCollisions) {}

    static Footer buildFooter() {
        JPanel p = Ui.panel(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel left = Ui.panel(new GridLayout(3, 1, 0, 3));
        JLabel status = Ui.label("Bereit.", FONT_MONO, MUTED);
        JLabel marked = Ui.label("", FONT_MONO, new Color(210, 153, 34));
        JProgressBar progress = Ui.progressBar();
        left.add(status); left.add(marked); left.add(progress);
        p.add(left, BorderLayout.CENTER);

        JPanel buttons = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton export         = Ui.button("Exportieren",              CARD,    MUTED,  120, 34);
        JButton markAll        = Ui.button("Alle Duplikate markieren", WARNING, BG,     210, 34);
        JButton deleteAll      = Ui.button("Markierte löschen (0)",    DANGER,  TEXT,   190, 34);
        JButton clear          = Ui.button("Zurücksetzen",             BORDER,  MUTED,  120, 34);
        JButton scan           = Ui.button("Scan starten ▶",           ACCENT,  BG,     150, 34);
        JButton nameCollisions = Ui.button("Namenskollisionen (0)",    CARD,    WARNING, 190, 34);
        for (JButton b : new JButton[]{export, markAll, deleteAll, scan, nameCollisions}) b.setEnabled(false);

        buttons.add(export); buttons.add(markAll); buttons.add(deleteAll);
        buttons.add(nameCollisions); buttons.add(clear); buttons.add(scan);
        p.add(buttons, BorderLayout.EAST);

        return new Footer(p, status, marked, progress, export, markAll, deleteAll, clear, scan, nameCollisions);
    }
}
