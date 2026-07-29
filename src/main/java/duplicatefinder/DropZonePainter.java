package duplicatefinder;

import javax.swing.JComponent;
import java.awt.*;
import java.io.File;

import static duplicatefinder.DuplicateFinderGUI.*;

/** Reines Zeichnen der {@link DropZonePanel}-Zustände, getrennt von Interaktionslogik. */
final class DropZonePainter {

    private DropZonePainter() {}

    static void paint(Graphics2D g2, JComponent c, File folder, boolean hovering, boolean scanning, float angle) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = c.getWidth(), h = c.getHeight(), cx = w / 2, cy = h / 2;

        g2.setColor(hovering ? new Color(88, 166, 255, 20) : CARD);
        g2.fillRoundRect(0, 0, w, h, 12, 12);

        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 0, new float[]{9, 6}, 0));
        g2.setColor(hovering ? ACCENT : BORDER);
        g2.drawRoundRect(4, 4, w - 9, h - 9, 10, 10);
        g2.setStroke(new BasicStroke(1));

        if (scanning) {
            paintScanning(g2, cx, cy, angle);
        } else if (folder != null) {
            paintFolderSelected(g2, c, folder, cx, cy, w);
        } else {
            paintEmpty(g2, cx, cy, hovering);
        }
    }

    private static void paintScanning(Graphics2D g2, int cx, int cy, float angle) {
        g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(ACCENT);
        g2.drawArc(cx - 18, cy - 26, 36, 36, (int) angle, 270);
        g2.setStroke(new BasicStroke(1));
        drawText(g2, "Analyse läuft …", FONT_UI, ACCENT, cx, cy + 22);
    }

    private static void paintFolderSelected(Graphics2D g2, JComponent c, File folder, int cx, int cy, int w) {
        drawFolderShape(g2, cx, cy - 14, SUCCESS);
        drawText(g2, folder.getName(), FONT_BOLD, TEXT, cx, cy + 10);
        drawText(g2, truncate(c, folder.getAbsolutePath(), w - 60), FONT_MONO, MUTED, cx, cy + 26);
    }

    private static void paintEmpty(Graphics2D g2, int cx, int cy, boolean hovering) {
        drawFolderShape(g2, cx, cy - 14, hovering ? ACCENT : MUTED);
        drawText(g2, hovering ? "Ordner loslassen" : "Ordner hierher ziehen",
                FONT_BOLD, hovering ? ACCENT : TEXT, cx, cy + 10);
        drawText(g2, "oder klicken zum Auswählen", FONT_UI, MUTED, cx, cy + 26);
    }

    private static void drawFolderShape(Graphics2D g2, int cx, int cy, Color c) {
        g2.setColor(c);
        g2.fillRoundRect(cx - 20, cy + 2, 40, 16, 4, 4);
        int[] tx = {cx - 20, cx - 9, cx - 5, cx + 3};
        int[] ty = {cy + 2, cy + 2, cy - 6, cy - 6};
        g2.fillPolygon(tx, ty, 4);
        g2.setColor(c.brighter());
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(cx - 20, cy + 2, cx + 20, cy + 2);
        g2.setStroke(new BasicStroke(1));
    }

    private static void drawText(Graphics2D g2, String text, Font font, Color color, int cx, int y) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, y);
    }

    private static String truncate(JComponent c, String path, int maxPx) {
        FontMetrics fm = c.getFontMetrics(FONT_MONO);
        if (fm == null || fm.stringWidth(path) <= maxPx) return path;
        while (path.length() > 6 && fm.stringWidth("…" + path) > maxPx) path = path.substring(1);
        return "…" + path;
    }
}
