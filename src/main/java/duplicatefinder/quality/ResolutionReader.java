// src/main/java/duplicatefinder/quality/ResolutionReader.java
package duplicatefinder.quality;

import java.nio.file.Path;

/** Liefert die Pixelzahl (Breite x Höhe) einer Bild-/Videodatei; austauschbar für Tests. */
public interface ResolutionReader {
    long pixelCount(Path file);
}