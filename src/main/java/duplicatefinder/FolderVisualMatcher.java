package duplicatefinder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Visuelle (pHash) und textuelle (SimHash) Ähnlichkeitsprüfung für den Ordnervergleich.
 * Wird von {@link FolderComparator} für die Fälle 3/4/5–8 genutzt.
 */
final class FolderVisualMatcher {

    private static final int TEXT_SIMHASH_THRESHOLD = 6;

    private FolderVisualMatcher() {}

    /** Vergleicht zwei Bilddateien per pHash; null wenn kein plausibler visueller Treffer. */
    static FolderSyncResult.FileEntry visualEntry(Path src, Path tgt, long sizeSrc, long sizeTgt) {
        try {
            if (PerceptualHasher.isAnimatedGif(src) || PerceptualHasher.isAnimatedGif(tgt)) return null;

            int[] dimSrc = PerceptualHasher.readDimensions(src);
            int[] dimTgt = PerceptualHasher.readDimensions(tgt);
            if (!PerceptualHasher.aspectRatioCompatible(dimSrc[0], dimSrc[1], dimTgt[0], dimTgt[1]))
                return null;

            long hashSrc = PerceptualHasher.hash(src);
            long hashTgt = PerceptualHasher.hash(tgt);
            int  dist    = PerceptualHasher.hammingDistance(hashSrc, hashTgt);
            FolderSyncResult.MatchStatus ms = similarityToStatus(PerceptualHasher.similarity(hashSrc, hashTgt));
            if (ms == null) return null;

            if (!PerceptualHasher.histogramsPlausiblySimilar(
                    PerceptualHasher.histogram(src), PerceptualHasher.histogram(tgt))) return null;

            return new FolderSyncResult.FileEntry(src, tgt, sizeSrc, sizeTgt, ms, dist);
        } catch (Exception e) {
            return null;
        }
    }

    /** Sucht im pHash-Index den besten visuellen Treffer (kleinste Hamming-Distanz) für eine Quelldatei. */
    static FolderSyncResult.FileEntry findBestVisualMatch(Path src, long sizeSrc, Map<Long, List<Path>> tgtByPHash) {
        if (PerceptualHasher.isAnimatedGif(src)) return null;
        try {
            long srcHash = PerceptualHasher.hash(src);
            int[] dimSrc = PerceptualHasher.readDimensions(src);
            Path bestMatch = findClosestCandidate(src, srcHash, dimSrc, tgtByPHash);
            if (bestMatch == null) return null;

            if (!PerceptualHasher.histogramsPlausiblySimilar(
                    PerceptualHasher.histogram(src), PerceptualHasher.histogram(bestMatch))) return null;

            FolderSyncResult.MatchStatus ms = similarityToStatus(
                    PerceptualHasher.similarity(srcHash, PerceptualHasher.hash(bestMatch)));
            if (ms == null) return null;
            return new FolderSyncResult.FileEntry(src, bestMatch, sizeSrc, FolderFileIndexer.safeSize(bestMatch),
                    ms, PerceptualHasher.hammingDistance(srcHash, PerceptualHasher.hash(bestMatch)));
        } catch (Exception e) {
            return null;
        }
    }

    private static Path findClosestCandidate(Path src, long srcHash, int[] dimSrc,
                                              Map<Long, List<Path>> tgtByPHash) throws Exception {
        int bestDist = Integer.MAX_VALUE;
        Path bestMatch = null;
        for (Map.Entry<Long, List<Path>> e : tgtByPHash.entrySet()) {
            int dist = PerceptualHasher.hammingDistance(srcHash, e.getKey());
            if (dist >= bestDist || dist > 15) continue;

            Path candidate = e.getValue().get(0);
            if (PerceptualHasher.isAnimatedGif(candidate)) continue;
            int[] dimTgt = PerceptualHasher.readDimensions(candidate);
            if (!PerceptualHasher.aspectRatioCompatible(dimSrc[0], dimSrc[1], dimTgt[0], dimTgt[1])) continue;

            bestDist = dist;
            bestMatch = candidate;
        }
        return bestMatch;
    }

    /** Vergleicht zwei Textdateien per SimHash; null wenn Distanz über dem Schwellwert liegt. */
    static FolderSyncResult.FileEntry textNearDuplicateEntry(Path src, Path tgt, long sizeSrc, long sizeTgt) {
        try {
            long hashSrc = SimHasher.hash(src);
            long hashTgt = SimHasher.hash(tgt);
            int dist = SimHasher.hammingDistance(hashSrc, hashTgt);
            if (dist > TEXT_SIMHASH_THRESHOLD) return null;
            return new FolderSyncResult.FileEntry(src, tgt, sizeSrc, sizeTgt,
                    FolderSyncResult.MatchStatus.NEAR_DUPLICATE_TEXT, dist);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * Klassifiziert ein Dateipaar mit gleichem Namen, aber ungleichem Byte-Inhalt (Fälle 3/4):
     * versucht zunächst einen visuellen Treffer (Bilder), dann einen SimHash-Treffer (Text).
     */
    static FolderSyncResult.FileEntry classifyNonByteMatch(Path absSrc, Path absTgt,
                                                           long sizeSrc, long sizeTgt, boolean srcIsImage) {
        if (srcIsImage && PerceptualHasher.isImage(absTgt)) {
            FolderSyncResult.FileEntry ve = visualEntry(absSrc, absTgt, sizeSrc, sizeTgt);
            return (ve != null && ve.isVisualMatch()) ? ve : null;
        }
        if (FileKindClassifier.classify(absSrc) == FileKind.TEXT
                && FileKindClassifier.classify(absTgt) == FileKind.TEXT) {
            return textNearDuplicateEntry(absSrc, absTgt, sizeSrc, sizeTgt);
        }
        return null;
    }

    private static FolderSyncResult.MatchStatus similarityToStatus(PerceptualHasher.Similarity sim) {
        return switch (sim) {
            case IDENTICAL         -> FolderSyncResult.MatchStatus.VISUAL_IDENTICAL;
            case NEAR_IDENTICAL    -> FolderSyncResult.MatchStatus.VISUAL_NEAR_IDENTICAL;
            case SIMILAR           -> FolderSyncResult.MatchStatus.VISUAL_SIMILAR;
            case POSSIBLY_SIMILAR  -> FolderSyncResult.MatchStatus.VISUAL_POSSIBLY_SIMILAR;
            case DIFFERENT         -> null;
        };
    }
}
