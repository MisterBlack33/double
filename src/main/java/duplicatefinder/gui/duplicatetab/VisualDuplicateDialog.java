package duplicatefinder.gui.duplicatetab;

import duplicatefinder.exclude.ExclusionStore;
import duplicatefinder.scan.VisualDuplicateGroup;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interaktive Ansicht der visuellen Duplikate (pHash-Ähnlichkeit, unterschiedlicher Byte-Inhalt).
 * Aufbau siehe {@link VisualDuplicateListBuilder}.
 */
public final class VisualDuplicateDialog {

    private static final int MAX_LISTED_GROUPS = 15;

    private VisualDuplicateDialog() {}

    public static void show(Component parent, List<VisualDuplicateGroup> groups,
                            ExclusionStore exclusions, Consumer<String> logger) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                "Visuelle Duplikate (unterschiedlicher Byte-Inhalt)");
        dialog.setModal(true);
        dialog.setSize(680, 720);
        dialog.setMinimumSize(new Dimension(420, 320));
        dialog.setLocationRelativeTo(parent);

        var listPanel = new VisualDuplicateListBuilder(dialog, groups, exclusions, logger).build();
        dialog.add(new JScrollPane(listPanel), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    /** Baut die Textzusammenfassung für Vorschau/Log-Zwecke, ohne die Swing-Ansicht zu öffnen. */
    public static String buildMessage(List<VisualDuplicateGroup> groups) {
        StringBuilder sb = new StringBuilder();
        sb.append(groups.size()).append(" visuelle Gruppe(n) gefunden:\n\n");

        int shown = 0;
        for (VisualDuplicateGroup group : groups) {
            if (shown++ >= MAX_LISTED_GROUPS) {
                sb.append("  … und weitere Gruppen\n");
                break;
            }
            appendGroup(sb, group);
        }
        return sb.toString();
    }

    private static void appendGroup(StringBuilder sb, VisualDuplicateGroup group) {
        sb.append("Hamming-Distanz ").append(group.getMaxHammingDistance()).append(":\n");
        for (Path path : group.getPaths()) {
            sb.append("  ").append(path.toAbsolutePath()).append("\n");
        }
        sb.append("\n");
    }
}