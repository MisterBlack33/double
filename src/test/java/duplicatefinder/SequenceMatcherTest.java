package duplicatefinder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SequenceMatcherTest {

    @Test
    void findsExactSubsequence() {
        long[] longSeq = {1, 2, 3, 4, 5};
        long[] shortSeq = {3, 4};
        assertTrue(SequenceMatcher.isSubsequenceOf(shortSeq, longSeq));
    }

    @Test
    void rejectsWhenShortSequenceLongerThanLong() {
        assertFalse(SequenceMatcher.isSubsequenceOf(new long[]{1, 2, 3}, new long[]{1}));
    }

    @Test
    void rejectsEmptyShortSequence() {
        assertFalse(SequenceMatcher.isSubsequenceOf(new long[]{}, new long[]{1, 2}));
    }

    @Test
    void toleratesSmallHammingDifferencesWithinFrames() {
        long[] longSeq = {0b0000, 0b0001, 0b0010};
        long[] shortSeq = {0b0000, 0b0001};
        assertTrue(SequenceMatcher.isSubsequenceOf(shortSeq, longSeq));
    }

    @Test
    void rejectsCompletelyDifferentSequence() {
        long[] longSeq = {0xFFFFFFFFL, 0x00000000L};
        long[] shortSeq = {0x0F0F0F0FL};
        assertFalse(SequenceMatcher.isSubsequenceOf(shortSeq, longSeq));
    }
}
