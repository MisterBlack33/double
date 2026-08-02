package duplicatefinder.exclude;

import java.nio.file.Path;

/**
 * Speichert Datei-Paare, die der Nutzer bewusst als "nicht zusammengehörig" markiert hat,
 * damit sie bei künftigen Scans nicht erneut als Namenskollision oder visuelles Duplikat auftauchen.
 */
public interface ExclusionStore {

    boolean isExcluded(Path a, Path b);

    void exclude(Path a, Path b);
}