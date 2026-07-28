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
 * <h3>Funktionsweise</h3>
 * <ol>
 *   <li>Bild auf 32×32 Pixel skalieren (Graustufen)</li>
 *   <li>2D-DCT berechnen (Discrete Cosine Transform)</li>
 *   <li>Nur die obere linke 8×8-Ecke der DCT verwenden (niedrige Frequenzen)</li>
 *   <li>Median der 64 Werte berechnen</li>
 *   <li>64-Bit-Hash: jedes Bit = 1 wenn DCT-Wert > Median, sonst 0</li>
 * </ol>
 *
 * <h3>Ähnlichkeitsschwellen (Hamming-Distanz)</h3>
 * <ul>
 *   <li>0       → exakt identisch (pixelgenau, nach Format-Konversion)</li>
 *   <li>1–5     → visuell identisch, minimale Kompressionsartefakte</li>
 *   <li>6–10    → sehr ähnlich (leichte Helligkeitsänderung, kleines Wasserzeichen)</li>
 *   <li>11–15   → ähnlich (zuschneiden, moderate Bearbeitung)</li>
 *   <li>&gt;15  → unterschiedliche Bilder</li>
 * </ul>
 */
public final class PerceptualHasher {

    // Bildformate die ImageIO ohne externe Bibliothek lesen kann
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "wbmp", "tiff", "tif", "webp"
    );

    private static final int RESIZE  = 32;  // Skalierungsgröße für DCT
    private static final int DCT_SIZE = 8;  // Größe des genutzten DCT-Fensters

    private static final double ASPECT_RATIO_TOLERANCE = 0.12;
    private static final int HISTOGRAM_BINS = 16;
    private static final double HISTOGRAM_MAX_DISTANCE = 0.35;

    /** Ähnlichkeitsstufen basierend auf Hamming-Distanz. */
    public enum Similarity {
        /** Hamming 0: pixelidentisch nach Normalisierung (Format-Konversion, EXIF-Strip). */
        IDENTICAL,
        /** Hamming 1–5: Kompressionsartefakte, minimale Qualitätsunterschiede. */
        NEAR_IDENTICAL,
        /** Hamming 6–10: leichte Bearbeitung, kleines Wasserzeichen, Helligkeitskorrektur. */
        SIMILAR,
        /** Hamming 11–15: stärkeres Zuschneiden, deutlichere Bearbeitung. */
        POSSIBLY_SIMILAR,
        /** Hamming > 15: verschiedene Bilder. */
        DIFFERENT
    }

    private PerceptualHasher() {}

    /**
     * Berechnet den pHash eines Bildes als 64-Bit-Long.
     *
     * @param imagePath Pfad zur Bilddatei
     * @return pHash-Wert
     * @throws IOException wenn das Bild nicht gelesen werden kann
     * @throws UnsupportedOperationException wenn das Format nicht unterstützt wird
     */
    public static long hash(Path imagePath) throws IOException {
        BufferedImage img = ImageIO.read(imagePath.toFile());
        if (img == null) {
            throw new UnsupportedOperationException(
                    "Format nicht lesbar: " + imagePath.getFileName());
        }
        return computeHash(img);
    }

    /**
     * Berechnet den pHash eines bereits geladenen Bildes.
     */
    public static long hash(BufferedImage img) {
        return computeHash(img);
    }

    /**
     * Vergleicht zwei pHash-Werte und gibt die Ähnlichkeitsstufe zurück.
     */
    public static Similarity similarity(long hashA, long hashB) {
        int dist = hammingDistance(hashA, hashB);
        if (dist == 0)        return Similarity.IDENTICAL;
        if (dist <= 5)        return Similarity.NEAR_IDENTICAL;
        if (dist <= 10)       return Similarity.SIMILAR;
        if (dist <= 15)       return Similarity.POSSIBLY_SIMILAR;
        return                       Similarity.DIFFERENT;
    }

    /**
     * Gibt die Hamming-Distanz (Anzahl verschiedener Bits) zwischen zwei Hashes zurück.
     * 0 = identisch, 64 = komplett verschieden.
     */
    public static int hammingDistance(long hashA, long hashB) {
        return Long.bitCount(hashA ^ hashB);
    }

    /**
     * Gibt true zurück, wenn die Dateiendung ein unterstütztes Bildformat ist.
     */
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
     * Verhindert falsch-positive visuelle Treffer bei stark unterschiedlich
     * geformten Bildern (z. B. Hochformat vs. Querformat).
     */
    public static boolean aspectRatioCompatible(int wA, int hA, int wB, int hB) {
        if (wA <= 0 || hA <= 0 || wB <= 0 || hB <= 0) return true; // keine Aussage möglich
        double ratioA = (double) wA / hA;
        double ratioB = (double) wB / hB;
        double diff = Math.abs(ratioA - ratioB) / Math.max(ratioA, ratioB);
        return diff <= ASPECT_RATIO_TOLERANCE;
    }

    /**
     * Berechnet ein normalisiertes Graustufen-Histogramm eines Bildes
     * (Zusatzprüfung, um pHash-Fehlalarme bei sehr unterschiedlichen Bildern
     * mit zufällig ähnlicher DCT-Struktur auszuschließen).
     */
    public static double[] histogram(Path imagePath) throws IOException {
        BufferedImage img = ImageIO.read(imagePath.toFile());
        if (img == null)
            throw new UnsupportedOperationException("Format nicht lesbar: " + imagePath.getFileName());
        return histogram(img);
    }

    public static double[] histogram(BufferedImage img) {
        BufferedImage gray = toGrayscale(resize(img, RESIZE, RESIZE));
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

    // ── Interne Implementierung ───────────────────────────────────────────────

    private static long computeHash(BufferedImage original) {
        // 1. Graustufen + auf RESIZE×RESIZE skalieren
        BufferedImage small = toGrayscale(resize(original, RESIZE, RESIZE));

        // 2. Pixelwerte in 2D-Array
        double[][] pixels = new double[RESIZE][RESIZE];
        for (int y = 0; y < RESIZE; y++)
            for (int x = 0; x < RESIZE; x++)
                pixels[y][x] = small.getRaster().getSampleDouble(x, y, 0);

        // 3. 2D-DCT
        double[][] dct = dct2D(pixels);

        // 4. Nur oberes linkes DCT_SIZE×DCT_SIZE Fenster
        //    DC-Term (0,0) weglassen – enthält Gesamt-Helligkeit, keine Strukturinfo
        double[] dctFlat = new double[DCT_SIZE * DCT_SIZE - 1];
        int k = 0;
        for (int y = 0; y < DCT_SIZE; y++)
            for (int x = 0; x < DCT_SIZE; x++)
                if (!(x == 0 && y == 0)) dctFlat[k++] = dct[y][x];

        // 5. Median bestimmen
        double median = median(dctFlat.clone());

        // 6. Bits setzen
        long hash = 0L;
        for (int i = 0; i < 63; i++) {
            if (dctFlat[i] > median) hash |= (1L << i);
        }
        return hash;
    }

    /** Bilineares Skalieren auf targetW × targetH. */
    private static BufferedImage resize(BufferedImage src, int targetW, int targetH) {
        BufferedImage result = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // Transparente Bereiche auf Weiß statt Schwarz legen – sonst hashen zwei
        // Bilder mit unterschiedlichem, aber transparentem Inhalt gleich.
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, targetW, targetH);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return result;
    }

    /** Konvertiert in Graustufen (Luminanz-gewichtet). */
    private static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    /**
     * 2D Discrete Cosine Transform (separierbar: erst Zeilen, dann Spalten).
     * Komplexität O(N³) – für N=32 völlig ausreichend.
     */
    private static double[][] dct2D(double[][] input) {
        int N = input.length;
        double[][] temp   = new double[N][N];
        double[][] output = new double[N][N];

        // DCT entlang Zeilen
        for (int y = 0; y < N; y++) temp[y] = dct1D(input[y]);
        // DCT entlang Spalten
        for (int x = 0; x < N; x++) {
            double[] col = new double[N];
            for (int y = 0; y < N; y++) col[y] = temp[y][x];
            col = dct1D(col);
            for (int y = 0; y < N; y++) output[y][x] = col[y];
        }
        return output;
    }

    /** 1D-DCT Typ II (Standard). */
    private static double[] dct1D(double[] in) {
        int N = in.length;
        double[] out = new double[N];
        for (int k = 0; k < N; k++) {
            double sum = 0.0;
            for (int n = 0; n < N; n++)
                sum += in[n] * Math.cos(Math.PI * k * (2.0 * n + 1) / (2.0 * N));
            double alpha = (k == 0) ? Math.sqrt(1.0 / N) : Math.sqrt(2.0 / N);
            out[k] = alpha * sum;
        }
        return out;
    }

    /** Berechnet den Median eines Arrays (sortiert eine Kopie). */
    private static double median(double[] arr) {
        java.util.Arrays.sort(arr);
        int mid = arr.length / 2;
        return (arr.length % 2 == 0) ? (arr[mid - 1] + arr[mid]) / 2.0 : arr[mid];
    }
}