package duplicatefinder.scan;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Dateien mit (bis auf Kopie-Suffix) identischem Namen, aber unterschiedlichem Inhalt.
 * Gefunden von {@link NameCollisionDetector}.
 */
public final class NameCollisionGroup {

    private final String fileName;
    private final List<Path> paths;

    NameCollisionGroup(String fileName, List<Path> paths) {
        this.fileName = fileName;
        this.paths = Collections.unmodifiableList(paths);
    }

    public String     getFileName() { return fileName; }
    public List<Path> getPaths()    { return paths; }
}