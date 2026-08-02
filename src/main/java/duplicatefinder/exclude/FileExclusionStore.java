package duplicatefinder.exclude;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Datei-basierte {@link ExclusionStore}-Implementierung. Speichert je Zeile einen
 * richtungsunabhängigen Paar-Schlüssel in einer einfachen Textdatei (z. B. neben dem
 * gescannten Ordner: {@code .duplicate-finder-exclusions.txt}).
 */
public final class FileExclusionStore implements ExclusionStore {

    private final Path storeFile;
    private final Set<String> keys = new HashSet<>();
    private boolean loaded;

    public FileExclusionStore(Path storeFile) {
        this.storeFile = storeFile;
    }

    @Override
    public boolean isExcluded(Path a, Path b) {
        ensureLoaded();
        return keys.contains(ExclusionKey.of(a, b));
    }

    @Override
    public void exclude(Path a, Path b) {
        ensureLoaded();
        String key = ExclusionKey.of(a, b);
        if (keys.add(key)) persist(key);
    }

    private void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(storeFile)) return;
        try {
            for (String line : Files.readAllLines(storeFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) keys.add(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Warnung: Ausschlussliste nicht lesbar – " + e.getMessage());
        }
    }

    private void persist(String key) {
        try {
            Files.writeString(storeFile, key + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Warnung: Ausschluss konnte nicht gespeichert werden – " + e.getMessage());
        }
    }
}