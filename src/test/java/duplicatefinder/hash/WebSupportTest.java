package duplicatefinder.hash;

import duplicatefinder.match.VisualDuplicateDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prüft, dass .webp-Dateien lesbar und vergleichbar sind (com.twelvemonkeys.imageio:imageio-webp).
 *
 * <p>Die eingebetteten Base64-Strings sind minimale, verlustfreie 32×32-Volltonbilder
 * (erzeugt mit {@code cwebp -lossless}), damit die Tests ohne externe Ressourcendatei
 * und ohne Netzwerkzugriff auskommen.
 */
class WebpSupportTest {

    private static final String RED_WEBP_BASE64 =
            "UklGRhwAAABXRUJQVlA4TA8AAAAvH8AHAAcQ/Y/+ByKi/wEA";
    private static final String BLUE_WEBP_BASE64 =
            "UklGRhwAAABXRUJQVlA4TA8AAAAvH8AHAAcQ0f/+ByKi/wEA";

    @TempDir Path tempDir;

    private Path writeFixture(String name, String base64) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, Base64.getDecoder().decode(base64));
        return file;
    }

    @Test
    void recognizesWebpExtensionCaseInsensitively() {
        assertTrue(PerceptualHasher.isImage(Paths.get("photo.webp")));
        assertTrue(PerceptualHasher.isImage(Paths.get("photo.WEBP")));
    }

    @Test
    void readsDimensionsFromWebpFile() throws IOException {
        Path file = writeFixture("red.webp", RED_WEBP_BASE64);

        int[] dims = PerceptualHasher.readDimensions(file);

        assertEquals(32, dims[0]);
        assertEquals(32, dims[1]);
    }

    @Test
    void hashesWebpFileWithoutError() throws IOException {
        Path file = writeFixture("red.webp", RED_WEBP_BASE64);

        assertDoesNotThrow(() -> PerceptualHasher.hash(file));
    }

    @Test
    void identicalWebpFilesHashToSameValue() throws IOException {
        Path a = writeFixture("red_a.webp", RED_WEBP_BASE64);
        Path b = writeFixture("red_b.webp", RED_WEBP_BASE64);

        assertEquals(PerceptualHasher.hash(a), PerceptualHasher.hash(b));
    }

    @Test
    void differentColorWebpFilesAreNotIdentical() throws IOException {
        Path red  = writeFixture("red.webp", RED_WEBP_BASE64);
        Path blue = writeFixture("blue.webp", BLUE_WEBP_BASE64);

        long hashRed  = PerceptualHasher.hash(red);
        long hashBlue = PerceptualHasher.hash(blue);

        assertNotEquals(PerceptualHasher.Similarity.IDENTICAL, PerceptualHasher.similarity(hashRed, hashBlue));
    }

    @Test
    void webpFilesAreClusteredByVisualDuplicateDetector() throws IOException {
        Path a = writeFixture("a.webp", RED_WEBP_BASE64);
        Path b = writeFixture("b.webp", RED_WEBP_BASE64);

        List<duplicatefinder.scan.VisualDuplicateGroup> groups = VisualDuplicateDetector.detect(List.of(a, b), null);

        assertEquals(1, groups.size());
    }
}