package duplicatefinder.report;

import duplicatefinder.scan.ScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateResultExporterTest {

    @TempDir Path tempDir;

    private ScanResult sampleResult() {
        String hash64 = "a".repeat(64);
        var group = new ScanResult.DuplicateGroup(hash64,
                List.of(Paths.get("a.txt"), Paths.get("b.txt")), 10L);
        return new ScanResult(List.of(group), 2);
    }

    @Test
    void writesCsvWithHeaderAndRows() throws IOException {
        File target = tempDir.resolve("out.csv").toFile();

        File written = DuplicateResultExporter.export(sampleResult(), target, "CSV (*.csv)");

        String content = Files.readString(written.toPath());
        assertTrue(content.startsWith("Gruppe,Hash,Dateiname,Pfad,Groesse_B"));
        assertTrue(content.contains("a".repeat(64)));
    }

    @Test
    void writesTextReportActuallyToFile() throws IOException {
        File target = tempDir.resolve("out.txt").toFile();

        File written = DuplicateResultExporter.export(sampleResult(), target, "Text (*.txt)");

        String content = Files.readString(written.toPath());
        assertTrue(content.contains("Gruppe #1"), "Textreport muss tatsächlich in die Datei geschrieben werden");
    }

    @Test
    void appendsExtensionWhenMissing() throws IOException {
        File target = tempDir.resolve("noext").toFile();

        File written = DuplicateResultExporter.export(sampleResult(), target, "CSV (*.csv)");

        assertTrue(written.getName().endsWith(".csv"));
    }
}
