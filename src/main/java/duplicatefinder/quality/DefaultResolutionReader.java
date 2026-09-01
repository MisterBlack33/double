// src/main/java/duplicatefinder/quality/DefaultResolutionReader.java
package duplicatefinder.quality;

import duplicatefinder.hash.PerceptualHasher;
import duplicatefinder.match.FileKind;
import duplicatefinder.match.FileKindClassifier;

import java.nio.file.Path;

/** Liest Pixelzahl über ImageIO (Bilder) bzw. ffprobe (Videos); 0 bei Fehler. */
final class DefaultResolutionReader implements ResolutionReader {

    @Override
    public long pixelCount(Path file) {
        try {
            int[] dim = FileKindClassifier.classify(file) == FileKind.VIDEO
                    ? VideoDimensionReader.read(file)
                    : PerceptualHasher.readDimensions(file);
            return (long) dim[0] * dim[1];
        } catch (Exception e) {
            return 0L;
        }
    }
}