package duplicatefinder.audio;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioFingerprinterTest {

    private static final double DUPLICATE_THRESHOLD = 0.08;

    @TempDir Path tempDir;

    private boolean ffmpegAvailable() {
        try {
            new ProcessBuilder("ffmpeg", "-version").start().waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Erzeugt eine WAV-Datei mit lautstärkemoduliertem Ton (nicht-triviales Zeitprofil). */
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
    void sameToneAtDifferentSampleRateHasLowDistance() throws Exception {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path a = writeToneWav("a.wav", 8_000, 3.0);
        Path b = writeToneWav("b.wav", 44_100, 3.0);

        double dist = AudioFingerprinter.distance(AudioFingerprinter.fingerprint(a), AudioFingerprinter.fingerprint(b));

        assertTrue(dist < DUPLICATE_THRESHOLD, "gleicher Song in anderer Samplerate sollte als Duplikat gelten");
    }

    @Test
    void differentDurationTimeProfileHasHighDistance() throws Exception {
        Assumptions.assumeTrue(ffmpegAvailable(), "ffmpeg nicht installiert");
        Path a = writeToneWav("a.wav", 8_000, 3.0);
        Path nightcoreLike = writeToneWav("nightcore.wav", 8_000, 1.2);

        double dist = AudioFingerprinter.distance(
                AudioFingerprinter.fingerprint(a), AudioFingerprinter.fingerprint(nightcoreLike));

        assertTrue(dist > DUPLICATE_THRESHOLD, "tempo-verändertes Zeitprofil sollte kein Duplikat sein");
    }
}