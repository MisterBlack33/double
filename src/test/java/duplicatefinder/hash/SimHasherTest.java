package duplicatefinder.hash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SimHasherTest {

    @TempDir Path tempDir;

    @Test
    void identicalTextProducesZeroDistance() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "The quick brown fox jumps over the lazy dog");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "The quick brown fox jumps over the lazy dog");

        assertEquals(0, SimHasher.hammingDistance(SimHasher.hash(a), SimHasher.hash(b)));
    }

    @Test
    void nearIdenticalTextHasLowDistance() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "The quick brown fox jumps over the lazy dog");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "The quick brown fox jumps over the lazy cat");

        int dist = SimHasher.hammingDistance(SimHasher.hash(a), SimHasher.hash(b));
        assertTrue(dist < 20);
    }

    @Test
    void whitespaceDifferencesDoNotAffectHash() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "hello   world\n\ntest");
        Path b = Files.writeString(tempDir.resolve("b.txt"), "hello world test");

        assertEquals(SimHasher.hash(a), SimHasher.hash(b));
    }

    @Test
    void hammingDistanceIsSymmetric() {
        assertEquals(SimHasher.hammingDistance(5L, 9L), SimHasher.hammingDistance(9L, 5L));
    }

    @Test
    void throwsForOversizedFile() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "short");
        assertDoesNotThrow(() -> SimHasher.hash(a));
    }
}
