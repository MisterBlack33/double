package duplicatefinder.match;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimilarityMatchTest {

    @Test
    void storesAndExposesAllFields() {
        var match = new SimilarityMatch(Paths.get("a.jpg"), Paths.get("b.jpg"),
                FileKind.IMAGE, MatchReason.EXACT, SimilarityMatch.Confidence.HIGH, 3);

        assertEquals(Paths.get("a.jpg"), match.getPathA());
        assertEquals(Paths.get("b.jpg"), match.getPathB());
        assertEquals(FileKind.IMAGE, match.getKind());
        assertEquals(MatchReason.EXACT, match.getReason());
        assertEquals(SimilarityMatch.Confidence.HIGH, match.getConfidence());
        assertEquals(3, match.getHammingDistance());
    }
}
