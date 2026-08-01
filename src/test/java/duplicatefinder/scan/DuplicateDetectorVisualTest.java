package duplicatefinder.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateDetectorVisualTest {

    @TempDir Path tempDir;

    private Path writeImage(String name, Color color, int w, int h, String format) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        Path p = tempDir.resolve(name);
        ImageIO.write(img, format, p.toFile());
        return p;
    }

    @Test
    void reportsVisualDuplicateForSameMotifDifferentFormat() throws IOException {
        // bmp/png sind beide verlustfrei – jpg würde durch Kompressionsrundung
        // die Hamming-Distanz künstlich erhöhen (siehe FolderComparatorTest).
        Path bmp = writeImage("photo.bmp", Color.MAGENTA, 40, 40, "bmp");
        Path png = writeImage("photo.png", Color.MAGENTA, 40, 40, "png");

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(bmp, png));

        assertTrue(result.hasVisualDuplicates());
        assertEquals(1, result.getVisualDuplicates().size());
        assertFalse(result.hasDuplicates(), "Unterschiedliche Bytes dürfen kein Byte-Duplikat ergeben");
    }

    @Test
    void byteDuplicateIsNotAlsoReportedAsVisualDuplicate() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "identical content");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "identical content");

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(a, b));

        assertTrue(result.hasDuplicates());
        assertFalse(result.hasVisualDuplicates());
    }

    @Test
    void exactByteDuplicateImageIsExcludedFromVisualPass() throws IOException {
        Path a = writeImage("a.png", Color.YELLOW, 40, 40, "png");
        Path b = Files.copy(a, tempDir.resolve("b.png"));

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(a, b));

        assertTrue(result.hasDuplicates());
        assertFalse(result.hasVisualDuplicates(), "Byte-identische Bilder sollen nur einmal gemeldet werden");
    }

    @Test
    void noVisualDuplicatesForUnrelatedImages() throws IOException {
        Path a = writeImage("a.png", Color.WHITE, 40, 40, "png");
        Path b = writeImage("b.png", Color.BLACK, 40, 40, "png");

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(a, b));

        assertFalse(result.hasVisualDuplicates());
    }
}