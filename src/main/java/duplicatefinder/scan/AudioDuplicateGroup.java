package duplicatefinder.scan;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Gruppe von Audiodateien mit demselben Song, aber unterschiedlichem Byte-Inhalt,
 * gefunden von {@link duplicatefinder.match.AudioDuplicateDetector}.
 */
public final class AudioDuplicateGroup {

    private final List<Path> paths;
    private final double     maxProfileDistance;

    public AudioDuplicateGroup(List<Path> paths, double maxProfileDistance) {
        this.paths              = Collections.unmodifiableList(paths);
        this.maxProfileDistance = maxProfileDistance;
    }

    public List<Path> getPaths()              { return paths; }
    public double      getMaxProfileDistance() { return maxProfileDistance; }
}