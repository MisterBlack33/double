package duplicatefinder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;

/**
 * Vergleicht zwei Ordner nach der 8-Fall-Spezifikation (byte-basiert) und ergänzt
 * für Bilder einen pHash-Vergleich sowie für Textdateien einen SimHash-Vergleich.
 *
 * <h3>Byte-Vergleich (alle Dateitypen)</h3>
 * <pre>
 *  Fall  Name  Inhalt  Größe  → MatchStatus
 *   1     =      =      =     DUPLICATE
 *   2     =      =      ≠     NEEDS_REVIEW
 *   3     =      ≠      =     CONFLICT (oder VISUAL_x / NEAR_DUPLICATE_TEXT)
 *   4     =      ≠      ≠     DIFFERENT (oder VISUAL_x / NEAR_DUPLICATE_TEXT)
 *   5     ≠      =      =     NEEDS_REVIEW
 *   6     ≠      =      ≠     NEEDS_REVIEW
 *   7/8   ≠      beliebig     DIFFERENT (oder VISUAL_* bei Bildern)
 * </pre>
 * Detaillierte Ähnlichkeitslogik siehe {@link FolderVisualMatcher}, Indexaufbau siehe
 * {@link FolderFileIndexer}.
 */
public class FolderComparator {

    /**
     * Vergleicht Quell- und Zielordner vollständig.
     *
     * @param visualCompare wenn true, pHash-Vergleich für Bilder aktivieren
     */
    public FolderSyncResult compare(Path sourceDir, Path targetDir, boolean visualCompare,
                                    BiConsumer<Integer, Integer> progressCallback) throws IOException {

        Map<Path, Path> srcMap = FolderFileIndexer.collectFiles(sourceDir);
        Map<Path, Path> tgtMap = FolderFileIndexer.collectFiles(targetDir);

        Map<String, List<Path>> tgtByHash = FolderFileIndexer.buildHashIndex(tgtMap.values(),
                (d, t) -> reportProgress(progressCallback, d, t + srcMap.size()));

        Map<Long, List<Path>> tgtByPHash = visualCompare
                ? FolderFileIndexer.buildPHashIndex(tgtMap.values(),
                        (d, t) -> reportProgress(progressCallback, tgtMap.size() + d, tgtMap.size() + srcMap.size()))
                : Collections.emptyMap();

        List<FolderSyncResult.FileEntry> entries = new ArrayList<>();
        int differentCount = comparePairs(srcMap, tgtMap, tgtByHash, tgtByPHash, visualCompare,
                entries, progressCallback);

        entries.sort(Comparator.comparingInt(e -> statusOrder(e.getStatus())));
        return new FolderSyncResult(sourceDir, targetDir, entries, srcMap.size(), tgtMap.size(), differentCount);
    }

    private int comparePairs(Map<Path, Path> srcMap, Map<Path, Path> tgtMap,
                             Map<String, List<Path>> tgtByHash, Map<Long, List<Path>> tgtByPHash,
                             boolean visualCompare, List<FolderSyncResult.FileEntry> entries,
                             BiConsumer<Integer, Integer> progressCallback) throws IOException {
        int differentCount = 0;
        int done = 0;
        for (Map.Entry<Path, Path> srcEntry : srcMap.entrySet()) {
            done++;
            reportProgress(progressCallback, tgtMap.size() + done, tgtMap.size() + srcMap.size());

            Path absSrc = srcEntry.getValue();
            boolean srcIsImage = visualCompare && PerceptualHasher.isImage(absSrc);
            Path absTgt = tgtMap.get(srcEntry.getKey());

            if (absTgt != null) {
                differentCount += compareSameName(absSrc, absTgt, srcIsImage, entries);
            } else {
                differentCount += compareDifferentName(absSrc, srcIsImage, tgtByHash, tgtByPHash, entries);
            }
        }
        return differentCount;
    }

    /** Fälle 1–4: gleicher relativer Pfad. Gibt 1 zurück wenn als DIFFERENT ignoriert. */
    private int compareSameName(Path absSrc, Path absTgt, boolean srcIsImage,
                                List<FolderSyncResult.FileEntry> entries) throws IOException {
        long sizeSrc = FolderFileIndexer.safeSize(absSrc);
        long sizeTgt = FolderFileIndexer.safeSize(absTgt);
        String hashSrc = FolderFileIndexer.sha256(absSrc);
        String hashTgt = FolderFileIndexer.sha256(absTgt);
        boolean contentEq = hashSrc.equals(hashTgt);
        boolean sizeEq = sizeSrc == sizeTgt;

        if (contentEq && sizeEq) {
            entries.add(entry(absSrc, absTgt, sizeSrc, sizeTgt, FolderSyncResult.MatchStatus.DUPLICATE, -1));
            return 0;
        }
        if (contentEq) {
            entries.add(entry(absSrc, absTgt, sizeSrc, sizeTgt, FolderSyncResult.MatchStatus.NEEDS_REVIEW, -1));
            return 0;
        }

        FolderSyncResult.FileEntry ve =
                FolderVisualMatcher.classifyNonByteMatch(absSrc, absTgt, sizeSrc, sizeTgt, srcIsImage);
        if (ve != null) {
            entries.add(ve);
            return 0;
        }
        if (sizeEq) {
            entries.add(entry(absSrc, absTgt, sizeSrc, sizeTgt, FolderSyncResult.MatchStatus.CONFLICT, -1));
            return 0;
        }
        return 1; // Fall 4, normalerweise ignoriert
    }

    /** Fälle 5–8: kein gleichnamiger Treffer im Ziel. Gibt 1 zurück wenn als DIFFERENT gezählt. */
    private int compareDifferentName(Path absSrc, boolean srcIsImage, Map<String, List<Path>> tgtByHash,
                                     Map<Long, List<Path>> tgtByPHash,
                                     List<FolderSyncResult.FileEntry> entries) throws IOException {
        long sizeSrc = FolderFileIndexer.safeSize(absSrc);
        String hashSrc = FolderFileIndexer.sha256(absSrc);
        List<Path> byteMatches = tgtByHash.getOrDefault(hashSrc, Collections.emptyList());

        if (!byteMatches.isEmpty()) {
            for (Path match : byteMatches) {
                entries.add(entry(absSrc, match, sizeSrc, FolderFileIndexer.safeSize(match),
                        FolderSyncResult.MatchStatus.NEEDS_REVIEW, -1));
            }
            return 0;
        }
        if (srcIsImage) {
            FolderSyncResult.FileEntry best = FolderVisualMatcher.findBestVisualMatch(absSrc, sizeSrc, tgtByPHash);
            if (best != null) {
                entries.add(best);
                return 0;
            }
        }
        return 1;
    }

    private void reportProgress(BiConsumer<Integer, Integer> cb, int done, int total) {
        if (cb != null) cb.accept(done, total);
    }

    private FolderSyncResult.FileEntry entry(Path src, Path tgt, long ss, long ts,
                                             FolderSyncResult.MatchStatus status, int hammingDist) {
        return new FolderSyncResult.FileEntry(src, tgt, ss, ts, status, hammingDist);
    }

    private int statusOrder(FolderSyncResult.MatchStatus s) {
        return switch (s) {
            case DUPLICATE               -> 0;
            case NEEDS_REVIEW            -> 1;
            case CONFLICT                -> 2;
            case NEAR_DUPLICATE_TEXT     -> 3;
            case VISUAL_IDENTICAL        -> 4;
            case VISUAL_NEAR_IDENTICAL   -> 5;
            case VISUAL_SIMILAR          -> 6;
            case VISUAL_POSSIBLY_SIMILAR -> 7;
            case DIFFERENT               -> 8;
        };
    }
}
