package duplicatefinder.exclude;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Entfernt aus einer Dateigruppe (Namenskollision, visuelles Duplikat) jede Datei, die
 * gegenüber ALLEN verbleibenden Mitgliedern als "verschieden" markiert wurde.
 */
public final class GroupExclusionFilter {

    private GroupExclusionFilter() {}

    public static List<Path> filter(List<Path> group, ExclusionStore exclusions) {
        List<Path> result = new ArrayList<>(group);
        while (removeOneFullyExcludedFile(result, exclusions)) {
            // weiter reduzieren, bis kein Kandidat mehr entfernbar ist
        }
        return result;
    }

    private static boolean removeOneFullyExcludedFile(List<Path> group, ExclusionStore exclusions) {
        for (Path candidate : group) {
            if (isExcludedAgainstAllOthers(candidate, group, exclusions)) {
                group.remove(candidate);
                return true;
            }
        }
        return false;
    }

    private static boolean isExcludedAgainstAllOthers(Path candidate, List<Path> group, ExclusionStore exclusions) {
        for (Path other : group) {
            if (other.equals(candidate)) continue;
            if (!exclusions.isExcluded(candidate, other)) return false;
        }
        return true;
    }
}