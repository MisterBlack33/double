// src/main/java/duplicatefinder/quality/OriginalOrderSorter.java
package duplicatefinder.quality;

import duplicatefinder.match.CopySuffixDetector;
import duplicatefinder.match.FileKind;
import duplicatefinder.match.FileKindClassifier;
import duplicatefinder.scan.FileTimestamps;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sortiert Duplikat-Kandidaten für die Original/Duplikat-Anzeige: bei Bild-, Video- und
 * Audiodateien entscheidet zuerst die höhere Qualität/Auflösung. Nur bei Gleichstand greift
 * die klassische Original-Erkennung (Kopie-Suffix, ältestes Datum).
 */
public final class OriginalOrderSorter {

    private OriginalOrderSorter() {}

    public static List<Path> sort(List<Path> paths) {
        return sort(paths, new DefaultResolutionReader());
    }

    static List<Path> sort(List<Path> paths, ResolutionReader resolutionReader) {
        if (paths.isEmpty()) return new ArrayList<>();

        FileKind kind = FileKindClassifier.classify(paths.get(0));
        List<Path> sorted = new ArrayList<>(paths);
        sorted.sort(qualityComparator(kind, resolutionReader)
                .thenComparing(CopySuffixDetector::hasCopySuffix)
                .thenComparing(FileTimestamps::lastModifiedMillis));
        return sorted;
    }

    private static Comparator<Path> qualityComparator(FileKind kind, ResolutionReader reader) {
        return switch (kind) {
            case IMAGE, VIDEO -> Comparator.comparingLong(reader::pixelCount).reversed();
            case AUDIO        -> AudioQualityComparator.INSTANCE;
            default           -> (a, b) -> 0;
        };
    }
}