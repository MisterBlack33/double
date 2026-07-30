package duplicatefinder.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/** Öffnet eine Datei mit der Standardanwendung des Betriebssystems. */
final class SystemFileOpener {

    private SystemFileOpener() {}

    static boolean open(Path file) {
        if (!Desktop.isDesktopSupported()) return false;
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.OPEN)) return false;
        try {
            desktop.open(file.toFile());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}