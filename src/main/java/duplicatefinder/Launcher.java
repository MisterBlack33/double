package duplicatefinder;

/** Zentraler und einziger Einstiegspunkt der Anwendung (GUI- oder CLI-Modus). */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        StartupMode.resolve(args).start(args);
    }
}
