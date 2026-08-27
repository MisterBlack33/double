package duplicatefinder.audio;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Erzeugt ein einfaches, zeit-normalisiertes Lautstärke-Profil einer Audiodatei (via ffmpeg).
 * Robust gegenüber Container/Bitrate (gleicher Song bleibt gleich laut zur gleichen relativen
 * Position), aber empfindlich gegenüber Tempo-/Pitch-Änderungen (z. B. Nightcore), da sich dabei
 * die zeitliche Zuordnung der Lautstärke-Verläufe verschiebt.
 */
public final class AudioFingerprinter {

    private static final int SAMPLE_RATE = 8_000;
    private static final int PROFILE_BUCKETS = 64;

    private AudioFingerprinter() {}

    /** @return zeit-normalisiertes Profil mit {@link #PROFILE_BUCKETS} Werten in [0,1] */
    public static double[] fingerprint(Path audio) throws IOException, InterruptedException {
        short[] samples = decodeToMonoPcm(audio);
        return toEnergyProfile(samples);
    }

    /** Mittlere absolute Differenz zweier Profile gleicher Länge; 0 = identisch. */
    public static double distance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < PROFILE_BUCKETS; i++) sum += Math.abs(a[i] - b[i]);
        return sum / PROFILE_BUCKETS;
    }

    private static short[] decodeToMonoPcm(Path audio) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-i", audio.toString(),
                "-ac", "1", "-ar", String.valueOf(SAMPLE_RATE), "-f", "s16le", "-loglevel", "error", "-");
        Process process = pb.start();
        byte[] raw = process.getInputStream().readAllBytes();
        process.waitFor();
        if (raw.length < 4) throw new IOException("Audio nicht dekodierbar: " + audio.getFileName());
        return bytesToSamples(raw);
    }

    private static short[] bytesToSamples(byte[] raw) {
        short[] samples = new short[raw.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((raw[2 * i] & 0xFF) | (raw[2 * i + 1] << 8));
        }
        return samples;
    }

    private static double[] toEnergyProfile(short[] samples) {
        double[] profile = new double[PROFILE_BUCKETS];
        if (samples.length == 0) return profile;

        int bucketSize = Math.max(1, samples.length / PROFILE_BUCKETS);
        for (int bucket = 0; bucket < PROFILE_BUCKETS; bucket++) {
            profile[bucket] = rmsEnergy(samples, bucket * bucketSize,
                    Math.min(samples.length, (bucket + 1) * bucketSize));
        }
        normalize(profile);
        return profile;
    }

    private static double rmsEnergy(short[] samples, int start, int end) {
        if (end <= start) return 0;
        long sumSq = 0;
        for (int i = start; i < end; i++) sumSq += (long) samples[i] * samples[i];
        return Math.sqrt((double) sumSq / (end - start));
    }

    private static void normalize(double[] profile) {
        double max = 0;
        for (double v : profile) max = Math.max(max, v);
        if (max <= 0) return;
        for (int i = 0; i < profile.length; i++) profile[i] /= max;
    }
}