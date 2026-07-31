package duplicatefinder.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameCollisionDetectorTest {
    @TempDir
    Path tempDir;

    @Test
    void detectsSameNameDifferentContentAcrossSubfolders() throws IOException {
        Path sub1 = Files.createDirectory(tempDir.resolve("sub1"));
        Path sub2 = Files.createDirectory(tempDir.resolve("sub2"));
        Files.writeString(sub1.resolve("x.jpg"), "AAAA");
        Files.writeString(sub2.resolve("x.jpg"), "BBBB");

        List<NameCollisionGroup> result = NameCollisionDetector.detect(
                new FileScanner().scan(tempDir));

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getPaths().size());
    }

    @Test
    void detectsCopySuffixVariantWithDifferentContent() throws IOException {
        Files.writeString(tempDir.resolve("x.jpg"), "AAAA");
        Files.writeString(tempDir.resolve("x (1).jpg"), "BBBB");

        List<NameCollisionGroup> result = NameCollisionDetector.detect(
                new FileScanner().scan(tempDir));

        assertEquals(1, result.size());
    }

    @Test
    void ignoresIdenticalContentSinceThatsAlreadyADuplicate() throws IOException {
        Files.writeString(tempDir.resolve("x.jpg"), "same");
        Files.writeString(tempDir.resolve("x (1).jpg"), "same");

        assertTrue(NameCollisionDetector.detect(new FileScanner().scan(tempDir)).isEmpty());
    }
}