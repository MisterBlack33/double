package duplicatefinder;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageMatchClassifierTest {

    @Test
    void sameFormatAndZeroDistanceIsExact() {
        assertEquals(MatchReason.EXACT,
                ImageMatchClassifier.classify(Paths.get("a.jpg"), Paths.get("b.jpg"), 0));
    }

    @Test
    void differentFormatAndZeroDistanceIsFormatVariant() {
        assertEquals(MatchReason.FORMAT_VARIANT,
                ImageMatchClassifier.classify(Paths.get("a.jpg"), Paths.get("b.png"), 0));
    }

    @Test
    void sameFormatAndNonZeroDistanceIsResolutionVariant() {
        assertEquals(MatchReason.RESOLUTION_VARIANT,
                ImageMatchClassifier.classify(Paths.get("a.jpg"), Paths.get("b.jpg"), 3));
    }

    @Test
    void differentFormatAndNonZeroDistanceIsFormatVariant() {
        assertEquals(MatchReason.FORMAT_VARIANT,
                ImageMatchClassifier.classify(Paths.get("a.jpg"), Paths.get("b.png"), 3));
    }
}
