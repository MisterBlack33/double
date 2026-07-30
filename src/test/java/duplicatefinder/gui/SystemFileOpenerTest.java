package duplicatefinder.gui;

import org.junit.jupiter.api.Test;

import java.awt.Desktop;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SystemFileOpenerTest {

    @Test
    void returnsFalseWhenDesktopNotSupported() {
        org.junit.jupiter.api.Assumptions.assumeFalse(Desktop.isDesktopSupported(),
                "Test nur relevant in Headless-Umgebung ohne Desktop-Support");
        Path anyFile = Paths.get("irrelevant.mp4");

        assertFalse(SystemFileOpener.open(anyFile));
    }
}