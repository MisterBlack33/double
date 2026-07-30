package duplicatefinder.media;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageCropDetectorTest {

    @TempDir Path tempDir;

    @Test
    void detectsSolidColorSmallImageAsCropOfLargerSameColorImage() throws IOException {
        Path big = tempDir.resolve("big.png");
        Path small = tempDir.resolve("small.png");
        ImageIO.write(solid(Color.ORANGE, 200, 200), "png", big.toFile());
        ImageIO.write(solid(Color.ORANGE, 60, 60), "png", small.toFile());

        assertTrue(ImageCropDetector.isCropOf(small, big));
    }

    @Test
    void rejectsWhenSmallerIsActuallyBigger() throws IOException {
        Path a = tempDir.resolve("a.png");
        Path b = tempDir.resolve("b.png");
        ImageIO.write(solid(Color.CYAN, 50, 50), "png", a.toFile());
        ImageIO.write(solid(Color.CYAN, 200, 200), "png", b.toFile());

        assertFalse(ImageCropDetector.isCropOf(b, a));
    }

    @Test
    void rejectsUnrelatedImages() throws IOException {
        Path big = tempDir.resolve("big.png");
        Path small = tempDir.resolve("small.png");
        ImageIO.write(solid(Color.WHITE, 200, 200), "png", big.toFile());
        ImageIO.write(solid(Color.BLACK, 60, 60), "png", small.toFile());

        assertFalse(ImageCropDetector.isCropOf(small, big));
    }

    private BufferedImage solid(Color c, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }
}
