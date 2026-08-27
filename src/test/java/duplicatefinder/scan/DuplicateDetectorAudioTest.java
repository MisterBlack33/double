package duplicatefinder.scan;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateDetectorAudioTest {

    @TempDir Path tempDir;

    private boolean ffmpegAvailable() {
        try {
            new ProcessBuilder("ffmpeg", "-version").start().waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Path writeToneWav(String name, int sampleRate, double durationSeconds) throws IOException {
        int frames = (int) (sampleRate * durationSeconds);
        byte[] data = new byte[frames * 2];
        for (int i = 0; i < frames; i++) {
            double t = i / (double) sampleRate;
            double envelope = 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.5 * t / durationSeconds * 3);
            short sample = (short) (envelope * Short.MAX_VALUE * Math.sin(2 * Math.PI * 440 * t));
            data[2 * i] = (byte) (sample & 0xFF);
            data[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        Path out = tempDir.resolve(name);
        try (AudioInputStream ais = new AudioInputStream(new ByteArrayInputStream(data), format, frames)) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out.toFile());
        }
        return out;
    }

    @Test
    void reportsAudioDuplicateForSameSongDifferentSampleRate() throws IOException {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path song    = writeToneWav("song.wav", 8_000, 3.0);
        Path songHq  = writeToneWav("song_hq.wav", 44_100, 3.0);

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(song, songHq));

        assertTrue(result.hasAudioDuplicates());
        assertEquals(1, result.getAudioDuplicates().size());
        assertFalse(result.hasDuplicates(), "unterschiedliche Bytes dürfen kein Byte-Duplikat ergeben");
    }

    @Test
    void doesNotReportNightcoreVariantAsDuplicateDespiteSimilarName() throws IOException {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path song      = writeToneWav("song.wav", 8_000, 3.0);
        Path nightcore = writeToneWav("nightcore_song.wav", 8_000, 1.2);

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(song, nightcore));

        assertFalse(result.hasAudioDuplicates());
    }

    @Test
    void byteIdenticalAudioIsNotAlsoReportedAsAudioDuplicate() throws IOException {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path a = writeToneWav("a.wav", 8_000, 2.0);
        Path b = java.nio.file.Files.copy(a, tempDir.resolve("b.wav"));

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(a, b));

        assertTrue(result.hasDuplicates(), "Byte-identische Dateien sollen als Byte-Duplikat gelten");
        assertFalse(result.hasAudioDuplicates(), "Byte-Duplikate sollen nicht zusätzlich als Audio-Duplikat gemeldet werden");
    }
}