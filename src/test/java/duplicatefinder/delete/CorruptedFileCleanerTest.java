package duplicatefinder.delete;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorruptedFileCleanerTest {

    @TempDir Path tempDir;

    @Test
    void returnsZeroAndSkipsDialogWhenAllFilesReadable() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "ok");

        assertEquals(0, CorruptedFileCleaner.cleanUnreadable(null, List.of(a), null));
    }

    @Test
    void deleteWithConfirmationReturnsZeroForEmptyList() {
        assertEquals(0, CorruptedFileCleaner.deleteWithConfirmation(null, List.of(), null));
    }

    @Test
    void cleanUnreadableReturnsZeroForEmptyInput() {
        assertEquals(0, CorruptedFileCleaner.cleanUnreadable(null, List.of(), null));
    }
}