package duplicatefinder.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.File;
import java.util.function.Consumer;

import static duplicatefinder.gui.UiTheme.*;

/** Wiederverwendbare Drag-&-Drop-Zone für Ordnerauswahl (Drop via Explorer/Finder oder Klick). */
public class DropZonePanel extends JPanel {

    private boolean   hovering, scanning;
    private File      folder;
    private float     angle;
    private final Timer spinTimer;
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
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(owner, "Drop fehlgeschlagen: " + ex.getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                }
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
        DropZonePainter.paint(g2, this, folder, hovering, scanning, angle);
        g2.dispose();
    }
}
