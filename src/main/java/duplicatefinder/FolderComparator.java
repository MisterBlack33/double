package duplicatefinder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Vergleicht zwei Ordner nach der 8-Fall-Spezifikation (byte-basiert)
 * und ergänzt für Bilder einen optionalen pHash-Vergleich sowie für
 * Textdateien einen SimHash-basierten Nah-Duplikat-Vergleich.
 *
 * <h3>Byte-Vergleich (alle Dateitypen)</h3>
 * <pre>
 *  Fall  Name  Inhalt  Größe  → MatchStatus
 *   1     =      =      =     DUPLICATE
 *   2     =      =      ≠     NEEDS_REVIEW
 *   3     =      ≠      =     CONFLICT
 *   4     =      ≠      ≠     DIFFERENT  (ignoriert)
 *   5     ≠      =      =     NEEDS_REVIEW
 *   6     ≠      =      ≠     NEEDS_REVIEW
 *   7     ≠      ≠      =     DIFFERENT
 *   8     ≠      ≠      ≠     DIFFERENT
 * </pre>
 *
 * <h3>pHash-Erweiterung (nur Bilder)</h3>
 * Fälle 3, 4, 7, 8 (byte-basiert DIFFERENT/CONFLICT) werden für Bilddateien
 * zusätzlich per pHash verglichen. Ergibt sich eine visuelle Ähnlichkeit,
 * wird der Eintrag als VISUAL_* statt DIFFERENT/CONFLICT gewertet.
 *
 * Ebenso werden Bildpaare aus verschiedenen Formaten (z.B. .jpg vs .png)
 * per pHash verglichen und als VISUAL_* gemeldet.
 *
 * <h3>SimHash-Erweiterung (nur Textdateien)</h3>
 * Fälle 3 und 4 (gleicher Name, ungleicher Inhalt) werden für Textdateien
 * zusätzlich per SimHash verglichen. Liegt die Hamming-Distanz unter dem
 * Schwellwert, wird der Eintrag als NEAR_DUPLICATE_TEXT statt
 * CONFLICT/DIFFERENT gewertet.
 */
public class FolderComparator {

    private static final int BUF = 65_536;
    private static final int TEXT_SIMHASH_THRESHOLD = 6; // von 64 Bit Hamming-Distanz

    /**
     * Vergleicht Quell- und Zielordner vollständig.
     *
     * @param sourceDir        Quellordner
     * @param targetDir        Zielordner
     * @param visualCompare    wenn true, pHash-Vergleich für Bilder aktivieren
     * @param progressCallback optional: (fertig, gesamt) → void
     */
    public FolderSyncResult compare(Path sourceDir, Path targetDir,
                                    boolean visualCompare,
                                    BiConsumer<Integer, Integer> progressCallback)
            throws IOException {

        // ── Dateien einlesen ──────────────────────────────────────────────────
        Map<Path, Path> srcMap = collectFiles(sourceDir);   // relPath → absPath
        Map<Path, Path> tgtMap = collectFiles(targetDir);

        // SHA-256-Index aller Zieldateien: hash → Liste von Pfaden
        Map<String, List<Path>> tgtByHash = buildHashIndex(tgtMap.values(),
                (d, t) -> {
                    if (progressCallback != null)
                        progressCallback.accept(d, t + srcMap.size());
                });

        // pHash-Index aller Bild-Zieldateien: pHash → Liste von Pfaden
        Map<Long, List<Path>> tgtByPHash = visualCompare
                ? buildPHashIndex(tgtMap.values(),
                (d, t) -> {
                    if (progressCallback != null)
                        progressCallback.accept(tgtMap.size() + d,
                                tgtMap.size() + srcMap.size());
                })
                : Collections.emptyMap();

        List<FolderSyncResult.FileEntry> entries = new ArrayList<>();
        int differentCount = 0;
        int total = srcMap.size(), done = 0;

        for (Map.Entry<Path, Path> srcEntry : srcMap.entrySet()) {
            Path relSrc  = srcEntry.getKey();
            Path absSrc  = srcEntry.getValue();
            long sizeSrc = safeSize(absSrc);
            boolean srcIsImage = visualCompare && PerceptualHasher.isImage(absSrc);

            done++;
            if (progressCallback != null)
                progressCallback.accept(tgtMap.size() + done, tgtMap.size() + total);

            boolean nameMatch = tgtMap.containsKey(relSrc);
            Path    absTgt    = nameMatch ? tgtMap.get(relSrc) : null;
            long    sizeTgt   = absTgt != null ? safeSize(absTgt) : -1;

            if (nameMatch) {
                // ── Fälle 1–4: gleicher rel. Pfad ────────────────────────────
                String hashSrc    = sha256(absSrc);
                String hashTgt    = sha256(absTgt);
                boolean contentEq = hashSrc.equals(hashTgt);
                boolean sizeEq    = sizeSrc == sizeTgt;

                if (contentEq && sizeEq) {
                    // Fall 1 → DUPLICATE
                    entries.add(entry(absSrc, absTgt, sizeSrc, sizeTgt,
                            FolderSyncResult.MatchStatus.DUPLICATE, -1));

                } else if (contentEq) {
                    // Fall 2 → NEEDS_REVIEW
                    entries.add(entry(absSrc, absTgt, sizeSrc, sizeTgt,
                            FolderSyncResult.MatchStatus.NEEDS_REVIEW, -1));

                } else if (sizeEq) {
                    // Fall 3: Name= Inhalt≠ Größe= → CONFLICT, Bild oder Text?
                    FolderSyncResult.FileEntry ve = classifyNonByteMatch(absSrc, absTgt, sizeSrc, sizeTgt, srcIsImage);
                    entries.add(ve != null ? ve
                            : entry(absSrc, absTgt, sizeSrc, sizeTgt, FolderSyncResult.MatchStatus.CONFLICT, -1));
                } else {
                    // Fall 4: Name= Inhalt≠ Größe≠ → normalerweise ignoriert
                    FolderSyncResult.FileEntry ve = classifyNonByteMatch(absSrc, absTgt, sizeSrc, sizeTgt, srcIsImage);
                    if (ve != null) { entries.add(ve); continue; }
                    differentCount++;
                }

            } else {
                // ── Fälle 5–8: Name ungleich ─────────────────────────────────
                String hashSrc = sha256(absSrc);
                List<Path> byteMatches = tgtByHash.getOrDefault(hashSrc, Collections.emptyList());

                if (!byteMatches.isEmpty()) {
                    // Inhalt (byte) gleich → Fälle 5 oder 6
                    for (Path match : byteMatches) {
                        entries.add(entry(absSrc, match, sizeSrc, safeSize(match),
                                FolderSyncResult.MatchStatus.NEEDS_REVIEW, -1));
                    }
                } else if (srcIsImage) {
                    // Kein Byte-Treffer, aber Bild → pHash-Suche im Ziel
                    FolderSyncResult.FileEntry best = findBestVisualMatch(
                            absSrc, sizeSrc, tgtByPHash);
                    if (best != null) entries.add(best);
                    else              differentCount++;
                } else {
                    // Fälle 7 + 8: kein Treffer, kein Bild
                    differentCount++;
                }
            }
        }

        // Sortierung: DUPLICATE → NEEDS_REVIEW → CONFLICT → NEAR_DUPLICATE_TEXT → VISUAL_*
        entries.sort(Comparator.comparingInt(e -> statusOrder(e.getStatus())));

        return new FolderSyncResult(sourceDir, targetDir, entries,
                srcMap.size(), tgtMap.size(), differentCount);
    }

    // ── pHash-Vergleich ───────────────────────────────────────────────────────

    /**
     * Vergleicht zwei Bilddateien per pHash und gibt einen FileEntry zurück,
     * oder null wenn pHash nicht berechnet werden konnte.
     */
    private FolderSyncResult.FileEntry visualEntry(Path src, Path tgt,
                                                   long sizeSrc, long sizeTgt) {
        try {
            if (PerceptualHasher.isAnimatedGif(src) || PerceptualHasher.isAnimatedGif(tgt)) return null;

            int[] dimSrc = PerceptualHasher.readDimensions(src);
            int[] dimTgt = PerceptualHasher.readDimensions(tgt);
            if (!PerceptualHasher.aspectRatioCompatible(dimSrc[0], dimSrc[1], dimTgt[0], dimTgt[1]))
                return null;

            long hashSrc = PerceptualHasher.hash(src);
            long hashTgt = PerceptualHasher.hash(tgt);
            int  dist    = PerceptualHasher.hammingDistance(hashSrc, hashTgt);
            PerceptualHasher.Similarity sim = PerceptualHasher.similarity(hashSrc, hashTgt);
            FolderSyncResult.MatchStatus ms = similarityToStatus(sim);
            if (ms == null) return null;

            if (!PerceptualHasher.histogramsPlausiblySimilar(
                    PerceptualHasher.histogram(src), PerceptualHasher.histogram(tgt))) return null;

            return entry(src, tgt, sizeSrc, sizeTgt, ms, dist);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sucht im pHash-Index des Zielordners nach dem besten visuellen Treffer
     * für eine Quelldatei. Wählt den Eintrag mit der kleinsten Hamming-Distanz.
     */
    private FolderSyncResult.FileEntry findBestVisualMatch(Path src, long sizeSrc,
                                                           Map<Long, List<Path>> tgtByPHash) {
        if (PerceptualHasher.isAnimatedGif(src)) return null;
        try {
            long srcHash = PerceptualHasher.hash(src);
            int[] dimSrc = PerceptualHasher.readDimensions(src);
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
            if (bestMatch == null) return null;

            if (!PerceptualHasher.histogramsPlausiblySimilar(
                    PerceptualHasher.histogram(src), PerceptualHasher.histogram(bestMatch))) return null;

            PerceptualHasher.Similarity sim = PerceptualHasher.similarity(
                    srcHash, PerceptualHasher.hash(bestMatch));
            FolderSyncResult.MatchStatus ms = similarityToStatus(sim);
            if (ms == null) return null;
            return entry(src, bestMatch, sizeSrc, safeSize(bestMatch), ms, bestDist);
        } catch (Exception e) {
            return null;
        }
    }

    private FolderSyncResult.MatchStatus similarityToStatus(PerceptualHasher.Similarity sim) {
        return switch (sim) {
            case IDENTICAL         -> FolderSyncResult.MatchStatus.VISUAL_IDENTICAL;
            case NEAR_IDENTICAL    -> FolderSyncResult.MatchStatus.VISUAL_NEAR_IDENTICAL;
            case SIMILAR           -> FolderSyncResult.MatchStatus.VISUAL_SIMILAR;
            case POSSIBLY_SIMILAR  -> FolderSyncResult.MatchStatus.VISUAL_POSSIBLY_SIMILAR;
            case DIFFERENT         -> null;
        };
    }

    // ── SimHash-Vergleich (Text) ──────────────────────────────────────────────

    /**
     * Vergleicht zwei Textdateien per SimHash und gibt einen FileEntry
     * mit Status NEAR_DUPLICATE_TEXT zurück, oder null wenn die Distanz
     * über dem Schwellwert liegt oder die Dateien nicht lesbar sind.
     */
    private FolderSyncResult.FileEntry textNearDuplicateEntry(Path src, Path tgt,
                                                              long sizeSrc, long sizeTgt) {
        try {
            long hashSrc = SimHasher.hash(src);
            long hashTgt = SimHasher.hash(tgt);
            int dist = SimHasher.hammingDistance(hashSrc, hashTgt);
            if (dist > TEXT_SIMHASH_THRESHOLD) return null;
            return entry(src, tgt, sizeSrc, sizeTgt,
                    FolderSyncResult.MatchStatus.NEAR_DUPLICATE_TEXT, dist);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Klassifiziert ein Dateipaar mit gleichem Namen, aber ungleichem Byte-Inhalt
     * (Fälle 3/4): versucht zunächst einen visuellen Treffer (Bilder), dann einen
     * SimHash-Treffer (Text). Gibt null zurück, wenn keine der beiden zutrifft.
     */
    private FolderSyncResult.FileEntry classifyNonByteMatch(Path absSrc, Path absTgt,
                                                            long sizeSrc, long sizeTgt,
                                                            boolean srcIsImage) {
        if (srcIsImage && PerceptualHasher.isImage(absTgt)) {
            FolderSyncResult.FileEntry ve = visualEntry(absSrc, absTgt, sizeSrc, sizeTgt);
            if (ve != null && ve.isVisualMatch()) return ve;
            return null;
        }
        if (FileKindClassifier.classify(absSrc) == FileKind.TEXT
                && FileKindClassifier.classify(absTgt) == FileKind.TEXT) {
            return textNearDuplicateEntry(absSrc, absTgt, sizeSrc, sizeTgt);
        }
        return null;
    }

    // ── Index-Aufbau ─────────────────────────────────────────────────────────

    private Map<Path, Path> collectFiles(Path dir) throws IOException {
        Map<Path, Path> map = new LinkedHashMap<>();
        try (Stream<Path> s = Files.walk(dir)) {
            s.filter(Files::isRegularFile)
                    .filter(abs -> !IgnoredFiles.shouldIgnore(abs))   // NEU
                    .forEach(abs -> map.put(dir.relativize(abs), abs));
        }
        return map;
    }

    private Map<String, List<Path>> buildHashIndex(Collection<Path> files,
                                                   BiConsumer<Integer, Integer> cb) {
        Map<String, List<Path>> index = new HashMap<>();
        int total = files.size(), done = 0;
        for (Path p : files) {
            try { index.computeIfAbsent(sha256(p), k -> new ArrayList<>()).add(p); }
            catch (IOException e) {
                System.err.printf("  Warnung: '%s' übersprungen (Hash) – %s%n", p.getFileName(), e.getMessage());
            }
            if (cb != null) cb.accept(++done, total);
        }
        return index;
    }

    /**
     * Baut pHash-Index nur für Bilddateien auf (pHash → Liste von Pfaden).
     * Animierte GIFs werden ausgeschlossen, da ihr pHash nur das erste Frame abbildet.
     */
    private Map<Long, List<Path>> buildPHashIndex(Collection<Path> files,
                                                  BiConsumer<Integer, Integer> cb) {
        Map<Long, List<Path>> index = new HashMap<>();
        int total = files.size(), done = 0;
        for (Path p : files) {
            if (PerceptualHasher.isImage(p) && !PerceptualHasher.isAnimatedGif(p)) {
                try { index.computeIfAbsent(PerceptualHasher.hash(p), k -> new ArrayList<>()).add(p); }
                catch (Exception e) {
                    System.err.printf("  Warnung: '%s' übersprungen (pHash) – %s%n", p.getFileName(), e.getMessage());
                }
            }
            if (cb != null) cb.accept(++done, total);
        }
        return index;
    }

    // ── Hilfsmethoden ────────────────────────────────────────────────────────

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buf = new byte[BUF]; int n;
                while ((n = is.read(buf)) != -1) md.update(buf, 0, n);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 nicht verfügbar", e);
        }
    }

    private long safeSize(Path p) {
        try { return Files.size(p); } catch (IOException e) { return -1; }
    }

    private FolderSyncResult.FileEntry entry(Path src, Path tgt,
                                             long ss, long ts,
                                             FolderSyncResult.MatchStatus status,
                                             int hammingDist) {
        return new FolderSyncResult.FileEntry(src, tgt, ss, ts, status, hammingDist);
    }

    private int statusOrder(FolderSyncResult.MatchStatus s) {
        return switch (s) {
            case DUPLICATE               -> 0;
            case NEEDS_REVIEW            -> 1;
            case CONFLICT                -> 2;
            case NEAR_DUPLICATE_TEXT     -> 3;   // NEU
            case VISUAL_IDENTICAL        -> 4;
            case VISUAL_NEAR_IDENTICAL   -> 5;
            case VISUAL_SIMILAR          -> 6;
            case VISUAL_POSSIBLY_SIMILAR -> 7;
            case DIFFERENT               -> 8;
        };
    }
}