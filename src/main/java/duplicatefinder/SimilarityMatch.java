package duplicatefinder;

import java.nio.file.Path;

/** Ergebnis eines Nah-Duplikat-Vergleichs – ersetzt/erweitert die vorherige Version. */
public final class SimilarityMatch {

    public enum Confidence { VERY_HIGH, HIGH, MEDIUM, LOW }

    private final Path pathA;
    private final Path pathB;
    private final FileKind kind;
    private final MatchReason reason;
    private final Confidence confidence;
    private final int hammingDistance;

    public SimilarityMatch(Path pathA, Path pathB, FileKind kind, MatchReason reason,
                           Confidence confidence, int hammingDistance) {
        this.pathA = pathA;
        this.pathB = pathB;
        this.kind = kind;
        this.reason = reason;
        this.confidence = confidence;
        this.hammingDistance = hammingDistance;
    }

    public Path getPathA()            { return pathA; }
    public Path getPathB()            { return pathB; }
    public FileKind getKind()         { return kind; }
    public MatchReason getReason()    { return reason; }
    public Confidence getConfidence() { return confidence; }
    public int getHammingDistance()   { return hammingDistance; }
}