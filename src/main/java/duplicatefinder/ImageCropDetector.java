package duplicatefinder;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Prüft, ob ein Bild als Ausschnitt eines größeren Bildes vorkommt (rechenintensiv, Opt-in). */
public final class ImageCropDetector {

    private static final double[] SCAN_SCALES = {1.0, 0.85, 0.7, 0.55, 0.4};
    private static final int GRID_STEPS = 4;
    private static final int CROP_MATCH_THRESHOLD = 8;

    private ImageCropDetector() {}

    public static boolean isCropOf(Path smaller, Path larger) throws IOException {
        BufferedImage small = ImageIO.read(smaller.toFile());
        BufferedImage big    = ImageIO.read(larger.toFile());
        if (small == null || big == null) return false;
        if (small.getWidth() > big.getWidth() || small.getHeight() > big.getHeight()) return false;

        long targetHash = PerceptualHasher.hash(small);
        return scanWindowsForMatch(big, targetHash);
    }

    private static boolean scanWindowsForMatch(BufferedImage big, long targetHash) {
        for (double scale : SCAN_SCALES) {
            int windowW = (int) (big.getWidth() * scale);
            int windowH = (int) (big.getHeight() * scale);
            if (windowW < 16 || windowH < 16) continue;
            if (scanGridAtScale(big, windowW, windowH, targetHash)) return true;
        }
        return false;
    }

    private static boolean scanGridAtScale(BufferedImage big, int windowW, int windowH, long targetHash) {
        int stepX = Math.max(1, (big.getWidth()  - windowW) / GRID_STEPS);
        int stepY = Math.max(1, (big.getHeight() - windowH) / GRID_STEPS);

        for (int y = 0; y + windowH <= big.getHeight(); y += stepY) {
            for (int x = 0; x + windowW <= big.getWidth(); x += stepX) {
                BufferedImage window = big.getSubimage(x, y, windowW, windowH);
                long windowHash = PerceptualHasher.hash(window);
                if (PerceptualHasher.hammingDistance(windowHash, targetHash) <= CROP_MATCH_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }
}
