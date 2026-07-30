package duplicatefinder.match;

import duplicatefinder.hash.PerceptualHasher;

import java.nio.file.Path;
import java.util.Set;

/** Ordnet Dateien anhand ihrer Endung einer {@link FileKind} zu. */
public final class FileKindClassifier {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "java", "py", "js", "ts", "json", "xml", "yaml", "yml",
            "csv", "html", "css", "properties", "sql", "log"
    );

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "mov", "avi", "mkv", "webm", "m4v", "wmv", "flv"
    );

    private FileKindClassifier() {}

    public static FileKind classify(Path file) {
        String extension = extractExtension(file);
        if (PerceptualHasher.SUPPORTED_EXTENSIONS.contains(extension)) return FileKind.IMAGE;
        if (VIDEO_EXTENSIONS.contains(extension))                      return FileKind.VIDEO;
        if (TEXT_EXTENSIONS.contains(extension))                       return FileKind.TEXT;
        return FileKind.BINARY;
    }

    private static String extractExtension(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }
}