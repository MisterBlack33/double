package duplicatefinder.report;

import duplicatefinder.scan.ScanResult;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultPrinterTest {

    @Test
    void printsNoDuplicatesMessageWhenEmpty() {
        ScanResult result = new ScanResult(List.of(), 5);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        new ResultPrinter().print(result, new PrintStream(buf, true, StandardCharsets.UTF_8));

        String output = buf.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Keine Duplikate"));
        assertTrue(output.contains("5"));
    }

    @Test
    void printsGroupDetailsWhenDuplicatesExist() {
        List<Path> paths = List.of(Paths.get("a.txt"), Paths.get("b.txt"));
        var group = new ScanResult.DuplicateGroup("abcdef0123456789", paths, 1024L);
        ScanResult result = new ScanResult(List.of(group), 2);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        new ResultPrinter().print(result, new PrintStream(buf, true, StandardCharsets.UTF_8));

        String output = buf.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("Gruppe #1"));
        assertTrue(output.contains("1.0 KB"));
    }

    @Test
    void formatsSizesInAppropriateUnits() {
        assertEquals("500 B", ResultPrinter.formatSize(500));
        assertEquals("1.0 KB", ResultPrinter.formatSize(1024));
        assertEquals("1.0 MB", ResultPrinter.formatSize(1024L * 1024));
        assertEquals("1.00 GB", ResultPrinter.formatSize(1024L * 1024 * 1024));
    }
}
