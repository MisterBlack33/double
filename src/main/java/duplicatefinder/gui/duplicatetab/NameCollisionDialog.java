package duplicatefinder.gui.duplicatetab;

import duplicatefinder.scan.NameCollisionGroup;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;

/** Zeigt gefundene Namenskollisionen (gleicher Name, unterschiedlicher Inhalt) in einem Dialog an. */
final class NameCollisionDialog {

    private static final int MAX_GROUPS_SHOWN = 15;

    private NameCollisionDialog() {}

    static void show(Component parent, List<NameCollisionGroup> collisions) {
        JOptionPane.showMessageDialog(parent, buildMessage(collisions),
                "Namenskollisionen (unterschiedlicher Inhalt)", JOptionPane.WARNING_MESSAGE);
    }

    static String buildMessage(List<NameCollisionGroup> collisions) {
        StringBuilder sb = new StringBuilder();
        sb.append(collisions.size()).append(" Namensgruppe(n) mit unterschiedlichem Inhalt gefunden:\n\n");

        int shown = 0;
        for (NameCollisionGroup group : collisions) {
            if (shown++ >= MAX_GROUPS_SHOWN) {
                sb.append("  … und weitere Gruppen\n");
                break;
            }
            appendGroup(sb, group);
        }
        return sb.toString();
    }

    private static void appendGroup(StringBuilder sb, NameCollisionGroup group) {
        sb.append(group.getFileName()).append(":\n");
        for (Path path : group.getPaths()) {
            sb.append("    ").append(path.toAbsolutePath()).append("\n");
        }
        sb.append("\n");
    }
}