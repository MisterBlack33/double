package duplicatefinder.match;

import java.nio.file.Path;
import java.util.regex.Pattern;

/** Erkennt Dateinamen mit typischem Kopie-Suffix (z. B. "foto (1).jpg", "foto - Kopie.jpg"). */
public final class CopySuffixDetector {

    private static final Pattern COPY_SUFFIX = Pattern.compile(
            "^.+\\s\\(\\d+\\)\\.[^.]+$"
                    + "|^.+\\s-\\s[Kk]opie(\\s\\(\\d+\\))?\\.[^.]+$"
                    + "|^.+\\s[Cc]opy(\\s\\(\\d+\\))?\\.[^.]+$"
    );

    private CopySuffixDetector() {}

    public static boolean hasCopySuffix(Path file) {
        return COPY_SUFFIX.matcher(file.getFileName().toString()).matches();
    }
}