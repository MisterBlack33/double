package duplicatefinder.scan;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorruptedFileScannerTest {

    @TempDir Path tempDir;

    @Test
    void flagsFileWithImageExtensionButInvalidContentAsUnreadable() throws IOException {
        Path fake = Files.write(tempDir.resolve("broken.jpg"),
                "not a real jpeg, just garbage bytes".getBytes());

        assertFalse(CorruptedFileScanner.isReadable(fake));
    }

    @Test
    void flagsTruncatedPngAsUnreadable() throws IOException {
        // gültige PNG-Signatur, aber danach abgeschnitten -> Decoder scheitert
        byte[] pngHeader = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        Path broken = Files.write(tempDir.resolve("truncated.png"), pngHeader);

        assertFalse(CorruptedFileScanner.isReadable(broken));
    }

    @Test
    void doesNotFlagValidImageAsUnreadable() throws IOException {
        var img = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Path valid = tempDir.resolve("valid.png");
        javax.imageio.ImageIO.write(img, "png", valid.toFile());

        assertTrue(CorruptedFileScanner.isReadable(valid));
    }

    @Test
    void doesNotFlagNonImageTextFileJustBecauseContentIsNotAnImage() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "hello world");

        assertTrue(CorruptedFileScanner.isReadable(a));
    }

    @Test
    void flagsBrokenSymlinkAsUnreadable() throws IOException {
        Path target = tempDir.resolve("missing.txt");
        Path link = tempDir.resolve("broken.txt");
        try {
            Files.createSymbolicLink(link, target);
        } catch (IOException | UnsupportedOperationException e) {
            Assumptions.assumeTrue(false, "Symlinks nicht erstellbar: " + e.getMessage());
            return;
        }

        assertFalse(CorruptedFileScanner.isReadable(link));
    }

    @Test
    void doesNotFlagRegularReadableFile() throws IOException {
        Path a = Files.writeString(tempDir.resolve("a.txt"), "content");
        assertTrue(CorruptedFileScanner.findUnreadable(List.of(a)).isEmpty());
    }

    @Test
    void returnsEmptyListForEmptyCollection() {
        assertTrue(CorruptedFileScanner.findUnreadable(List.of()).isEmpty());
    }

    @Test
    void isReadableReturnsFalseForMissingFile() {
        assertFalse(CorruptedFileScanner.isReadable(tempDir.resolve("gone.txt")));
    }
}