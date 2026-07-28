package duplicatefinder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Ergebnis eines Ordner-Vergleichs.
 *
 * <h3>Byte-Vergleich (alle Dateitypen)</h3>
 * <pre>
 *  Fall  Name  Inhalt  Größe  → Status
 *   1     =      =      =     DUPLICATE
 *   2     =      =      ≠     NEEDS_REVIEW
 *   3     =      ≠      =     CONFLICT
 *   4     =      ≠      ≠     DIFFERENT  (ignoriert)
 *   5     ≠      =      =     NEEDS_REVIEW
 *   6     ≠      =      ≠     NEEDS_REVIEW
 *   7     ≠      ≠      =     DIFFERENT  (ignoriert)
 *   8     ≠      ≠      ≠     DIFFERENT  (ignoriert)
 * </pre>
 *
 * <h3>Zusätzlich für Bilder (pHash)</h3>
 * <pre>
 *  VISUAL_IDENTICAL      pHash-Distanz 0    – gleich nach Format-Konversion / EXIF-Strip
 *  VISUAL_NEAR_IDENTICAL pHash-Distanz 1–5  – Kompressionsartefakte, andere JPEG-Qualität
 *  VISUAL_SIMILAR        pHash-Distanz 6–10 – Helligkeit, kleines Wasserzeichen
 *  VISUAL_POSSIBLY       pHash-Distanz 11–15– Zuschneiden, stärkere Bearbeitung
 * </pre>
 */
public final class FolderSyncResult {

    public enum MatchStatus {
        // ── Byte-basiert ──────────────────────────────────────────────────────
        /** Fall 1: Name = Inhalt = Größe = → sicher löschbar. */
        DUPLICATE,
        /** Fälle 2, 5, 6: Inhalt gleich, aber Name oder Größe weichen ab. */
        NEEDS_REVIEW,
        /** Fall 3: Name + Größe gleich, Inhalt verschieden → Konflikt. */
        CONFLICT,
        /** Fälle 4, 7, 8: kein relevanter Treffer → wird nicht angezeigt. */
        DIFFERENT,

        // ── Visuell (nur Bilder, pHash) ───────────────────────────────────────
        /** pHash-Distanz 0: visuell identisch, unterschiedliches Format/EXIF. */
        VISUAL_IDENTICAL,
        /** pHash-Distanz 1–5: fast identisch (Kompression, leichte Qualitätsunterschiede). */
        VISUAL_NEAR_IDENTICAL,
        /** pHash-Distanz 6–10: ähnlich (Helligkeit, Wasserzeichen, leichte Bearbeitung). */
        VISUAL_SIMILAR,
        /** pHash-Distanz 11–15: möglicherweise ähnlich (Zuschneiden, stärkere Bearbeitung). */
        VISUAL_POSSIBLY_SIMILAR
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