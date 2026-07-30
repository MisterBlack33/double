package duplicatefinder.scan;

import duplicatefinder.match.CopySuffixDetector;

import java.nio.file.Path;
import java.util.*;

/**
 * Unveränderliches Ergebnisobjekt eines Scan-Durchlaufs.
 *
 * <p>Kapselt alle Duplikat-Gruppen und vorberechnete Statistiken,
 * damit GUI und CLI dieselbe Datenquelle nutzen.
 */
public final class ScanResult {

    private final List<DuplicateGroup> groups;
    private final int  totalFilesScanned;
    private final long totalWastedBytes;

    public ScanResult(List<DuplicateGroup> groups, int totalFilesScanned) {
        this.groups            = Collections.unmodifiableList(new ArrayList<>(groups));
        this.totalFilesScanned = totalFilesScanned;
        this.totalWastedBytes  = groups.stream().mapToLong(DuplicateGroup::wastedBytes).sum();
    }

    public List<DuplicateGroup> getGroups()           { return groups; }
    public int  getTotalFilesScanned()                { return totalFilesScanned; }
    public int  getDuplicateGroupCount()              { return groups.size(); }
    public long getTotalWastedBytes()                 { return totalWastedBytes; }
    public boolean hasDuplicates()                    { return !groups.isEmpty(); }

    /** Gesamtanzahl redundanter Dateien (Originale abgezogen). */
    public int getRedundantFileCount() {
        return groups.stream().mapToInt(g -> g.getPaths().size() - 1).sum();
    }

    /** Eine Gruppe inhaltlich identischer Dateien. */
    public static final class DuplicateGroup {

        private final String     hash;
        private final List<Path> paths;
        private final long       fileSize;

        public DuplicateGroup(String hash, List<Path> paths, long fileSize) {
            this.hash     = hash;
            this.paths    = Collections.unmodifiableList(sortOriginalFirst(paths));
            this.fileSize = fileSize;
        }

        /**
         * Original zuerst: 1) keine Kopie-Suffix ("foto.jpg" vor "foto (1).jpg"),
         * 2) bei Gleichstand die ältere Datei (früheres Änderungsdatum).
         */
        private static List<Path> sortOriginalFirst(List<Path> paths) {
            List<Path> sorted = new ArrayList<>(paths);
            sorted.sort(Comparator.comparing(CopySuffixDetector::hasCopySuffix)
                    .thenComparing(FileTimestamps::lastModifiedMillis));
            return sorted;
        }

        public String     getHash()       { return hash; }
        public List<Path> getPaths()      { return paths; }
        public long       getFileSize()   { return fileSize; }

        /** Verschwendeter Speicher = Dateigröße × (Anzahl − 1). */
        public long wastedBytes() {
            return fileSize * (paths.size() - 1);
        }
    }
}
