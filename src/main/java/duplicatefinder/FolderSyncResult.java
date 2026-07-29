package duplicatefinder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Ergebnis eines Ordner-Vergleichs (byte-basiert, plus optional pHash/SimHash).
 * Siehe {@link FolderComparator} für die vollständige Fallunterscheidung.
 */
public final class FolderSyncResult {

    public enum MatchStatus {
        DUPLICATE, NEEDS_REVIEW, CONFLICT, DIFFERENT,
        /** Textdateien mit gleichem Namen, ungleichem Inhalt, aber geringer SimHash-Distanz. */
        NEAR_DUPLICATE_TEXT,
        VISUAL_IDENTICAL, VISUAL_NEAR_IDENTICAL, VISUAL_SIMILAR, VISUAL_POSSIBLY_SIMILAR
    }

    /** Ein Vergleichspaar: Quelldatei + passende Zieldatei + Bewertung. */
    public static final class FileEntry {
        private final Path        sourcePath;
        private final Path        targetPath;
        private final long        sourceSize;
        private final long        targetSize;
        private final MatchStatus status;
        /** pHash-Hamming-Distanz; -1 wenn kein visueller Vergleich durchgeführt wurde. */
        private final int         hammingDistance;

        public FileEntry(Path sourcePath, Path targetPath,
                         long sourceSize, long targetSize,
                         MatchStatus status, int hammingDistance) {
            this.sourcePath      = sourcePath;
            this.targetPath      = targetPath;
            this.sourceSize      = sourceSize;
            this.targetSize      = targetSize;
            this.status          = status;
            this.hammingDistance = hammingDistance;
        }

        public Path        getSourcePath()      { return sourcePath; }
        public Path        getTargetPath()      { return targetPath; }
        public long        getSourceSize()      { return sourceSize; }
        public long        getTargetSize()      { return targetSize; }
        public MatchStatus getStatus()          { return status; }
        public int         getHammingDistance() { return hammingDistance; }

        public boolean isDuplicate()     { return status == MatchStatus.DUPLICATE; }
        public boolean needsReview()     { return status == MatchStatus.NEEDS_REVIEW; }
        public boolean isConflict()      { return status == MatchStatus.CONFLICT; }
        public boolean isVisualMatch()   {
            return status == MatchStatus.VISUAL_IDENTICAL
                    || status == MatchStatus.VISUAL_NEAR_IDENTICAL
                    || status == MatchStatus.VISUAL_SIMILAR
                    || status == MatchStatus.VISUAL_POSSIBLY_SIMILAR;
        }
    }

    private final Path            sourceDir;
    private final Path            targetDir;
    private final List<FileEntry> entries;
    private final int             totalSourceFiles;
    private final int             totalTargetFiles;
    private final int             differentCount;

    public FolderSyncResult(Path sourceDir, Path targetDir, List<FileEntry> entries,
                            int totalSourceFiles, int totalTargetFiles, int differentCount) {
        this.sourceDir        = sourceDir;
        this.targetDir        = targetDir;
        this.entries          = Collections.unmodifiableList(entries);
        this.totalSourceFiles = totalSourceFiles;
        this.totalTargetFiles = totalTargetFiles;
        this.differentCount   = differentCount;
    }

    public Path            getSourceDir()        { return sourceDir; }
    public Path            getTargetDir()        { return targetDir; }
    public List<FileEntry> getEntries()          { return entries; }
    public int             getTotalSourceFiles() { return totalSourceFiles; }
    public int             getTotalTargetFiles() { return totalTargetFiles; }
    public int             getDifferentCount()   { return differentCount; }

    public long countByStatus(MatchStatus s) {
        return entries.stream().filter(e -> e.getStatus() == s).count();
    }

    public long countVisual() {
        return entries.stream().filter(FileEntry::isVisualMatch).count();
    }
}
