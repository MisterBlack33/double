package duplicatefinder.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileScannerTest {

    @TempDir Path tempDir;

    @Test
    void findsFilesRecursivelyAndSortsThem() throws IOException {
        Files.createFile(tempDir.resolve("b.txt"));
        Path sub = Files.createDirectory(tempDir.resolve("sub"));
        Files.createFile(sub.resolve("a.txt"));

        List<Path> found = new FileScanner().scan(tempDir);

        assertEquals(2, found.size());
        assertTrue(found.get(0).compareTo(found.get(1)) <= 0);
    }

    @Test
    void invokesProgressCallbackPerFile() throws IOException {
        Files.createFile(tempDir.resolve("a.txt"));
        Files.createFile(tempDir.resolve("b.txt"));
        int[] count = {0};

        new FileScanner().scan(tempDir, p -> count[0]++);

        assertEquals(2, count[0]);
    }

    @Test
    void returnsEmptyListForEmptyDirectory() throws IOException {
        assertTrue(new FileScanner().scan(tempDir).isEmpty());
    }

    @Test
    void ignoresDirectoriesThemselves() throws IOException {
        Files.createDirectory(tempDir.resolve("onlyDir"));

        assertTrue(new FileScanner().scan(tempDir).isEmpty());
    }
}
