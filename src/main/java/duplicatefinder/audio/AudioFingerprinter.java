package duplicatefinder.audio;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Erzeugt ein Lautstärke-Profil einer Audiodatei in Buckets fester ABSOLUTER Länge (via ffmpeg).
 * Robust gegenüber Container/Bitrate (gleicher Song bleibt gleich laut zur gleichen Zeitposition),
 * aber empfindlich gegenüber Tempo-/Pitch-Änderungen (z. B. Nightcore): eine gestauchte Version hat
 * automatisch weniger Buckets, wodurch {@link #distance} sie klar vom Original unterscheidet.
 *
 * <p>Wichtig: Buckets dürfen NICHT relativ zur Gesamtlänge skaliert werden (z. B. "Datei / 64
 * Buckets") – sonst wäre das Profil tempo-invariant, da ein gleichmäßig beschleunigter Song
 * dieselbe relative Lautstärke-Kurve behält und fälschlich als Duplikat erkannt würde.
 */
public final class AudioFingerprinter {

    private static final int SAMPLE_RATE = 8_000;
    private static final double BUCKET_SECONDS = 0.5;
    private static final int BUCKET_SAMPLES = (int) (SAMPLE_RATE * BUCKET_SECONDS);

    private AudioFingerprinter() {}

    /** @return Lautstärke-Profil mit einem Wert pro {@link #BUCKET_SECONDS}-Fenster, normiert auf [0,1] */
    public static double[] fingerprint(Path audio) throws IOException, InterruptedException {
        short[] samples = decodeToMonoPcm(audio);
        return toEnergyProfile(samples);
    }

    /**
     * Mittlere absolute Differenz zweier Profile, auf die längere Länge aufgefüllt (fehlende
     * Buckets zählen als Stille = 0). Unterschiedliche Länge (= unterschiedliche Dauer) schlägt
     * sich direkt in der Distanz nieder.
     */
    public static double distance(double[] a, double[] b) {
        int len = Math.max(a.length, b.length);
        if (len == 0) return 0;
        double sum = 0;
        for (int i = 0; i < len; i++) {
            sum += Math.abs(valueAt(a, i) - valueAt(b, i));
        }
        return sum / len;
    }

    private static double valueAt(double[] profile, int index) {
        return index < profile.length ? profile[index] : 0.0;
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
        if (samples.length == 0) return new double[0];

        int bucketCount = (int) Math.ceil(samples.length / (double) BUCKET_SAMPLES);
        double[] profile = new double[bucketCount];
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            int start = bucket * BUCKET_SAMPLES;
            int end = Math.min(samples.length, start + BUCKET_SAMPLES);
            profile[bucket] = rmsEnergy(samples, start, end);
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