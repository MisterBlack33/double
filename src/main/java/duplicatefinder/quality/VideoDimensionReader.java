// src/main/java/duplicatefinder/quality/VideoDimensionReader.java
package duplicatefinder.quality;

import java.io.IOException;
import java.nio.file.Path;

/** Liest Breite/Höhe eines Videos via externem ffprobe-Prozess. */
final class VideoDimensionReader {

    private VideoDimensionReader() {}

    static int[] read(Path video) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("ffprobe", "-v", "error",
                "-select_streams", "v:0", "-show_entries", "stream=width,height",
                "-of", "csv=s=x:p=0", video.toString());
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor();
        String[] parts = output.split("x");
        if (parts.length != 2) throw new IOException("Auflösung nicht lesbar: " + video.getFileName());
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }
}