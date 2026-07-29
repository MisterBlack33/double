package duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.function.Consumer;

import static duplicatefinder.DuplicateFinderGUI.*;

/**
 * Baut die wiederkehrenden Swing-Komponenten von {@link SyncTab} (Drop-Zonen-Reihe, Fußzeile),
 * damit die Tab-Klasse selbst überschaubar bleibt. Tabellen-Layout siehe {@link SyncTabFormatting}.
 */
final class SyncTabUi {

    private SyncTabUi() {}

    /** Bündelt die beiden Drop-Zonen, damit {@link SyncTab} sie mit einem Aufruf verdrahten kann. */
    record DropRow(JPanel panel, DropZonePanel source, DropZonePanel target) {}

    static DropRow buildDropRow(JFrame owner, Consumer<File> onSourcePicked, Consumer<File> onTargetPicked) {
        JPanel p = Ui.panel(new GridLayout(1, 3, 12, 0));
        p.setPreferredSize(new Dimension(0, 140));

        var srcBox = buildDropBox(owner, "Quellordner (Referenz)", ACCENT, onSourcePicked);
        var tgtBox = buildDropBox(owner, "Zielordner (Vergleich)", SUCCESS, onTargetPicked);

        JPanel arrow = Ui.panel(new BorderLayout());
        arrow.add(new SyncArrowGlyph(), BorderLayout.CENTER);

        p.add(srcBox.panel()); p.add(arrow); p.add(tgtBox.panel());
        return new DropRow(p, srcBox.zone(), tgtBox.zone());
    }

    private record DropBox(JPanel panel, DropZonePanel zone) {}

    private static DropBox buildDropBox(JFrame owner, String title, Color titleColor, Consumer<File> onPicked) {
        JPanel box = Ui.panel(new BorderLayout(0, 6));
        box.add(Ui.label(title, FONT_BOLD, titleColor), BorderLayout.NORTH);
        DropZonePanel dz = new DropZonePanel(owner, onPicked);
        dz.setPreferredSize(new Dimension(0, 110));
        box.add(dz, BorderLayout.CENTER);
        return new DropBox(box, dz);
    }

    /** Bündelt die Footer-Komponenten, damit {@link SyncTab} sie mit einem Aufruf verdrahten kann. */
    record Footer(JPanel panel, JLabel status, JLabel marked, JProgressBar progress,
                  JButton clear, JButton compare, JButton markAllDuplicates, JButton deleteMarked) {}

    static Footer buildFooter() {
        JPanel p = Ui.panel(new BorderLayout(12, 0));
        p.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel left = Ui.panel(new GridLayout(3, 1, 0, 3));
        JLabel status = Ui.label("Quell- und Zielordner auswählen.", FONT_MONO, MUTED);
        JLabel marked = Ui.label("", FONT_MONO, WARNING);
        JProgressBar progress = Ui.progressBar();
        left.add(status); left.add(marked); left.add(progress);
        p.add(left, BorderLayout.CENTER);

        JPanel btns = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton clear             = Ui.button("Zurücksetzen",              BORDER, MUTED,  130, 34);
        JButton compare           = Ui.button("Vergleich starten ▶",      ACCENT, BG,     165, 34);
        JButton markAllDuplicates = Ui.button("Alle Duplikate markieren", WARNING, BG,    205, 34);
        JButton deleteMarked      = Ui.button("Markierte löschen (0)",    DANGER,  TEXT,  190, 34);
        compare.setEnabled(false);
        markAllDuplicates.setEnabled(false);
        deleteMarked.setEnabled(false);

        btns.add(clear); btns.add(compare); btns.add(markAllDuplicates); btns.add(deleteMarked);
        p.add(btns, BorderLayout.EAST);

        return new Footer(p, status, marked, progress, clear, compare, markAllDuplicates, deleteMarked);
    }
}
