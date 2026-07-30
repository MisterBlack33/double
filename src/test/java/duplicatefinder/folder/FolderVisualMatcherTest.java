package duplicatefinder.folder;

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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FolderVisualMatcherTest {

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
    void visualEntryDetectsIdenticalImagesAcrossFiles() throws IOException {
        Path src = writeImage("a.png", Color.RED, 64, 64, "png");
        Path tgt = writeImage("b.png", Color.RED, 64, 64, "png");

        var entry = FolderVisualMatcher.visualEntry(src, tgt, 100, 100);

        assertNotNull(entry);
        assertTrue(entry.isVisualMatch());
    }

    @Test
    void visualEntryReturnsNullForIncompatibleAspectRatio() throws IOException {
        Path src = writeImage("a.png", Color.BLUE, 200, 50, "png");
        Path tgt = writeImage("b.png", Color.BLUE, 50, 200, "png");

        assertNull(FolderVisualMatcher.visualEntry(src, tgt, 100, 100));
    }

    @Test
    void findBestVisualMatchFindsClosestCandidate() throws IOException {
        Path src = writeImage("a.png", Color.GREEN, 64, 64, "png");
        Path candidate = writeImage("b.png", Color.GREEN, 64, 64, "png");
        Map<Long, List<Path>> index = FolderFileIndexer.buildPHashIndex(List.of(candidate), null);

        var best = FolderVisualMatcher.findBestVisualMatch(src, 100, index);

        assertNotNull(best);
        assertEquals(candidate, best.getTargetPath());
    }

    @Test
    void findBestVisualMatchReturnsNullWhenIndexEmpty() throws IOException {
        Path src = writeImage("a.png", Color.PINK, 32, 32, "png");
        assertNull(FolderVisualMatcher.findBestVisualMatch(src, 10, Map.of()));
    }

    @Test
    void textNearDuplicateEntryReturnsNullWhenTooDifferent() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "aaaaaaaaaaaaaaaaaaaaaaaa");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "zzzzzzzzzzzzzzzzzzzzzzzz totally unrelated content");

        assertNull(FolderVisualMatcher.textNearDuplicateEntry(a, b, 1, 1));
    }

    @Test
    void classifyNonByteMatchReturnsNullForUnrelatedBinaryFiles() throws IOException {
        Path a = Files.write(tempDir.resolve("a.bin"), new byte[]{1, 2, 3});
        Path b = Files.write(tempDir.resolve("b.bin"), new byte[]{9, 9, 9});

        assertNull(FolderVisualMatcher.classifyNonByteMatch(a, b, 3, 3, false));
    }

    @Test
    void visualEntryReturnsNullOnUnreadableImage() throws IOException {
        Path a = writeImage("a.png", Color.RED, 32, 32, "png");
        Path b = Files.writeString(tempDir.resolve("b.png"), "not really an image");

        assertNull(FolderVisualMatcher.visualEntry(a, b, 1, 1));
    }

    @Test
    void findBestVisualMatchReturnsNullOnCorruptSourceImage() throws IOException {
        Path src = Files.writeString(tempDir.resolve("a.png"), "corrupt");
        Path candidate = writeImage("b.png", Color.YELLOW, 32, 32, "png");
        Map<Long, List<Path>> index = FolderFileIndexer.buildPHashIndex(List.of(candidate), null);

        assertNull(FolderVisualMatcher.findBestVisualMatch(src, 1, index));
    }

    @Test
    void findBestVisualMatchSkipsIncompatibleAspectRatioCandidate() throws IOException {
        Path src = writeImage("a.png", Color.BLUE, 200, 50, "png");
        Path candidate = writeImage("b.png", Color.BLUE, 50, 200, "png");
        Map<Long, List<Path>> index = FolderFileIndexer.buildPHashIndex(List.of(candidate), null);

        assertNull(FolderVisualMatcher.findBestVisualMatch(src, 1, index));
    }

    @Test
    void textNearDuplicateEntryReturnsNullOnUnreadableFile() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "some readable text content here");
        Path b = tempDir.resolve("missing.txt");

        assertNull(FolderVisualMatcher.textNearDuplicateEntry(a, b, 1, 1));
    }

    @Test
    void classifyNonByteMatchFallsBackToNullWhenTargetIsNotImageDespiteSourceBeingImage() throws IOException {
        Path a = writeImage("a.png", Color.RED, 20, 20, "png");
        Path b = Files.write(tempDir.resolve("b.bin"), new byte[]{1, 2, 3});

        assertNull(FolderVisualMatcher.classifyNonByteMatch(a, b, 1, 1, true));
    }
}
