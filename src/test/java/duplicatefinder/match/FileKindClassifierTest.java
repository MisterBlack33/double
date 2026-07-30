package duplicatefinder.match;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileKindClassifierTest {

    @Test
    void classifiesKnownImageExtensionAsImage() {
        assertEquals(FileKind.IMAGE, FileKindClassifier.classify(Paths.get("photo.PNG")));
    }

    @Test
    void classifiesKnownTextExtensionAsText() {
        assertEquals(FileKind.TEXT, FileKindClassifier.classify(Paths.get("notes.MD")));
    }

    @Test
    void classifiesUnknownExtensionAsBinary() {
        assertEquals(FileKind.BINARY, FileKindClassifier.classify(Paths.get("archive.zip")));
    }

    @Test
    void classifiesFileWithoutExtensionAsBinary() {
        assertEquals(FileKind.BINARY, FileKindClassifier.classify(Paths.get("README")));
    }
}
