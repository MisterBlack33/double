package duplicatefinder.gui.duplicatetab;

import duplicatefinder.exclude.ExclusionStore;
import duplicatefinder.scan.VisualDuplicateGroup;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interaktive Ansicht der visuellen Duplikate (pHash-Ähnlichkeit, unterschiedlicher Byte-Inhalt):
 * Datei öffnen, einzeln löschen, oder als "verschieden" markieren (dauerhafter Ausschluss für
 * künftige Scans). Aufbau siehe {@link VisualDuplicateListBuilder}.
 *
 * <p><b>Annahme:</b> {@code VisualDuplicateGroup} bietet {@code getPaths()} sowie
 * {@code getMaxHammingDistance()} (entsprechend der bisherigen Anzeige "Max. Hamming-Distanz: X").
 * Falls die tatsächliche Klasse anders benannt ist, bitte {@link VisualDuplicateListBuilder}
 * entsprechend anpassen.
 */
public final class VisualDuplicateDialog {

    private VisualDuplicateDialog() {}

    public static void show(Component parent, List<VisualDuplicateGroup> groups,
                            ExclusionStore exclusions, Consumer<String> logger) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Visuelle Duplikate (unterschiedlicher Byte-Inhalt)");
        dialog.setModal(true);
        dialog.setSize(680, 720);
        dialog.setLocationRelativeTo(parent);

        var listPanel = new VisualDuplicateListBuilder(dialog, groups, exclusions, logger).build();
        dialog.add(new JScrollPane(listPanel), BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}