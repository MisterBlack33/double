package duplicatefinder.cli;

import duplicatefinder.delete.DeletionExecutor;
import duplicatefinder.report.ResultPrinter;
import duplicatefinder.scan.CorruptedFileScanner;
import duplicatefinder.scan.DuplicateDetector;
import duplicatefinder.scan.FileScanner;
import duplicatefinder.scan.ScanResult;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/** CLI-Modus: java -jar duplicate-finder.jar &lt;Verzeichnispfad&gt; */
public class DuplicateFinderCLI {

    public static void run(String[] args) {
        Path rootDir = Paths.get(args[0]);

        if (!rootDir.toFile().isDirectory()) {
            System.err.println("Fehler: '" + rootDir + "' ist kein gültiges Verzeichnis.");
            System.exit(1);
        }

        printBanner();
        System.out.println("Durchsuche: " + rootDir.toAbsolutePath());
        System.out.println();

        try {
            List<Path> files = new FileScanner().scan(rootDir);
            List<Path> unreadable = CorruptedFileScanner.findUnreadable(files);
            if (!unreadable.isEmpty()) {
                System.out.printf("%d nicht lesbare Datei(en) werden gelöscht:%n", unreadable.size());
                DeletionExecutor.delete(unreadable, " [korrupt]", System.out::println);
                files.removeAll(unreadable);
            }
            System.out.printf("Gefundene Dateien: %d%n%n", files.size());

            ScanResult result    = new DuplicateDetector().findDuplicates(files);
            new ResultPrinter().print(result);

        } catch (IOException e) {
            System.err.println("Fehler beim Scannen: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void printBanner() {
        System.out.println("=".repeat(60));
        System.out.println("  Duplicate File Finder");
        System.out.println("=".repeat(60));
    }
}
