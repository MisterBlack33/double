package duplicatefinder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SimHash-basierte Ähnlichkeitserkennung für Text-/Code-Dateien.
 * Erkennt inhaltlich fast identische Dateien (z. B. andere Zeilenenden,
 * einzelne geänderte Zeilen), die kein exakter SHA-256-Treffer mehr sind.
 */
public final class SimHasher {

    private static final int SHINGLE_LENGTH = 5;
    private static final int HASH_BITS = 64;
    private static final int MAX_FILE_BYTES_FOR_TEXT_COMPARE = 5_000_000;

    private SimHasher() {}

    public static long hash(Path file) throws IOException {
        String normalized = readNormalized(file);
        int[] bitWeights = new int[HASH_BITS];

        for (String shingle : shingles(normalized)) {
            long shingleHash = shingle.hashCode() * 0x9E3779B97F4A7C15L + shingle.length();
            accumulateWeights(bitWeights, shingleHash);
        }
        return buildHashFromWeights(bitWeights);
    }

    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    private static String readNormalized(Path file) throws IOException {
        if (Files.size(file) > MAX_FILE_BYTES_FOR_TEXT_COMPARE) {
            throw new IOException("Datei zu groß für Textvergleich: " + file.getFileName());
        }
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        return raw.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private static java.util.List<String> shingles(String text) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (int i = 0; i + SHINGLE_LENGTH <= text.length(); i++) {
            result.add(text.substring(i, i + SHINGLE_LENGTH));
        }
        return result.isEmpty() ? java.util.List.of(text) : result;
    }

    private static void accumulateWeights(int[] weights, long shingleHash) {
        for (int bit = 0; bit < HASH_BITS; bit++) {
            boolean bitSet = ((shingleHash >>> bit) & 1L) == 1L;
            weights[bit] += bitSet ? 1 : -1;
        }
    }

    private static long buildHashFromWeights(int[] weights) {
        long hash = 0L;
        for (int bit = 0; bit < HASH_BITS; bit++) {
            if (weights[bit] > 0) hash |= (1L << bit);
        }
        return hash;
    }
}
