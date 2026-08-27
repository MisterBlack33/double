package duplicatefinder.audio;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Extrahiert die Bitrate einer Audiodatei via Apache Tika. */
final class TikaBitrateReader {

    private static final String BITRATE_KEY = "bitrate";

    private TikaBitrateReader() {}

    static int read(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            Metadata metadata = new Metadata();
            new AutoDetectParser().parse(is, new BodyContentHandler(-1), metadata, new ParseContext());
            return parseKbps(metadata.get(BITRATE_KEY));
        } catch (Exception e) {
            // Fehlende/kaputte Metadaten dürfen das Ranking nicht abbrechen.
            return 0;
        }
    }

    private static int parseKbps(String raw) {
        if (raw == null) return 0;
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        return digitsOnly.isEmpty() ? 0 : Integer.parseInt(digitsOnly);
    }
}