package duplicatefinder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class IgnoredFilesTest {

    @TempDir Path tempDir;

    @Test
    void ignoresKnownSystemFileNames() throws IOException {
        Path thumbs = Files.createFile(tempDir.resolve("Thumbs.db"));
        assertTrue(IgnoredFiles.isIgnoredSystemFile(thumbs));
        assertTrue(IgnoredFiles.shouldIgnore(thumbs));
    }

    @Test
    void doesNotIgnoreRegularFiles() throws IOException {
        Path regular = Files.createFile(tempDir.resolve("photo.jpg"));
        assertFalse(IgnoredFiles.isIgnoredSystemFile(regular));
    }

    @Test
    void ignoresEmptyFiles() throws IOException {
        Path empty = Files.createFile(tempDir.resolve("empty.txt"));
        assertTrue(IgnoredFiles.isEmptyFile(empty));
        assertTrue(IgnoredFiles.shouldIgnore(empty));
    }

    @Test
    void doesNotFlagNonEmptyFileAsEmpty() throws IOException {
        Path f = tempDir.resolve("data.txt");
        Files.writeString(f, "content");
        assertFalse(IgnoredFiles.isEmptyFile(f));
    }

    @Test
    void isEmptyFileReturnsFalseForMissingFile() {
        assertFalse(IgnoredFiles.isEmptyFile(tempDir.resolve("missing.txt")));
    }

    @Test
    void doesNotFlagRegularFileAsSymlink() throws IOException {
        Path f = Files.createFile(tempDir.resolve("real.txt"));
        assertFalse(IgnoredFiles.isSymbolicLink(f));
    }
}
