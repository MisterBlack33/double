package duplicatefinder.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PerceptualHasherTest {

    @TempDir Path tempDir;

    private BufferedImage solidImage(Color c, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    @Test
    void identicalImagesHashToSameValue() {
        BufferedImage img = solidImage(Color.RED, 64, 64);
        assertEquals(PerceptualHasher.hash(img), PerceptualHasher.hash(img));
    }

    @Test
    void hammingDistanceOfIdenticalHashesIsZero() {
        long h = PerceptualHasher.hash(solidImage(Color.BLUE, 64, 64));
        assertEquals(0, PerceptualHasher.hammingDistance(h, h));
        assertEquals(PerceptualHasher.Similarity.IDENTICAL, PerceptualHasher.similarity(h, h));
    }

    @Test
    void veryDifferentHashesAreNotIdentical() {
        long h1 = PerceptualHasher.hash(solidImage(Color.WHITE, 64, 64));
        long h2 = PerceptualHasher.hash(solidImage(Color.BLACK, 64, 64));
        assertNotEquals(PerceptualHasher.Similarity.IDENTICAL, PerceptualHasher.similarity(h1, h2));
    }

    @Test
    void recognizesSupportedImageExtensions() {
        assertTrue(PerceptualHasher.isImage(java.nio.file.Paths.get("photo.png")));
        assertTrue(PerceptualHasher.isImage(java.nio.file.Paths.get("photo.JPG")));
        assertFalse(PerceptualHasher.isImage(java.nio.file.Paths.get("document.pdf")));
    }

    @Test
    void aspectRatioCompatibleAcceptsEqualRatios() {
        assertTrue(PerceptualHasher.aspectRatioCompatible(100, 100, 200, 200));
    }

    @Test
    void aspectRatioCompatibleRejectsVeryDifferentRatios() {
        assertFalse(PerceptualHasher.aspectRatioCompatible(100, 100, 100, 400));
    }

    @Test
    void aspectRatioCompatibleAllowsUnknownDimensions() {
        assertTrue(PerceptualHasher.aspectRatioCompatible(0, 0, 100, 100));
    }

    @Test
    void histogramsOfIdenticalImagesAreSimilar() {
        BufferedImage img = solidImage(Color.GREEN, 32, 32);
        double[] h1 = PerceptualHasher.histogram(img);
        double[] h2 = PerceptualHasher.histogram(img);
        assertTrue(PerceptualHasher.histogramsPlausiblySimilar(h1, h2));
    }

    @Test
    void readsDimensionsFromRealFile() throws IOException {
        Path file = tempDir.resolve("img.png");
        ImageIO.write(solidImage(Color.YELLOW, 40, 20), "png", file.toFile());

        int[] dims = PerceptualHasher.readDimensions(file);

        assertEquals(40, dims[0]);
        assertEquals(20, dims[1]);
    }

    @Test
    void nonGifFileIsNeverAnimated() throws IOException {
        Path file = tempDir.resolve("img.png");
        ImageIO.write(solidImage(Color.CYAN, 10, 10), "png", file.toFile());

        assertFalse(PerceptualHasher.isAnimatedGif(file));
    }

    @Test
    void hashFromFileMatchesHashFromBufferedImage() throws IOException {
        Path file = tempDir.resolve("img.png");
        BufferedImage img = solidImage(Color.MAGENTA, 48, 48);
        ImageIO.write(img, "png", file.toFile());

        assertEquals(PerceptualHasher.hash(img), PerceptualHasher.hash(file));
    }

    @Test
    void hashThrowsForUnreadableFile() throws IOException {
        Path file = tempDir.resolve("notanimage.png");
        java.nio.file.Files.writeString(file, "not an image");

        assertThrows(UnsupportedOperationException.class, () -> PerceptualHasher.hash(file));
    }

    @Test
    void similarityThresholdsMapToExpectedLevels() {
        assertEquals(PerceptualHasher.Similarity.NEAR_IDENTICAL, PerceptualHasher.similarity(0b1L, 0b0L));
        assertEquals(PerceptualHasher.Similarity.SIMILAR,
                PerceptualHasher.similarity(0b1111111L, 0b0L));
        assertEquals(PerceptualHasher.Similarity.POSSIBLY_SIMILAR,
                PerceptualHasher.similarity(0xFFFL, 0b1L));
        assertEquals(PerceptualHasher.Similarity.DIFFERENT,
                PerceptualHasher.similarity(0xFFFFFFFFL, 0L));
    }

    @Test
    void readDimensionsThrowsForUnreadableFile() throws IOException {
        Path file = tempDir.resolve("notanimage.png");
        java.nio.file.Files.writeString(file, "not an image");

        assertThrows(IOException.class, () -> PerceptualHasher.readDimensions(file));
    }

    @Test
    void histogramFromFileMatchesHistogramFromBufferedImage() throws IOException {
        Path file = tempDir.resolve("img.png");
        BufferedImage img = solidImage(Color.RED, 32, 32);
        ImageIO.write(img, "png", file.toFile());

        assertArrayEquals(PerceptualHasher.histogram(img), PerceptualHasher.histogram(file));
    }

    @Test
    void histogramThrowsForUnreadableFile() throws IOException {
        Path file = tempDir.resolve("notanimage.png");
        java.nio.file.Files.writeString(file, "not an image");

        assertThrows(UnsupportedOperationException.class, () -> PerceptualHasher.histogram(file));
    }

    @Test
    void histogramsPlausiblySimilarRejectsVeryDifferentDistributions() {
        double[] a = {1.0, 0.0};
        double[] b = {0.0, 1.0};
        assertFalse(PerceptualHasher.histogramsPlausiblySimilar(a, b));
    }

    @Test
    void isAnimatedGifReturnsFalseForNonExistentGifFile() {
        assertFalse(PerceptualHasher.isAnimatedGif(tempDir.resolve("missing.gif")));
    }
}
