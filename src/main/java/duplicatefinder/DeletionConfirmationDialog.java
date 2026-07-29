package duplicatefinder;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/** Baut die Löschbestätigung auf und holt die Zustimmung des Nutzers ein. */
public final class DeletionConfirmationDialog {

    public record Group(String label, Collection<Path> paths) {}

    private static final int MAX_LISTED = 20;

    private DeletionConfirmationDialog() {}

    public static boolean confirm(Component parent, List<Group> groups) {
        int total = groups.stream().mapToInt(g -> g.paths().size()).sum();
        if (total == 0) return false;

        String message = buildMessage(groups, total);
        return JOptionPane.showConfirmDialog(parent, message, "Löschen bestätigen",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static String buildMessage(List<Group> groups, int total) {
        StringBuilder sb = new StringBuilder();
        sb.append(total).append(" Datei(en) werden permanent gelöscht:\n\n");

        int shown = 0;
        outer:
        for (Group group : groups) {
            for (Path path : group.paths()) {
                if (shown++ >= MAX_LISTED) {
                    sb.append("  … und weitere\n");
                    break outer;
                }
                sb.append("  ").append(group.label()).append(path.toAbsolutePath()).append("\n");
            }
        }
        sb.append("\nDiese Aktion kann NICHT rückgängig gemacht werden!");
        return sb.toString();
    }
}
