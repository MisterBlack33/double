package duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import static duplicatefinder.DuplicateFinderGUI.*;

/** Tabellenspalten, Status-Labels, Farben und Legende für {@link SyncTab}. */
final class SyncTabFormatting {

    private static final Color VISUAL_1 = new Color(180,  80, 220);
    private static final Color VISUAL_2 = new Color(130,  80, 200);
    private static final Color VISUAL_3 = new Color( 88, 130, 255);
    private static final Color VISUAL_4 = new Color( 88, 166, 255);

    private SyncTabFormatting() {}

    static DefaultTableModel buildResultModel() {
        String[] cols = {"✓ Quelle", "✓ Ziel", "Status", "Fall",
                "Quelldatei (relativ)", "Zieldatei (relativ)",
                "Gr. Quelle", "Gr. Ziel", "Hamming"};
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 0 || c == 1; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 0 || c == 1) ? Boolean.class : String.class;
            }
        };
    }

    static void applyColumnLayout(JTable table) {
        int[] w = {72, 62, 115, 44, 200, 200, 82, 82, 70};
        for (int i = 0; i < w.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        table.getColumnModel().getColumn(3).setMaxWidth(50);
        table.getColumnModel().getColumn(8).setMaxWidth(80);
    }

    static JPanel buildLegend() {
        JPanel legend = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        legend.add(legendDot(DANGER,   "Duplikat"));
        legend.add(legendDot(WARNING,  "Zu prüfen"));
        legend.add(legendDot(new Color(210, 120, 40), "Konflikt"));
        legend.add(legendDot(VISUAL_1, "Vis. identisch"));
        legend.add(legendDot(VISUAL_2, "Vis. fast identisch"));
        legend.add(legendDot(VISUAL_3, "Vis. ähnlich"));
        legend.add(legendDot(VISUAL_4, "Vis. mögl. ähnlich"));
        legend.add(legendDot(new Color(46, 160, 130), "Text ähnlich"));
        return legend;
    }

    static String statusLabel(FolderSyncResult.MatchStatus s) {
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

    static String caseLabel(FolderSyncResult.FileEntry fe) {
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

    private static Color statusColor(String label) {
        return switch (label) {
            case "Duplikat"       -> DANGER;
            case "Zu prüfen"      -> WARNING;
            case "Konflikt"       -> new Color(210, 120, 40);
            case "Text ähnlich"   -> new Color(46, 160, 130);
            case "Vis. identisch" -> VISUAL_1;
            case "Vis. fast id."  -> VISUAL_2;
            case "Vis. ähnlich"   -> VISUAL_3;
            case "Vis. mögl."     -> VISUAL_4;
            default               -> TEXT;
        };
    }

    static TableCellRenderer statusRenderer() {
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

    private static JPanel legendDot(Color c, String label) {
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
