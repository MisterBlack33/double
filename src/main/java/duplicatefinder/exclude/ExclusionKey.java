package duplicatefinder.exclude;

import java.nio.file.Path;

/** Baut einen richtungsunabhängigen Schlüssel für ein Datei-Paar, sodass (a,b) == (b,a). */
final class ExclusionKey {

    private ExclusionKey() {}

    static String of(Path a, Path b) {
        String sa = a.toAbsolutePath().normalize().toString();
        String sb = b.toAbsolutePath().normalize().toString();
        return sa.compareTo(sb) <= 0 ? sa + "|" + sb : sb + "|" + sa;
    }
}