package duplicatefinder.scan;

import duplicatefinder.hash.PerceptualHasher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Findet Dateien, die vor einem Vergleich aussortiert werden müssen: nicht lesbare Bytes
 * ODER (bei Bildformaten) nicht dekodierbarer Bildinhalt (z. B. defekte JPEG/PNG-Struktur).
 * Keine inhaltliche Ähnlichkeitsprüfung – rein strukturelle Integrität.
 */
public final class CorruptedFileScanner {

    private CorruptedFileScanner() {}

    public static List<Path> findUnreadable(Collection<Path> files) {
        List<Path> unreadable = new ArrayList<>();
        for (Path file : files) {
            if (!isReadable(file)) unreadable.add(file);
        }
        return unreadable;
    }

    static boolean isReadable(Path file) {
        if (!isReadableAsBytes(file)) return false;
        if (PerceptualHasher.isImage(file)) return isDecodableAsImage(file);
        return true;
    }

    private static boolean isReadableAsBytes(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            is.read();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isDecodableAsImage(Path file) {
        try {
            BufferedImage img = ImageIO.read(file.toFile());
            return img != null;
        } catch (Exception e) {
            return false;
        }
    }
}