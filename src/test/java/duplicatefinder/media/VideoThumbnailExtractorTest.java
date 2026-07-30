package duplicatefinder.media;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VideoThumbnailExtractorTest {

    @TempDir Path tempDir;

    private boolean ffmpegAvailable() {
        try {
            new ProcessBuilder("ffmpeg", "-version").start().waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void throwsForMissingVideoFile() {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path missing = tempDir.resolve("missing.mp4");

        assertThrows(IOException.class, () -> VideoThumbnailExtractor.extractFrame(missing));
    }
}