package duplicatefinder.gui.synctab;

import javax.swing.JComponent;
import java.awt.*;

import static duplicatefinder.gui.UiTheme.*;

/** Zeichnet das "⟷ vergleichen"-Symbol zwischen den beiden Drop-Zonen in {@link SyncTab}. */
final class SyncArrowGlyph extends JComponent {

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(MUTED);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int cx = getWidth() / 2, cy = getHeight() / 2;
        String arrow = "⟷";
        g2.drawString(arrow, cx - fm.stringWidth(arrow) / 2, cy + fm.getAscent() / 2 - 6);
        g2.setFont(FONT_SMALL);
        String label = "vergleichen";
        g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + 18);
        g2.dispose();
    }
}
