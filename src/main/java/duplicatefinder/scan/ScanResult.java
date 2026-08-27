package duplicatefinder.scan;

import duplicatefinder.match.CopySuffixDetector;

import java.nio.file.Path;
import java.util.*;

/**
 * Unveränderliches Ergebnisobjekt eines Scan-Durchlaufs.
 *
 * <p>Kapselt Byte-Duplikat-Gruppen, Namenskollisionen, visuelle und Audio-Duplikate,
 * damit GUI und CLI dieselbe Datenquelle nutzen.
 */
public final class ScanResult {

    private final List<DuplicateGroup>       groups;
    private final List<NameCollisionGroup>   nameCollisions;
    private final List<VisualDuplicateGroup> visualDuplicates;
    private final List<AudioDuplicateGroup>  audioDuplicates;
    private final int  totalFilesScanned;
    private final long totalWastedBytes;

    public ScanResult(List<DuplicateGroup> groups, int totalFilesScanned) {
        this(groups, totalFilesScanned, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    public ScanResult(List<DuplicateGroup> groups, int totalFilesScanned,
                      List<NameCollisionGroup> nameCollisions) {
        this(groups, totalFilesScanned, nameCollisions, Collections.emptyList(), Collections.emptyList());
    }

    public ScanResult(List<DuplicateGroup> groups, int totalFilesScanned,
                      List<NameCollisionGroup> nameCollisions, List<VisualDuplicateGroup> visualDuplicates) {
        this(groups, totalFilesScanned, nameCollisions, visualDuplicates, Collections.emptyList());
    }

    public ScanResult(List<DuplicateGroup> groups, int totalFilesScanned,
                      List<NameCollisionGroup> nameCollisions, List<VisualDuplicateGroup> visualDuplicates,
                      List<AudioDuplicateGroup> audioDuplicates) {
        this.groups            = Collections.unmodifiableList(new ArrayList<>(groups));
        this.nameCollisions    = Collections.unmodifiableList(new ArrayList<>(nameCollisions));
        this.visualDuplicates  = Collections.unmodifiableList(new ArrayList<>(visualDuplicates));
        this.audioDuplicates   = Collections.unmodifiableList(new ArrayList<>(audioDuplicates));
        this.totalFilesScanned = totalFilesScanned;
        this.totalWastedBytes  = groups.stream().mapToLong(DuplicateGroup::wastedBytes).sum();
    }

    public List<DuplicateGroup>       getGroups()           { return groups; }
    public List<NameCollisionGroup>   getNameCollisions()   { return nameCollisions; }
    public List<VisualDuplicateGroup> getVisualDuplicates() { return visualDuplicates; }
    public List<AudioDuplicateGroup>  getAudioDuplicates()  { return audioDuplicates; }
    public boolean hasNameCollisions()                      { return !nameCollisions.isEmpty(); }
    public boolean hasVisualDuplicates()                    { return !visualDuplicates.isEmpty(); }
    public boolean hasAudioDuplicates()                     { return !audioDuplicates.isEmpty(); }
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