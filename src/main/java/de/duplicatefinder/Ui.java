package de.duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

import static de.duplicatefinder.DuplicateFinderGUI.*;

/**
 * Kleine Factory-Klasse für konsistente UI-Komponenten.
 */
final class Ui {

    private Ui() {}

    static JPanel panel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(BG);
        return p;
    }

    static JLabel label(String text, Font font, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(fg);
        l.setBackground(BG);
        return l;
    }

    static JButton button(String text, Color bg, Color fg, int w, int h) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = isEnabled() ? bg : bg.darker();
                g2.setColor(getModel().isRollover() && isEnabled() ? base.brighter() : base);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(fg);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    static JScrollPane scrollPane(JComponent c) {
        JScrollPane sc = new JScrollPane(c);
        sc.setBackground(SURFACE);
        sc.getViewport().setBackground(SURFACE);
        sc.setBorder(BorderFactory.createLineBorder(BORDER));
        return sc;
    }

    static void styleTable(JTable t) {
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setFont(FONT_MONO);
        t.setRowHeight(30);
        t.setGridColor(BORDER);
        t.setSelectionBackground(ACCENT_A);
        t.setSelectionForeground(TEXT);
        t.setShowVerticalLines(false);
        t.setFillsViewportHeight(true);
        t.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader th = t.getTableHeader();
        th.setBackground(CARD);
        th.setForeground(MUTED);
        th.setFont(new Font("SansSerif", Font.BOLD, 11));
        th.setReorderingAllowed(false);
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
    }

    /** Statuszeile mit Progress-Bar. */
    static JPanel statusBar(JLabel lbl, JProgressBar pb) {
        JPanel p = panel(new GridLayout(2, 1, 0, 4));
        p.add(lbl);
        p.add(pb);
        return p;
    }

    static JProgressBar progressBar() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setBackground(CARD);
        pb.setForeground(ACCENT);
        pb.setBorderPainted(false);
        pb.setPreferredSize(new Dimension(0, 5));
        pb.setStringPainted(false);
        return pb;
    }

    /** Formatiert Bytes in lesbare Einheit. */
    static String fmtSize(long bytes) {
        if (bytes < 1_024)      return bytes + " B";
        double kb = bytes / 1_024.0;
        if (kb < 1_024)         return String.format("%.1f KB", kb);
        double mb = kb / 1_024.0;
        if (mb < 1_024)         return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1_024.0);
    }
}
