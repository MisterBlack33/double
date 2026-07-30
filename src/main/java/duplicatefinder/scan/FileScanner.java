package duplicatefinder.scan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Durchsucht ein Verzeichnis rekursiv und liefert alle regulären Dateien.
 *
 * <p>Unterstützt einen optionalen Fortschritts-Callback, den die GUI nutzen kann,
 * um den Scan-Fortschritt darzustellen.
 */
public class FileScanner {

    public List<Path> scan(Path rootDir, Consumer<Path> progressCallback) throws IOException {
        List<Path> files = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                files.add(p);
                if (progressCallback != null) progressCallback.accept(p);
            });
        }

        files.sort(Path::compareTo);
        return files;
    }

    public List<Path> scan(Path rootDir) throws IOException {
        return scan(rootDir, null);
    }
}
