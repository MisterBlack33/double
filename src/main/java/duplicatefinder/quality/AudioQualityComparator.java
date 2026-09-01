// src/main/java/duplicatefinder/quality/AudioQualityComparator.java
package duplicatefinder.quality;

import duplicatefinder.audio.AudioQualityRanker;

import java.nio.file.Path;
import java.util.Comparator;

/** Vergleicht Audiodateien nach reiner Qualität (verlustfrei, dann Bitrate) – ohne
 *  Dateigröße, da diese hier kein Qualitätsmerkmal ist (siehe {@link OriginalOrderSorter}). */
final class AudioQualityComparator implements Comparator<Path> {

    static final AudioQualityComparator INSTANCE = new AudioQualityComparator();

    private AudioQualityComparator() {}

    @Override
    public int compare(Path a, Path b) {
        int losslessCmp = Boolean.compare(AudioQualityRanker.isLossless(b), AudioQualityRanker.isLossless(a));
        if (losslessCmp != 0) return losslessCmp;
        return Integer.compare(AudioQualityRanker.bitrateKbps(b), AudioQualityRanker.bitrateKbps(a));
    }
}