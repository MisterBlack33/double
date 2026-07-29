package duplicatefinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Exportiert ein {@link ScanResult} als CSV oder Text-Report. */
final class DuplicateResultExporter {

    private DuplicateResultExporter() {}

    /** @return die tatsächlich geschriebene Datei (ggf. mit ergänzter Endung) */
    static File export(ScanResult result, File target, String filterDescription) throws IOException {
        boolean csv = filterDescription.contains("CSV") || target.getName().endsWith(".csv");
        File out = target.getName().contains(".")
                ? target
                : new File(target.getAbsolutePath() + (csv ? ".csv" : ".txt"));

        try (PrintStream ps = new PrintStream(new FileOutputStream(out), true, StandardCharsets.UTF_8)) {
            if (csv) writeCsv(ps, result); else new ResultPrinter().print(result, ps);
        }
        return out;
    }

    private static void writeCsv(PrintStream ps, ScanResult result) {
        ps.println("Gruppe,Hash,Dateiname,Pfad,Groesse_B");
        int g = 1;
        for (ScanResult.DuplicateGroup grp : result.getGroups()) {
            for (Path p : grp.getPaths())
                ps.printf("%d,%s,\"%s\",\"%s\",%d%n",
                        g, grp.getHash(), p.getFileName(), p.getParent(), grp.getFileSize());
            g++;
        }
    }
}
