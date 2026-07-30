package duplicatefinder.hash;

/** Prüft, ob eine kurze Fingerprint-Sequenz als zusammenhängender Ausschnitt in einer
 *  längeren Sequenz vorkommt (z. B. kurzer Clip aus einem längeren Video). */
public final class SequenceMatcher {

    private static final int FRAME_MATCH_THRESHOLD = 8;
    private static final double MIN_MATCHING_FRAME_RATIO = 0.7;

    private SequenceMatcher() {}

    public static boolean isSubsequenceOf(long[] shortSeq, long[] longSeq) {
        if (shortSeq.length == 0 || longSeq.length < shortSeq.length) return false;

        for (int offset = 0; offset <= longSeq.length - shortSeq.length; offset++) {
            if (windowMatches(shortSeq, longSeq, offset)) return true;
        }
        return false;
    }

    private static boolean windowMatches(long[] shortSeq, long[] longSeq, int offset) {
        int matchingFrames = 0;
        for (int i = 0; i < shortSeq.length; i++) {
            int distance = PerceptualHasher.hammingDistance(shortSeq[i], longSeq[offset + i]);
            if (distance <= FRAME_MATCH_THRESHOLD) matchingFrames++;
        }
        return matchingFrames >= shortSeq.length * MIN_MATCHING_FRAME_RATIO;
    }
}
