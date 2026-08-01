package duplicatefinder.gui.duplicatetab;

import duplicatefinder.scan.VisualDuplicateGroup;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;

/** Zeigt visuell ähnliche Bilder mit unterschiedlichem Byte-Inhalt in einem Dialog an. */
final class VisualDuplicateDialog {

    private static final int MAX_GROUPS_SHOWN = 15;

    private VisualDuplicateDialog() {}

    static void show(Component parent, List<VisualDuplicateGroup> groups) {
        JOptionPane.showMessageDialog(parent, buildMessage(groups),
                "Visuelle Duplikate (unterschiedlicher Byte-Inhalt)", JOptionPane.INFORMATION_MESSAGE);
    }

    static String buildMessage(List<VisualDuplicateGroup> groups) {
        StringBuilder sb = new StringBuilder();
        sb.append(groups.size()).append(" visuelle Gruppe(n) gefunden:\n\n");

        int shown = 0;
        for (VisualDuplicateGroup group : groups) {
            if (shown++ >= MAX_GROUPS_SHOWN) {
                sb.append("  … und weitere Gruppen\n");
                break;
            }
            appendGroup(sb, group);
        }
        return sb.toString();
    }

    private static void appendGroup(StringBuilder sb, VisualDuplicateGroup group) {
        sb.append("  Max. Hamming-Distanz ").append(group.getMaxHammingDistance()).append(":\n");
        for (Path path : group.getPaths()) {
            sb.append("    ").append(path.toAbsolutePath()).append("\n");
        }
        sb.append("\n");
    }
}