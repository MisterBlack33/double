package duplicatefinder.match;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopySuffixDetectorTest {

    @Test
    void detectsNumberedParenthesisSuffix() {
        assertTrue(CopySuffixDetector.hasCopySuffix(Paths.get("foto (1).jpg")));
    }

    @Test
    void detectsGermanKopieSuffix() {
        assertTrue(CopySuffixDetector.hasCopySuffix(Paths.get("dokument - Kopie.pdf")));
        assertTrue(CopySuffixDetector.hasCopySuffix(Paths.get("dokument - Kopie (2).pdf")));
    }

    @Test
    void detectsEnglishCopySuffix() {
        assertTrue(CopySuffixDetector.hasCopySuffix(Paths.get("image copy.png")));
        assertTrue(CopySuffixDetector.hasCopySuffix(Paths.get("image copy (3).png")));
    }

    @Test
    void doesNotFlagRegularFileName() {
        assertFalse(CopySuffixDetector.hasCopySuffix(Paths.get("foto.jpg")));
        assertFalse(CopySuffixDetector.hasCopySuffix(Paths.get("bericht_2024.pdf")));
    }
}