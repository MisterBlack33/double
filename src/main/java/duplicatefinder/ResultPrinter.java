package duplicatefinder;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

/** Gibt das {@link ScanResult} übersichtlich aus (Konsole oder beliebiger {@link PrintStream}). */
public class ResultPrinter {

    private static final String SEP = "=".repeat(70);
    private static final String DIV = "-".repeat(70);

    /** Schreibt den Report auf die Konsole. */
    public void print(ScanResult result) {
        print(result, System.out);
    }

    /** Schreibt den Report auf den übergebenen Stream, z. B. für Datei-Exports. */
    public void print(ScanResult result, PrintStream out) {
        out.println(SEP);
        out.println("  Ergebnis");
        out.println(SEP);

        if (!result.hasDuplicates()) {
            out.println("✓ Keine Duplikate gefunden.");
            printSummary(result, out);
            return;
        }

        int grpNum = 1;
        for (ScanResult.DuplicateGroup group : result.getGroups()) {
            out.printf("%nGruppe #%d  [SHA-256: %s…]  Größe: %s%n",
                    grpNum++,
                    group.getHash().substring(0, 16),
                    formatSize(group.getFileSize()));
            out.println(DIV);

            List<Path> paths = group.getPaths();
            for (int i = 0; i < paths.size(); i++) {
                String marker = (i == 0) ? "  [Original?] " : "  [Duplikat ] ";
                out.println(marker + paths.get(i).toAbsolutePath());
            }

            out.printf("  → %d Duplikat(e) | Einsparpotenzial: %s%n",
                    paths.size() - 1,
                    formatSize(group.wastedBytes()));
        }

        printSummary(result, out);
    }

    private void printSummary(ScanResult result, PrintStream out) {
        out.println();
        out.println(SEP);
        out.println("  Zusammenfassung");
        out.println(SEP);
        out.printf("  Gescannte Dateien:         %d%n",  result.getTotalFilesScanned());
        out.printf("  Duplikat-Gruppen:          %d%n",  result.getDuplicateGroupCount());
        out.printf("  Redundante Dateien gesamt: %d%n",  result.getRedundantFileCount());
        out.printf("  Einsparpotenzial:          %s%n",  formatSize(result.getTotalWastedBytes()));
        out.println(SEP);
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
