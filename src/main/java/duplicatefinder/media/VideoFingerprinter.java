package duplicatefinder.media;

import duplicatefinder.hash.PerceptualHasher;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** Extrahiert Bild-Fingerprints aus einem Video mittels externem ffmpeg-Prozess. */
public final class VideoFingerprinter {

    private static final int SAMPLE_FRAMES = 12;
    private static final String FFMPEG_BINARY = "ffmpeg";

    private VideoFingerprinter() {}

    public static long[] fingerprint(Path video, Path tempDir) throws IOException, InterruptedException {
        double durationSeconds = probeDurationSeconds(video);
        List<Long> hashes = new ArrayList<>();

        for (int i = 0; i < SAMPLE_FRAMES; i++) {
            double timestamp = durationSeconds * (i + 1) / (double) (SAMPLE_FRAMES + 1);
            Path frameFile = extractFrame(video, tempDir, timestamp, i);
            if (Files.exists(frameFile)) {
                hashes.add(PerceptualHasher.hash(frameFile));
                Files.deleteIfExists(frameFile);
            }
        }
        return hashes.stream().mapToLong(Long::longValue).toArray();
    }

    private static double probeDurationSeconds(Path video) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "error",
                "-show_entries", "format=duration", "-of", "csv=p=0", video.toString());
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor();
        try { return Double.parseDouble(output); }
        catch (NumberFormatException e) { throw new IOException("Videolänge nicht lesbar: " + video); }
    }

    private static Path extractFrame(Path video, Path tempDir, double timestamp, int index)
            throws IOException, InterruptedException {
        Path out = tempDir.resolve("frame_" + index + ".png");
        ProcessBuilder pb = new ProcessBuilder(FFMPEG_BINARY, "-ss", String.valueOf(timestamp),
                "-i", video.toString(), "-frames:v", "1", "-y", out.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.getInputStream().readAllBytes();
        process.waitFor();
        return out;
    }
}
