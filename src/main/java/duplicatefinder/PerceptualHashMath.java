package duplicatefinder;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Reine Bild- und DCT-Mathematik für {@link PerceptualHasher}.
 *
 * <h3>Ablauf</h3>
 * <ol>
 *   <li>Bild auf 32×32 Pixel skalieren (Graustufen)</li>
 *   <li>2D-DCT berechnen</li>
 *   <li>Obere linke 8×8-Ecke verwenden (niedrige Frequenzen)</li>
 *   <li>Median bestimmen, Bits gegen Median setzen</li>
 * </ol>
 */
final class PerceptualHashMath {

    private static final int RESIZE = 32;
    private static final int DCT_SIZE = 8;

    private PerceptualHashMath() {}

    static long computeHash(BufferedImage original) {
        BufferedImage small = toGrayscale(resize(original, RESIZE, RESIZE));

        double[][] pixels = new double[RESIZE][RESIZE];
        for (int y = 0; y < RESIZE; y++)
            for (int x = 0; x < RESIZE; x++)
                pixels[y][x] = small.getRaster().getSampleDouble(x, y, 0);

        double[][] dct = dct2D(pixels);

        // DC-Term (0,0) weglassen – enthält Gesamt-Helligkeit, keine Strukturinfo
        double[] dctFlat = new double[DCT_SIZE * DCT_SIZE - 1];
        int k = 0;
        for (int y = 0; y < DCT_SIZE; y++)
            for (int x = 0; x < DCT_SIZE; x++)
                if (!(x == 0 && y == 0)) dctFlat[k++] = dct[y][x];

        double median = median(dctFlat.clone());

        long hash = 0L;
        for (int i = 0; i < 63; i++) {
            if (dctFlat[i] > median) hash |= (1L << i);
        }
        return hash;
    }

    /** Bilineares Skalieren auf targetW × targetH. Transparenz wird zu Weiß aufgelöst. */
    static BufferedImage resize(BufferedImage src, int targetW, int targetH) {
        BufferedImage result = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, targetW, targetH);
        g.drawImage(src, 0, 0, targetW, targetH, null);
        g.dispose();
        return result;
    }

    static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    /** 2D-DCT, separierbar (erst Zeilen, dann Spalten). Komplexität O(N³), für N=32 ausreichend. */
    private static double[][] dct2D(double[][] input) {
        int n = input.length;
        double[][] temp   = new double[n][n];
        double[][] output = new double[n][n];

        for (int y = 0; y < n; y++) temp[y] = dct1D(input[y]);
        for (int x = 0; x < n; x++) {
            double[] col = new double[n];
            for (int y = 0; y < n; y++) col[y] = temp[y][x];
            col = dct1D(col);
            for (int y = 0; y < n; y++) output[y][x] = col[y];
        }
        return output;
    }

    /** 1D-DCT Typ II (Standard). */
    private static double[] dct1D(double[] in) {
        int n = in.length;
        double[] out = new double[n];
        for (int k = 0; k < n; k++) {
            double sum = 0.0;
            for (int i = 0; i < n; i++)
                sum += in[i] * Math.cos(Math.PI * k * (2.0 * i + 1) / (2.0 * n));
            double alpha = (k == 0) ? Math.sqrt(1.0 / n) : Math.sqrt(2.0 / n);
            out[k] = alpha * sum;
        }
        return out;
    }

    private static double median(double[] arr) {
        java.util.Arrays.sort(arr);
        int mid = arr.length / 2;
        return (arr.length % 2 == 0) ? (arr[mid - 1] + arr[mid]) / 2.0 : arr[mid];
    }
}
