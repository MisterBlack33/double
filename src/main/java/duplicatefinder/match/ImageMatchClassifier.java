package duplicatefinder.match;

import java.nio.file.Path;

/** Ordnet einen Bild-pHash-Treffer einem konkreten {@link MatchReason} zu. */
public final class ImageMatchClassifier {

    private ImageMatchClassifier() {}

    public static MatchReason classify(Path a, Path b, int hammingDistance) {
        boolean sameFormat = extension(a).equals(extension(b));
        if (hammingDistance == 0) {
            return sameFormat ? MatchReason.EXACT : MatchReason.FORMAT_VARIANT;
        }
        return sameFormat ? MatchReason.RESOLUTION_VARIANT : MatchReason.FORMAT_VARIANT;
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }
}
