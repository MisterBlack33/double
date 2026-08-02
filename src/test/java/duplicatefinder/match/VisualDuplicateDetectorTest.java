package duplicatefinder.match;

import duplicatefinder.scan.VisualDuplicateGroup;
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

class VisualDuplicateDetectorTest {

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
    void groupsSameMotifSavedAsDifferentFormats() throws IOException {
        // bmp/png sind beide verlustfrei – jpg würde durch Kompressionsrundung
        // die Hamming-Distanz künstlich erhöhen (siehe FolderComparatorTest).
        Path bmp = writeImage("a.bmp", Color.RED, 64, 64, "bmp");
        Path png = writeImage("b.png", Color.RED, 64, 64, "png");

        List<VisualDuplicateGroup> groups = VisualDuplicateDetector.detect(List.of(bmp, png), null);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getPaths().size());
    }

    @Test
    void doesNotGroupVisuallyDifferentImages() throws IOException {
        Path white = writeImage("white.png", Color.WHITE, 64, 64, "png");
        Path black = writeImage("black.png", Color.BLACK, 64, 64, "png");

        assertTrue(VisualDuplicateDetector.detect(List.of(white, black), null).isEmpty());
    }

    @Test
    void groupsThreeVisuallyIdenticalImagesTransitively() throws IOException {
        Path a = writeImage("a.png", Color.GREEN, 48, 48, "png");
        Path b = writeImage("b.gif", Color.GREEN, 48, 48, "gif");
        Path c = writeImage("c.bmp", Color.GREEN, 48, 48, "bmp");

        List<VisualDuplicateGroup> groups = VisualDuplicateDetector.detect(List.of(a, b, c), null);

        assertEquals(1, groups.size());
        assertEquals(3, groups.get(0).getPaths().size());
    }

    @Test
    void ignoresNonImageFiles() throws IOException {
        Path txt = Files.writeString(tempDir.resolve("notes.txt"), "hello");

        assertTrue(VisualDuplicateDetector.detect(List.of(txt), null).isEmpty());
    }

    @Test
    void singleImageProducesNoGroup() throws IOException {
        Path only = writeImage("solo.png", Color.BLUE, 32, 32, "png");

        assertTrue(VisualDuplicateDetector.detect(List.of(only), null).isEmpty());
    }

    @Test
    void skipsUnreadableImageFileWithoutThrowing() throws IOException {
        Path broken = Files.writeString(tempDir.resolve("broken.png"), "not an image");
        Path valid  = writeImage("valid.png", Color.PINK, 32, 32, "png");

        assertDoesNotThrow(() -> VisualDuplicateDetector.detect(List.of(broken, valid), null));
    }

    @Test
    void rejectsIncompatibleAspectRatioDespiteSameColor() throws IOException {
        Path wide = writeImage("wide.png", Color.ORANGE, 200, 50, "png");
        Path tall = writeImage("tall.png", Color.ORANGE, 50, 200, "png");

        assertTrue(VisualDuplicateDetector.detect(List.of(wide, tall), null).isEmpty());
    }

    @Test
    void invokesProgressCallbackForEachImage() throws IOException {
        Path a = writeImage("a.png", Color.CYAN, 20, 20, "png");
        Path b = writeImage("b.png", Color.CYAN, 20, 20, "png");
        int[] calls = {0};

        VisualDuplicateDetector.detect(List.of(a, b), (done, total) -> calls[0]++);

        assertEquals(2, calls[0]);
    }

    @Test
    void emptyInputProducesEmptyResult() {
        assertTrue(VisualDuplicateDetector.detect(List.of(), null).isEmpty());
    }
}