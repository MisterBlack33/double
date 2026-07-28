package duplicatefinder;

/**
 * Zentraler und einziger Einstiegspunkt der Anwendung.
 *
 * <p>Entscheidet anhand der Kommandozeilenargumente, ob die Anwendung im
 * GUI- oder im CLI-Modus gestartet wird. Keine andere Klasse im Projekt
 * besitzt eine eigene {@code main}-Methode.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        StartupMode.resolve(args).start(args);
    }
}