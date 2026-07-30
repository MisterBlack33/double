package duplicatefinder.scan;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScanResultTest {

    @TempDir Path tempDir;

    @Test
    void computesWastedBytesAndRedundantCount() {
        List<Path> paths = List.of(Paths.get("a.txt"), Paths.get("b.txt"), Paths.get("c.txt"));
        var group = new ScanResult.DuplicateGroup("hash1", paths, 100L);
        var result = new ScanResult(List.of(group), 10);

        assertEquals(200L, group.wastedBytes());
        assertEquals(200L, result.getTotalWastedBytes());
        assertEquals(2, result.getRedundantFileCount());
        assertEquals(1, result.getDuplicateGroupCount());
        assertTrue(result.hasDuplicates());
        assertEquals(10, result.getTotalFilesScanned());
    }

    @Test
    void reportsNoDuplicatesForEmptyGroups() {
        var result = new ScanResult(List.of(), 5);

        assertFalse(result.hasDuplicates());
        assertEquals(0, result.getDuplicateGroupCount());
        assertEquals(0, result.getRedundantFileCount());
        assertEquals(0L, result.getTotalWastedBytes());
    }

    @Test
    void groupsAndPathsAreUnmodifiable() {
        var group = new ScanResult.DuplicateGroup("h", List.of(Paths.get("a")), 1L);
        var result = new ScanResult(List.of(group), 1);

        assertThrows(UnsupportedOperationException.class, () -> result.getGroups().add(group));
        assertThrows(UnsupportedOperationException.class, () -> group.getPaths().add(Paths.get("x")));
    }

    @Test
    void placesFileWithoutCopySuffixFirstRegardlessOfInputOrder() {
        var group = new ScanResult.DuplicateGroup("hash1",
                List.of(Paths.get("foto (1).jpg"), Paths.get("foto.jpg")), 100L);

        assertEquals(Paths.get("foto.jpg"), group.getPaths().get(0));
        assertEquals(Paths.get("foto (1).jpg"), group.getPaths().get(1));
    }

    @Test
    void placesOlderFileFirstWhenNeitherHasCopySuffix() throws IOException {
        Path older = Files.createFile(tempDir.resolve("a.txt"));
        Path newer = Files.createFile(tempDir.resolve("b.txt"));
        Files.setLastModifiedTime(older, FileTime.fromMillis(1_000));
        Files.setLastModifiedTime(newer, FileTime.fromMillis(2_000));

        var group = new ScanResult.DuplicateGroup("hash1", List.of(newer, older), 10L);

        assertEquals(older, group.getPaths().get(0));
        assertEquals(newer, group.getPaths().get(1));
    }

    @Test
    void copySuffixOutranksAgeWhenBothPresent() throws IOException {
        Path oldButSuffixed = Files.createFile(tempDir.resolve("a (1).txt"));
        Path newerNoSuffix = Files.createFile(tempDir.resolve("a.txt"));
        Files.setLastModifiedTime(oldButSuffixed, FileTime.fromMillis(1_000));
        Files.setLastModifiedTime(newerNoSuffix, FileTime.fromMillis(9_000));

        var group = new ScanResult.DuplicateGroup("hash1", List.of(oldButSuffixed, newerNoSuffix), 10L);

        assertEquals(newerNoSuffix, group.getPaths().get(0));
    }
}
