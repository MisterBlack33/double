package duplicatefinder.exclude;

import java.nio.file.Path;

/** Null-Object: schließt nie etwas aus. Default, solange kein Ordner-Kontext für Persistenz bekannt ist. */
public final class NoOpExclusionStore implements ExclusionStore {

    public static final NoOpExclusionStore INSTANCE = new NoOpExclusionStore();

    private NoOpExclusionStore() {}

    @Override
    public boolean isExcluded(Path a, Path b) {
        return false;
    }

    @Override
    public void exclude(Path a, Path b) {
        // bewusst wirkungslos
    }
}