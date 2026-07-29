package duplicatefinder;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static duplicatefinder.DuplicateFinderGUI.*;

/**
 * Baut die Detailansicht einer Duplikat-Gruppe (Dateiliste mit Checkboxen + Markier-Aktionen)
 * für {@link DuplicateTab}, damit dieser nicht zu einer God-Class anwächst.
 */
final class DuplicateGroupDetailBuilder {

    private final DuplicateTab tab;
    private final List<Path> paths;
    private final Set<Path> markedForDeletion;
    private final JCheckBox[] checkboxes;
    private final FilePreviewPanel previewPanel;

    DuplicateGroupDetailBuilder(DuplicateTab tab, List<Path> paths, Set<Path> markedForDeletion,
                                JCheckBox[] checkboxes, FilePreviewPanel previewPanel) {
        this.tab = tab;
        this.paths = paths;
        this.markedForDeletion = markedForDeletion;
        this.checkboxes = checkboxes;
        this.previewPanel = previewPanel;
    }

    JPanel build() {
        JPanel listPanel = buildFileList();
        JScrollPane sc = new JScrollPane(listPanel);
        sc.setBorder(null);
        sc.getViewport().setBackground(SURFACE);

        JPanel center = Ui.panel(new BorderLayout());
        center.add(sc, BorderLayout.CENTER);
        center.add(buildActions(), BorderLayout.SOUTH);
        return center;
    }

    private JPanel buildFileList() {
        JPanel listPanel = Ui.panel(new GridBagLayout());
        listPanel.setBorder(new EmptyBorder(8, 10, 8, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.gridx = 0; gbc.insets = new Insets(2, 0, 2, 0);

        for (int i = 0; i < paths.size(); i++) {
            gbc.gridy = i;
            listPanel.add(buildFileRow(i, paths.get(i)), gbc);
        }
        gbc.gridy = paths.size(); gbc.weighty = 1;
        listPanel.add(Box.createVerticalGlue(), gbc);
        return listPanel;
    }

    private JPanel buildFileRow(int idx, Path path) {
        boolean alreadyMarked = markedForDeletion.contains(path);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(idx == 0 ? new Color(63, 185, 80, 15) : SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { previewPanel.preview(path); }
        });

        JCheckBox cb = new JCheckBox();
        cb.setBackground(row.getBackground());
        cb.setSelected(alreadyMarked);
        cb.addActionListener(e -> {
            if (cb.isSelected()) markedForDeletion.add(path);
            else                 markedForDeletion.remove(path);
            tab.onCheckboxChanged();
        });
        checkboxes[idx] = cb;

        JPanel info = Ui.panel(new GridLayout(2, 1, 0, 2));
        info.setBackground(row.getBackground());
        JLabel name = Ui.label(path.getFileName().toString(), FONT_BOLD, idx == 0 ? SUCCESS : TEXT);
        JLabel loc  = Ui.label(path.getParent() != null ? path.getParent().toString() : "",
                new Font("Monospaced", Font.PLAIN, 11), MUTED);
        info.add(name); info.add(loc);

        JLabel badge = Ui.label(idx == 0 ? "Original" : "Duplikat", FONT_SMALL, idx == 0 ? SUCCESS : DANGER);
        badge.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(cb,    BorderLayout.WEST);
        row.add(info,  BorderLayout.CENTER);
        row.add(badge, BorderLayout.EAST);
        return row;
    }

    private JPanel buildActions() {
        JPanel actions = Ui.panel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        actions.setBackground(CARD);
        JButton btnAll  = Ui.button("Alle",      CARD, TEXT,    60, 28);
        JButton btnDups = Ui.button("Duplikate", CARD, WARNING, 90, 28);
        JButton btnNone = Ui.button("Keine",     CARD, MUTED,   60, 28);
        btnAll.addActionListener(e  -> { setAll(true);  tab.saveCurrentGroupCheckboxes(); });
        btnDups.addActionListener(e -> { selectDuplicatesOnly(); tab.saveCurrentGroupCheckboxes(); });
        btnNone.addActionListener(e -> { setAll(false); tab.saveCurrentGroupCheckboxes(); });
        actions.add(Ui.label("Markieren:", FONT_SMALL, MUTED));
        actions.add(btnAll); actions.add(btnDups); actions.add(btnNone);
        return actions;
    }

    private void setAll(boolean b) {
        for (JCheckBox cb : checkboxes) cb.setSelected(b);
    }

    private void selectDuplicatesOnly() {
        checkboxes[0].setSelected(false);
        for (int i = 1; i < checkboxes.length; i++) checkboxes[i].setSelected(true);
    }
}
