package duplicatefinder.gui.duplicatetab;

import duplicatefinder.exclude.ExclusionStore;
import duplicatefinder.gui.FileRowActions;
import duplicatefinder.gui.Ui;
import duplicatefinder.scan.NameCollisionGroup;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static duplicatefinder.gui.UiTheme.*;

/**
 * Baut die interaktive Gruppen-/Dateiliste für {@link NameCollisionDialog}.
 * Struktur analog {@link VisualDuplicateListBuilder}.
 */
final class NameCollisionListBuilder {

    private final JDialog dialog;
    private final List<NameCollisionGroup> groups;
    private final ExclusionStore exclusions;
    private final Consumer<String> logger;
    private JPanel root;

    NameCollisionListBuilder(JDialog dialog, List<NameCollisionGroup> groups,
                             ExclusionStore exclusions, Consumer<String> logger) {
        this.dialog = dialog;
        this.groups = new ArrayList<>(groups);
        this.exclusions = exclusions;
        this.logger = logger;
    }

    JPanel build() {
        root = Ui.panel(new GridBagLayout());
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        rebuild();
        return root;
    }

    private void rebuild() {
        root.removeAll();
        GridBagConstraints gbc = baseConstraints();
        int row = 0;

        root.add(Ui.label(groups.size() + " Namenskollision(en) gefunden:", FONT_BOLD, TEXT), at(gbc, row++));

        for (NameCollisionGroup group : new ArrayList<>(groups)) {
            root.add(buildGroupPanel(group), at(gbc, row++));
        }
        root.revalidate();
        root.repaint();
    }

    private GridBagConstraints baseConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.gridx = 0; gbc.insets = new Insets(4, 0, 4, 0);
        return gbc;
    }

    private GridBagConstraints at(GridBagConstraints gbc, int row) {
        gbc.gridy = row;
        return gbc;
    }

    private JPanel buildGroupPanel(NameCollisionGroup group) {
        JPanel panel = Ui.panel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(6, 8, 6, 8)));
        GridBagConstraints gbc = baseConstraints();

        panel.add(Ui.label(group.getFileName(), FONT_BOLD, WARNING), at(gbc, 0));
        int r = 1;
        for (Path path : group.getPaths()) {
            panel.add(buildFileRow(path, group.getPaths()), at(gbc, r++));
        }
        return panel;
    }

    private JPanel buildFileRow(Path path, List<Path> groupPaths) {
        JPanel row = Ui.panel(new BorderLayout(6, 0));
        row.add(Ui.label(path.toAbsolutePath().toString(), FONT_MONO, MUTED), BorderLayout.CENTER);

        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton open   = Ui.button("Öffnen", CARD, ACCENT, 80, 26);
        JButton delete = Ui.button("Löschen", CARD, DANGER, 80, 26);
        JButton keep   = Ui.button("Verschieden", CARD, SUCCESS, 100, 26);

        open.addActionListener(e -> FileRowActions.open(dialog, path));
        delete.addActionListener(e -> onDelete(path));
        keep.addActionListener(e -> onMarkDifferent(path, groupPaths));

        actions.add(open); actions.add(delete); actions.add(keep);
        row.add(actions, BorderLayout.EAST);
        return row;
    }

    private void onDelete(Path path) {
        if (!FileRowActions.deleteSingle(dialog, path, logger)) return;
        if (logger != null) logger.accept("Gelöscht (Namenskollision): " + path.getFileName());
        removeIfGroupBecomesTrivial(path);
        rebuild();
    }

    private void onMarkDifferent(Path path, List<Path> groupPaths) {
        FileRowActions.markDifferentFromRest(path, groupPaths, exclusions);
        if (logger != null) logger.accept("Als verschieden markiert: " + path.getFileName());
        removeIfGroupBecomesTrivial(path);
        rebuild();
    }

    private void removeIfGroupBecomesTrivial(Path path) {
        groups.removeIf(g -> g.getPaths().contains(path) && g.getPaths().size() <= 2);
    }
}