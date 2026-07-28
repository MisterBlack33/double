package duplicatefinder;

/**
 * Einstiegspunkt des Duplicate-Finder-Projekts.
 *
 * <ul>
 *   <li>Ohne Argumente  → GUI starten</li>
 *   <li>Mit Pfad-Argument → CLI-Modus</li>
 * </ul>
 */
public class DuplicateFinder {

    public static void main(String[] args) {
        if (args.length > 0) {
            DuplicateFinderCLI.run(args);
        } else {
            DuplicateFinderGUI.launch();
        }
    }
}
