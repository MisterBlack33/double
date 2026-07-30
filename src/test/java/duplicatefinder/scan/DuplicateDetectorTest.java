package duplicatefinder.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateDetectorTest {

    @TempDir Path tempDir;

    @Test
    void findsExactContentDuplicates() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "hello world");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "hello world");
        Files.writeString(tempDir.resolve("c.txt"), "different");

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(a, b, tempDir.resolve("c.txt")));

        assertTrue(result.hasDuplicates());
        assertEquals(1, result.getDuplicateGroupCount());
        assertEquals(1, result.getRedundantFileCount());
        assertEquals(2, result.getGroups().get(0).getPaths().size());
    }

    @Test
    void returnsNoDuplicatesForUniqueFiles() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "one");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "two");

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(a, b));

        assertFalse(result.hasDuplicates());
        assertEquals(2, result.getTotalFilesScanned());
    }

    @Test
    void ignoresEmptyFilesEvenIfMultipleExist() throws IOException {
        Path a = Files.createFile(tempDir.resolve("empty1.txt"));
        Path b = Files.createFile(tempDir.resolve("empty2.txt"));

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(a, b));

        assertFalse(result.hasDuplicates());
    }

    @Test
    void handlesEmptyFileListGracefully() throws IOException {
        ScanResult result = new DuplicateDetector().findDuplicates(List.of());

        assertFalse(result.hasDuplicates());
        assertEquals(0, result.getTotalFilesScanned());
    }

    @Test
    void invokesProgressCallbackDuringFullHashing() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "same content here");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "same content here");
        int[] calls = {0};

        new DuplicateDetector().findDuplicates(List.of(a, b), (done, total) -> calls[0]++);

        assertTrue(calls[0] > 0);
    }

    @Test
    void groupsLargerFilesBeforeSmallerOnes() throws IOException {
        Path small1 = Files.writeString(tempDir.resolve("s1.txt"), "aa");
        Path small2 = Files.writeString(tempDir.resolve("s2.txt"), "aa");
        Path big1 = Files.writeString(tempDir.resolve("b1.txt"), "aaaaaaaaaa");
        Path big2 = Files.writeString(tempDir.resolve("b2.txt"), "aaaaaaaaaa");

        ScanResult result = new DuplicateDetector().findDuplicates(List.of(small1, small2, big1, big2));

        assertEquals(2, result.getDuplicateGroupCount());
        assertTrue(result.getGroups().get(0).getFileSize() >= result.getGroups().get(1).getFileSize());
    }
}
