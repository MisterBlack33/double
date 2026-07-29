package duplicatefinder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Perceptual Hashing (pHash) für Bilder – ohne externe Bibliotheken.
 *
 * <h3>Ähnlichkeitsschwellen (Hamming-Distanz)</h3>
 * <ul>
 *   <li>0     → exakt identisch</li>
 *   <li>1–5   → visuell identisch, minimale Kompressionsartefakte</li>
 *   <li>6–10  → sehr ähnlich</li>
 *   <li>11–15 → ähnlich (zuschneiden, moderate Bearbeitung)</li>
 *   <li>&gt;15 → unterschiedliche Bilder</li>
 * </ul>
 */
public final class PerceptualHasher {

    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "wbmp", "tiff", "tif", "webp"
    );

    private static final int RESIZE  = 32;
    private static final double ASPECT_RATIO_TOLERANCE = 0.12;
    private static final int HISTOGRAM_BINS = 16;
    private static final double HISTOGRAM_MAX_DISTANCE = 0.35;

    /** Ähnlichkeitsstufen basierend auf Hamming-Distanz. */
    public enum Similarity { IDENTICAL, NEAR_IDENTICAL, SIMILAR, POSSIBLY_SIMILAR, DIFFERENT }

    private PerceptualHasher() {}

    public static long hash(Path imagePath) throws IOException {
        BufferedImage img = ImageIO.read(imagePath.toFile());
        if (img == null) {
            throw new UnsupportedOperationException("Format nicht lesbar: " + imagePath.getFileName());
        }
        return PerceptualHashMath.computeHash(img);
    }

    public static long hash(BufferedImage img) {
        return PerceptualHashMath.computeHash(img);
    }

    public static Similarity similarity(long hashA, long hashB) {
        int dist = hammingDistance(hashA, hashB);
        if (dist == 0)  return Similarity.IDENTICAL;
        if (dist <= 5)  return Similarity.NEAR_IDENTICAL;
        if (dist <= 10) return Similarity.SIMILAR;
        if (dist <= 15) return Similarity.POSSIBLY_SIMILAR;
        return Similarity.DIFFERENT;
    }

    /** Gibt die Hamming-Distanz (Anzahl verschiedener Bits) zwischen zwei Hashes zurück. */
    public static int hammingDistance(long hashA, long hashB) {
        return Long.bitCount(hashA ^ hashB);
    }

    public static boolean isImage(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1));
    }

    /** Liest nur die Bildmaße, ohne die vollen Pixel zu dekodieren. */
    public static int[] readDimensions(Path imagePath) throws IOException {
        try (javax.imageio.stream.ImageInputStream iis =
                     ImageIO.createImageInputStream(imagePath.toFile())) {
            java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext())
                throw new IOException("Format nicht lesbar: " + imagePath.getFileName());
            javax.imageio.ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Prüft, ob zwei Seitenverhältnisse innerhalb der Toleranz übereinstimmen.
     * Verhindert falsch-positive visuelle Treffer bei unterschiedlich geformten Bildern.
     */
    public static boolean aspectRatioCompatible(int wA, int hA, int wB, int hB) {
        if (wA <= 0 || hA <= 0 || wB <= 0 || hB <= 0) return true;
        double ratioA = (double) wA / hA;
        double ratioB = (double) wB / hB;
        double diff = Math.abs(ratioA - ratioB) / Math.max(ratioA, ratioB);
        return diff <= ASPECT_RATIO_TOLERANCE;
    }

    public static double[] histogram(Path imagePath) throws IOException {
        BufferedImage img = ImageIO.read(imagePath.toFile());
        if (img == null)
            throw new UnsupportedOperationException("Format nicht lesbar: " + imagePath.getFileName());
        return histogram(img);
    }

    public static double[] histogram(BufferedImage img) {
        BufferedImage gray = PerceptualHashMath.toGrayscale(PerceptualHashMath.resize(img, RESIZE, RESIZE));
        int[] bins = new int[HISTOGRAM_BINS];
        for (int y = 0; y < RESIZE; y++) {
            for (int x = 0; x < RESIZE; x++) {
                int value = gray.getRaster().getSample(x, y, 0);
                bins[Math.min(HISTOGRAM_BINS - 1, value * HISTOGRAM_BINS / 256)]++;
            }
        }
        double total = RESIZE * RESIZE;
        double[] normalized = new double[HISTOGRAM_BINS];
        for (int i = 0; i < HISTOGRAM_BINS; i++) normalized[i] = bins[i] / total;
        return normalized;
    }

    /** Gibt true zurück, wenn zwei Histogramme sich plausibel ähnlich sind. */
    public static boolean histogramsPlausiblySimilar(double[] a, double[] b) {
        double distance = 0.0;
        for (int i = 0; i < a.length; i++) distance += Math.abs(a[i] - b[i]);
        return distance <= HISTOGRAM_MAX_DISTANCE;
    }

    /** Gibt true zurück, wenn die GIF-Datei mehr als ein Frame enthält (animiert). */
    public static boolean isAnimatedGif(Path file) {
        if (!file.getFileName().toString().toLowerCase().endsWith(".gif")) return false;
        try (javax.imageio.stream.ImageInputStream iis =
                     ImageIO.createImageInputStream(file.toFile())) {
            if (iis == null) return false;
            java.util.Iterator<javax.imageio.ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) return false;
            javax.imageio.ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return reader.getNumImages(true) > 1;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            return false;
        }
    }
}
