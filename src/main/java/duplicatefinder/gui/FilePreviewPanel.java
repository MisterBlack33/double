package duplicatefinder.gui;

import duplicatefinder.hash.PerceptualHasher;
import duplicatefinder.match.FileKind;
import duplicatefinder.match.FileKindClassifier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;

import static duplicatefinder.gui.UiTheme.*;

/** Zeigt eine Inhaltsvorschau (Bild oder Text) für eine ausgewählte Datei an. */
public class FilePreviewPanel extends JPanel {

    private static final int THUMB_SIZE = 260;
    private static final int MAX_TEXT_CHARS = 4_000;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
    private final JTextArea textArea = new JTextArea();
    private final JLabel placeholderLabel = new JLabel("", SwingConstants.CENTER);

    public FilePreviewPanel() {
        super(new BorderLayout());
        setBackground(SURFACE);
        setBorder(BorderFactory.createLineBorder(BORDER));
        buildCards();
        add(header(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        showPlaceholder("Keine Datei ausgewählt");
    }

    /** Zeigt die Vorschau für die übergebene Datei; leitet je nach Typ weiter. */
    public void preview(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            showPlaceholder("Keine Vorschau verfügbar");
            return;
        }
        if (PerceptualHasher.isImage(file)) {
            previewImage(file);
        } else if (FileKindClassifier.classify(file) == FileKind.TEXT) {
            previewText(file);
        } else {
            showPlaceholder("Kein Vorschautyp für: " + file.getFileName());
        }
    }

    private void previewImage(Path file) {
        try {
            BufferedImage img = javax.imageio.ImageIO.read(file.toFile());
            if (img == null) throw new IOException("nicht lesbar");
            imageLabel.setIcon(new ImageIcon(scale(img)));
            cards.show(content, "image");
        } catch (IOException e) {
            showPlaceholder("Bild nicht lesbar: " + file.getFileName());
        }
    }

    private void previewText(Path file) {
        try {
            String raw = Files.readString(file);
            textArea.setText(raw.length() > MAX_TEXT_CHARS
                    ? raw.substring(0, MAX_TEXT_CHARS) + "\n…"
                    : raw);
            textArea.setCaretPosition(0);
            cards.show(content, "text");
        } catch (MalformedInputException e) {
            showPlaceholder("Kein Text (Binärinhalt): " + file.getFileName());
        } catch (IOException e) {
            showPlaceholder("Datei nicht lesbar: " + file.getFileName());
        }
    }

    private void showPlaceholder(String message) {
        placeholderLabel.setText(message);
        cards.show(content, "placeholder");
    }

    private Image scale(BufferedImage img) {
        double ratio = Math.min(1.0, (double) THUMB_SIZE / Math.max(img.getWidth(), img.getHeight()));
        int w = Math.max(1, (int) (img.getWidth() * ratio));
        int h = Math.max(1, (int) (img.getHeight() * ratio));
        return img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
    }

    private JLabel header() {
        JLabel l = Ui.label("Vorschau", FONT_BOLD, MUTED);
        l.setBorder(new EmptyBorder(6, 10, 6, 10));
        l.setOpaque(true);
        l.setBackground(CARD);
        return l;
    }

    private void buildCards() {
        imageLabel.setOpaque(true);
        imageLabel.setBackground(SURFACE);
        content.add(imageLabel, "image");

        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setFont(FONT_MONO);
        textArea.setBackground(SURFACE);
        textArea.setForeground(TEXT);
        content.add(new JScrollPane(textArea), "text");

        placeholderLabel.setForeground(MUTED);
        placeholderLabel.setFont(FONT_UI);
        placeholderLabel.setOpaque(true);
        placeholderLabel.setBackground(SURFACE);
        content.add(placeholderLabel, "placeholder");
    }
}
