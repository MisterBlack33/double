package duplicatefinder.media;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Extrahiert ein einzelnes Vorschaubild aus einer Videodatei mittels externem ffmpeg-Prozess. */
public final class VideoThumbnailExtractor {

    private static final String PRIMARY_TIMESTAMP = "00:00:01";
    private static final String FALLBACK_TIMESTAMP = "00:00:00";
    private static final String FFMPEG_BINARY = "ffmpeg";

    private VideoThumbnailExtractor() {}

    public static BufferedImage extractFrame(Path video) throws IOException, InterruptedException {
        Path tempFrame = Files.createTempFile("preview_", ".png");
        try {
            BufferedImage frame = tryExtractAt(video, tempFrame, PRIMARY_TIMESTAMP);
            return frame != null ? frame : tryExtractAt(video, tempFrame, FALLBACK_TIMESTAMP);
        } finally {
            Files.deleteIfExists(tempFrame);
        }
    }

    private static BufferedImage tryExtractAt(Path video, Path outFrame, String timestamp)
            throws IOException, InterruptedException {
        String ffmpegOutput = runFfmpeg(video, outFrame, timestamp);
        if (Files.size(outFrame) == 0) {
            throw new IOException("ffmpeg lieferte kein Bild (" + video.getFileName() + "): " + ffmpegOutput);
        }
        return ImageIO.read(outFrame.toFile());
    }

    private static String runFfmpeg(Path video, Path outFrame, String timestamp)
            throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(FFMPEG_BINARY, "-i", video.toString(),
                "-ss", timestamp, "-frames:v", "1", "-y", outFrame.toString());
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IOException("ffmpeg nicht gefunden – ist es installiert und im PATH? "
                    + "Prüfen mit: where ffmpeg", e);
        }
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        return output;
    }
}