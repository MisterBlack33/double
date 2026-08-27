package duplicatefinder.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioQualityRankerTest {

    @TempDir Path tempDir;

    private AudioQualityRanker.BitrateReader fakeReader(Map<Path, Integer> bitrates) {
        return file -> bitrates.getOrDefault(file, 0);
    }

    @Test
    void ranksHigherMp3BitrateBeforeLower() throws IOException {
        Path high = Files.createFile(tempDir.resolve("song320.mp3"));
        Path low = Files.createFile(tempDir.resolve("song128.mp3"));
        var reader = fakeReader(Map.of(high, 320, low, 128));

        List<Path> ranked = AudioQualityRanker.rankByQuality(List.of(low, high), reader);

        assertEquals(high, ranked.get(0));
    }

    @Test
    void ranksFlacBeforeAnyMp3RegardlessOfBitrate() throws IOException {
        Path flac = Files.createFile(tempDir.resolve("song.flac"));
        Path mp3 = Files.createFile(tempDir.resolve("song.mp3"));
        var reader = fakeReader(Map.of(mp3, 320));

        List<Path> ranked = AudioQualityRanker.rankByQuality(List.of(mp3, flac), reader);

        assertEquals(flac, ranked.get(0));
    }

    @Test
    void sameBitrateBreaksTieBySmallerFileSize() throws IOException {
        Path smaller = Files.write(tempDir.resolve("a.mp3"), new byte[10]);
        Path bigger = Files.write(tempDir.resolve("b.mp3"), new byte[100]);
        var reader = fakeReader(Map.of(smaller, 192, bigger, 192));

        List<Path> ranked = AudioQualityRanker.rankByQuality(List.of(bigger, smaller), reader);

        assertEquals(smaller, ranked.get(0));
    }

    @Test
    void unknownBitrateIsTreatedAsLowestQuality() throws IOException {
        Path known = Files.createFile(tempDir.resolve("known.mp3"));
        Path unknown = Files.createFile(tempDir.resolve("unknown.mp3"));
        var reader = fakeReader(Map.of(known, 128));

        List<Path> ranked = AudioQualityRanker.rankByQuality(List.of(unknown, known), reader);

        assertEquals(known, ranked.get(0));
    }

    @Test
    void isLosslessRecognizesKnownExtensionsCaseInsensitively() {
        assertTrue(AudioQualityRanker.isLossless(Path.of("track.FLAC")));
        assertTrue(AudioQualityRanker.isLossless(Path.of("track.wav")));
    }

    @Test
    void isLosslessRejectsLossyExtension() {
        assertTrue(!AudioQualityRanker.isLossless(Path.of("track.mp3")));
    }

    @Test
    void defaultRankByQualityUsesRealReaderWithoutThrowing() throws IOException {
        Path a = Files.createFile(tempDir.resolve("a.mp3"));
        Path b = Files.createFile(tempDir.resolve("b.mp3"));

        assertEquals(2, AudioQualityRanker.rankByQuality(List.of(a, b)).size());
    }
}