package duplicatefinder.scan;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Gruppe visuell (fast) identischer Bilder mit unterschiedlichem Byte-Inhalt,
 * gefunden von {@link duplicatefinder.match.VisualDuplicateDetector}.
 */
public final class VisualDuplicateGroup {

    private final List<Path> paths;
    private final int        maxHammingDistance;

    public VisualDuplicateGroup(List<Path> paths, int maxHammingDistance) {
        this.paths              = Collections.unmodifiableList(paths);
        this.maxHammingDistance = maxHammingDistance;
    }

    public List<Path> getPaths()              { return paths; }
    public int         getMaxHammingDistance() { return maxHammingDistance; }
}