package duplicatefinder.match;

import duplicatefinder.scan.AudioDuplicateGroup;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AudioDuplicateDetectorTest {

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
    void groupsSameSongAtDifferentSampleRates() throws IOException {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path a = writeToneWav("song.wav", 8_000, 3.0);
        Path b = writeToneWav("song_hq.wav", 44_100, 3.0);

        List<AudioDuplicateGroup> groups = AudioDuplicateDetector.detect(List.of(a, b), null);

        assertEquals(1, groups.size());
        assertEquals(2, groups.get(0).getPaths().size());
    }

    @Test
    void doesNotGroupTempoChangedVersion() throws IOException {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path a = writeToneWav("song.wav", 8_000, 3.0);
        Path nightcore = writeToneWav("nightcore_song.wav", 8_000, 1.2);

        assertTrue(AudioDuplicateDetector.detect(List.of(a, nightcore), null).isEmpty());
    }

    @Test
    void ignoresNonAudioFiles() throws IOException {
        Path txt = Files.writeString(tempDir.resolve("notes.txt"), "hello");

        assertTrue(AudioDuplicateDetector.detect(List.of(txt), null).isEmpty());
    }

    @Test
    void emptyInputProducesEmptyResult() {
        assertTrue(AudioDuplicateDetector.detect(List.of(), null).isEmpty());
    }
}