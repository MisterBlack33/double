package duplicatefinder.scan;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScanResultTest {

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
}
