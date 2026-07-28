package duplicatefinder;

import java.nio.file.Path;
import java.util.List;

/**
 * Gibt das {@link ScanResult} übersichtlich auf der Konsole aus.
 */
public class ResultPrinter {

    private static final String SEP = "=".repeat(70);
    private static final String DIV = "-".repeat(70);

    public void print(ScanResult result) {
        System.out.println(SEP);
        System.out.println("  Ergebnis");
        System.out.println(SEP);

        if (!result.hasDuplicates()) {
            System.out.println("✓ Keine Duplikate gefunden.");
            printSummary(result);
            return;
        }

        int grpNum = 1;
        for (ScanResult.DuplicateGroup group : result.getGroups()) {
            System.out.printf("%nGruppe #%d  [SHA-256: %s…]  Größe: %s%n",
                    grpNum++,
                    group.getHash().substring(0, 16),
                    formatSize(group.getFileSize()));
            System.out.println(DIV);

            List<Path> paths = group.getPaths();
            for (int i = 0; i < paths.size(); i++) {
                String marker = (i == 0) ? "  [Original?] " : "  [Duplikat ] ";
                System.out.println(marker + paths.get(i).toAbsolutePath());
            }

            System.out.printf("  → %d Duplikat(e) | Einsparpotenzial: %s%n",
                    paths.size() - 1,
                    formatSize(group.wastedBytes()));
        }

        printSummary(result);
    }

    private void printSummary(ScanResult result) {
        System.out.println();
        System.out.println(SEP);
        System.out.println("  Zusammenfassung");
        System.out.println(SEP);
        System.out.printf("  Gescannte Dateien:         %d%n",  result.getTotalFilesScanned());
        System.out.printf("  Duplikat-Gruppen:          %d%n",  result.getDuplicateGroupCount());
        System.out.printf("  Redundante Dateien gesamt: %d%n",  result.getRedundantFileCount());
        System.out.printf("  Einsparpotenzial:          %s%n",  formatSize(result.getTotalWastedBytes()));
        System.out.println(SEP);
    }

    static String formatSize(long bytes) {
        if (bytes < 1_024)            return bytes + " B";
        double kb = bytes / 1_024.0;
        if (kb < 1_024)               return String.format("%.1f KB", kb);
        double mb = kb / 1_024.0;
        if (mb < 1_024)               return String.format("%.1f MB", mb);
        return String.format("%.2f GB", mb / 1_024.0);
    }
}
