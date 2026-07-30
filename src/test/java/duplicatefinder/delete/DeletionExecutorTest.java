package duplicatefinder.delete;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeletionExecutorTest {

    @TempDir Path tempDir;

    @Test
    void deletesExistingFilesAndCountsThem() throws IOException {
        Path a = Files.createFile(tempDir.resolve("a.txt"));
        Path b = Files.createFile(tempDir.resolve("b.txt"));
        List<String> logs = new ArrayList<>();

        DeletionExecutor.Result result = DeletionExecutor.delete(List.of(a, b), "", logs::add);

        assertEquals(2, result.deleted());
        assertEquals(0, result.failed());
        assertFalse(Files.exists(a));
        assertFalse(Files.exists(b));
        assertEquals(2, logs.size());
        assertTrue(logs.get(0).startsWith("Gelöscht: "));
    }

    @Test
    void countsMissingFileAsFailure() {
        Path missing = tempDir.resolve("missing.txt");

        DeletionExecutor.Result result = DeletionExecutor.delete(List.of(missing), "", msg -> {});

        assertEquals(0, result.deleted());
        assertEquals(1, result.failed());
    }

    @Test
    void appliesLabelPrefixToLogMessage() throws IOException {
        Path a = Files.createFile(tempDir.resolve("a.txt"));
        List<String> logs = new ArrayList<>();

        DeletionExecutor.delete(List.of(a), " [Quelle]", logs::add);

        assertTrue(logs.get(0).startsWith("Gelöscht [Quelle]: "));
    }

    @Test
    void worksWithoutLoggerCallback() throws IOException {
        Path a = Files.createFile(tempDir.resolve("a.txt"));

        DeletionExecutor.Result result = DeletionExecutor.delete(List.of(a), "", null);

        assertEquals(1, result.deleted());
    }

    @Test
    void returnsZeroResultForEmptyInput() {
        DeletionExecutor.Result result = DeletionExecutor.delete(List.of(), "", null);

        assertEquals(0, result.deleted());
        assertEquals(0, result.failed());
    }
}
