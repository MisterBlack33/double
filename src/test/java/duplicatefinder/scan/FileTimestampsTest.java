package duplicatefinder.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTimestampsTest {

    @TempDir Path tempDir;

    @Test
    void returnsActualModificationTime() throws IOException {
        Path file = Files.createFile(tempDir.resolve("a.txt"));
        FileTime expected = FileTime.fromMillis(1_700_000_000_000L);
        Files.setLastModifiedTime(file, expected);

        assertEquals(expected.toMillis(), FileTimestamps.lastModifiedMillis(file));
    }

    @Test
    void returnsMaxValueForMissingFile() {
        assertEquals(Long.MAX_VALUE, FileTimestamps.lastModifiedMillis(tempDir.resolve("missing.txt")));
    }
}