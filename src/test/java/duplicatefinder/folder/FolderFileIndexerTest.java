package duplicatefinder.folder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FolderFileIndexerTest {

    @TempDir Path tempDir;

    @Test
    void collectFilesReturnsRelativePathsExcludingIgnored() throws IOException {
        Files.writeString(tempDir.resolve("a.txt"), "x");
        Files.writeString(tempDir.resolve("Thumbs.db"), "junk");
        Path sub = Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(sub.resolve("b.txt"), "y");

        Map<Path, Path> files = FolderFileIndexer.collectFiles(tempDir);

        assertEquals(2, files.size());
        assertTrue(files.containsKey(Path.of("a.txt")));
        assertTrue(files.containsKey(Path.of("sub", "b.txt")));
    }

    @Test
    void buildHashIndexGroupsFilesWithIdenticalContent() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "same");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "same");
        Path c = Files.writeString(tempDir.resolve("c.txt"), "different");

        Map<String, List<Path>> index = FolderFileIndexer.buildHashIndex(List.of(a, b, c), null);

        assertEquals(2, index.size());
        assertTrue(index.values().stream().anyMatch(list -> list.size() == 2));
    }

    @Test
    void sha256IsStableAndDeterministic() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "hello");
        assertEquals(FolderFileIndexer.sha256(a), FolderFileIndexer.sha256(a));
    }

    @Test
    void safeSizeReturnsMinusOneForMissingFile() {
        assertEquals(-1, FolderFileIndexer.safeSize(tempDir.resolve("missing.txt")));
    }

    @Test
    void safeSizeReturnsActualSizeForExistingFile() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "12345");
        assertEquals(5, FolderFileIndexer.safeSize(a));
    }

    @Test
    void buildPHashIndexSkipsNonImageFiles() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "not an image");

        Map<Long, List<Path>> index = FolderFileIndexer.buildPHashIndex(List.of(a), null);

        assertTrue(index.isEmpty());
    }
}
