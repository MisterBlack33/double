// src/test/java/duplicatefinder/quality/OriginalOrderSorterTest.java
package duplicatefinder.quality;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OriginalOrderSorterTest {

    @TempDir Path tempDir;

    private ResolutionReader fakeResolution(Map<Path, Long> pixels) {
        return file -> pixels.getOrDefault(file, 0L);
    }

    @Test
    void higherResolutionImageWinsRegardlessOfName() throws IOException {
        Path small = Files.createFile(tempDir.resolve("foto.jpg"));
        Path big = Files.createFile(tempDir.resolve("foto (1).jpg"));
        var reader = fakeResolution(Map.of(small, 100L, big, 400L));

        assertEquals(big, OriginalOrderSorter.sort(List.of(small, big), reader).get(0));
    }

    @Test
    void equalResolutionFallsBackToCopySuffixRule() throws IOException {
        Path copy = Files.createFile(tempDir.resolve("foto (1).jpg"));
        Path original = Files.createFile(tempDir.resolve("foto.jpg"));
        var reader = fakeResolution(Map.of(copy, 200L, original, 200L));

        assertEquals(original, OriginalOrderSorter.sort(List.of(copy, original), reader).get(0));
    }

    @Test
    void videoUsesSameResolutionRuleAsImages() throws IOException {
        Path low = Files.createFile(tempDir.resolve("a.mp4"));
        Path high = Files.createFile(tempDir.resolve("b.mp4"));
        var reader = fakeResolution(Map.of(low, 720L * 480, high, 1920L * 1080));

        assertEquals(high, OriginalOrderSorter.sort(List.of(low, high), reader).get(0));
    }

    @Test
    void losslessAudioWinsOverLossyRegardlessOfCopySuffix() throws IOException {
        Path mp3 = Files.createFile(tempDir.resolve("song.mp3"));
        Path flac = Files.createFile(tempDir.resolve("song (1).flac"));

        assertEquals(flac, OriginalOrderSorter.sort(List.of(mp3, flac), null).get(0));
    }

    @Test
    void equalAudioQualityFallsBackToOlderFileRule() throws IOException {
        Path newer = Files.createFile(tempDir.resolve("a.mp3"));
        Path older = Files.createFile(tempDir.resolve("b.mp3"));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000));
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_000));

        assertEquals(older, OriginalOrderSorter.sort(List.of(newer, older), null).get(0));
    }

    @Test
    void binaryFilesIgnoreQualityAndUseCopySuffixRule() throws IOException {
        Path copy = Files.createFile(tempDir.resolve("data (1).zip"));
        Path original = Files.createFile(tempDir.resolve("data.zip"));

        assertEquals(original, OriginalOrderSorter.sort(List.of(copy, original), null).get(0));
    }

    @Test
    void emptyListReturnsEmptyList() {
        assertTrue(OriginalOrderSorter.sort(List.of()).isEmpty());
    }

    @Test
    void defaultOverloadWorksWithoutInjectedReader() throws IOException {
        Path a = Files.createFile(tempDir.resolve("data (1).txt"));
        Path b = Files.createFile(tempDir.resolve("data.txt"));

        assertEquals(b, OriginalOrderSorter.sort(List.of(a, b)).get(0));
    }
}