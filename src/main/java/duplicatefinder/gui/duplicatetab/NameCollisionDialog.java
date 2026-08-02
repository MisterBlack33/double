package duplicatefinder.gui.duplicatetab;

import duplicatefinder.exclude.ExclusionStore;
import duplicatefinder.scan.NameCollisionGroup;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interaktive Ansicht der Namenskollisionen (gleicher Name, unterschiedlicher Inhalt):
 * Datei öffnen, einzeln löschen, oder als "verschieden" markieren (dauerhafter Ausschluss
 * für künftige Scans). Aufbau siehe {@link NameCollisionListBuilder}.
 */
public final class NameCollisionDialog {

    private NameCollisionDialog() {}

    public static void show(Component parent, List<NameCollisionGroup> groups,
                            ExclusionStore exclusions, Consumer<String> logger) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Namenskollisionen (unterschiedlicher Inhalt)");
        dialog.setModal(true);
        dialog.setSize(680, 720);
        dialog.setLocationRelativeTo(parent);

        var listPanel = new NameCollisionListBuilder(dialog, groups, exclusions, logger).build();
        dialog.add(new JScrollPane(listPanel), BorderLayout.CENTER);
        dialog.setVisible(true);
    }
}