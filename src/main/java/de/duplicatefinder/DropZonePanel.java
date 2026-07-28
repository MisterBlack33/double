package de.duplicatefinder;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.util.function.Consumer;

import static de.duplicatefinder.DuplicateFinderGUI.*;

/**
 * Wiederverwendbare Drag-&-Drop-Zone für Ordnerauswahl.
 * Unterstützt Drop via Explorer/Finder sowie Klick zum Auswählen.
 */
public class DropZonePanel extends JPanel {

    private boolean   hovering, scanning;
    private File      folder;
    private float     angle;
    private Timer     spinTimer;
    private final Consumer<File> onFolderPicked;
    private final JFrame         owner;

    public DropZonePanel(JFrame owner, Consumer<File> onFolderPicked) {
        this.owner          = owner;
        this.onFolderPicked = onFolderPicked;

        setBackground(CARD);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        spinTimer = new Timer(16, e -> { angle += 4f; repaint(); });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (!scanning) pickFolder();
            }
        });

        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override public void dragEnter(DropTargetDragEvent e) { hovering = true;  repaint(); }
            @Override public void dragExit(DropTargetEvent e)      { hovering = false; repaint(); }
            @Override public void drop(DropTargetDropEvent e) {
                hovering = false;
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    java.util.List<File> fs = (java.util.List<File>)
                            e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!fs.isEmpty()) accept(fs.get(0));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
    }

    public void setFolder(File f)  { this.folder = f; repaint(); }
    public File getFolder()        { return folder; }

    public void setScanning(boolean s) {
        scanning = s;
        if (s) spinTimer.start(); else { spinTimer.stop(); repaint(); }
    }

    public void reset() {
        folder = null; hovering = false; scanning = false;
        spinTimer.stop(); repaint();
    }

    private void accept(File f) {
        if (f.isDirectory()) {
            folder = f;
            repaint();
            onFolderPicked.accept(f);
        } else {
            JOptionPane.showMessageDialog(owner,
                    "Bitte einen Ordner (kein Einzelfile) auswählen.",
                    "Kein Ordner", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void pickFolder() {
        JFileChooser fc = new JFileChooser(folder);
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Ordner auswählen");
        if (fc.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION) {
            accept(fc.getSelectedFile());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2;

        // Hintergrund
        g2.setColor(hovering ? new Color(88, 166, 255, 20) : CARD);
        g2.fillRoundRect(0, 0, w, h, 12, 12);

        // Gestrichelter Rahmen
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND, 0, new float[]{9, 6}, 0));
        g2.setColor(hovering ? ACCENT : BORDER);
        g2.drawRoundRect(4, 4, w - 9, h - 9, 10, 10);
        g2.setStroke(new BasicStroke(1));

        if (scanning) {
            g2.setStroke(new BasicStroke(2.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(ACCENT);
            g2.drawArc(cx - 18, cy - 26, 36, 36, (int) angle, 270);
            g2.setStroke(new BasicStroke(1));
            drawText(g2, "Analyse läuft …", FONT_UI, ACCENT, cx, cy + 22);

        } else if (folder != null) {
            drawFolderShape(g2, cx, cy - 14, SUCCESS);
            drawText(g2, folder.getName(), FONT_BOLD, TEXT, cx, cy + 10);
            drawText(g2, truncate(folder.getAbsolutePath(), w - 60), FONT_MONO, MUTED, cx, cy + 26);

        } else {
            drawFolderShape(g2, cx, cy - 14, hovering ? ACCENT : MUTED);
            drawText(g2, hovering ? "Ordner loslassen" : "Ordner hierher ziehen",
                    FONT_BOLD, hovering ? ACCENT : TEXT, cx, cy + 10);
            drawText(g2, "oder klicken zum Auswählen", FONT_UI, MUTED, cx, cy + 26);
        }
        g2.dispose();
    }

    private void drawFolderShape(Graphics2D g2, int cx, int cy, Color c) {
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

    private void drawText(Graphics2D g2, String text, Font font, Color color, int cx, int y) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2, y);
    }

    private String truncate(String path, int maxPx) {
        FontMetrics fm = getFontMetrics(FONT_MONO);
        if (fm == null || fm.stringWidth(path) <= maxPx) return path;
        while (path.length() > 6 && fm.stringWidth("…" + path) > maxPx) path = path.substring(1);
        return "…" + path;
    }
}
