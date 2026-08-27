package duplicatefinder.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Ordnet Audio-Duplikat-Kandidaten nach Qualität absteigend: verlustfrei zuerst,
 * dann höhere Bitrate, dann kleinere Dateigröße als Tie-Breaker.
 * Bitrate-Ermittlung ist per {@link BitrateReader} austauschbar (Dependency Injection),
 * damit Tests ohne echte Audiodateien auskommen.
 */
public final class AudioQualityRanker {

    /** Liest die Bitrate (kbps) einer Audiodatei; 0 bei Fehler/unbekannt. */
    public interface BitrateReader {
        int bitrateKbps(Path file);
    }

    private static final Set<String> LOSSLESS_EXTENSIONS = Set.of("flac", "wav", "alac", "aiff", "ape");

    private AudioQualityRanker() {}

    public static List<Path> rankByQuality(List<Path> candidates) {
        return rankByQuality(candidates, TikaBitrateReader::read);
    }

    public static List<Path> rankByQuality(List<Path> candidates, BitrateReader reader) {
        List<Path> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator
                .comparing(AudioQualityRanker::isLossless).reversed()
                .thenComparing((Path p) -> reader.bitrateKbps(p), Comparator.reverseOrder())
                .thenComparing(AudioQualityRanker::fileSize));
        return sorted;
    }

    static boolean isLossless(Path file) {
        return LOSSLESS_EXTENSIONS.contains(extension(file));
    }

    private static String extension(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    private static long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }
}