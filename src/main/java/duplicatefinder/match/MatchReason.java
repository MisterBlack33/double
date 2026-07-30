package duplicatefinder.match;

/** Grund, warum zwei Dateien als (mögliches) Duplikat markiert wurden. */
public enum MatchReason {
    /** Bit-identischer Inhalt. */
    EXACT,
    /** Gleicher visueller/inhaltlicher Inhalt, anderes Dateiformat (z. B. PNG vs. JPG, MP4 vs. GIF). */
    FORMAT_VARIANT,
    /** Gleicher Inhalt, andere Auflösung/Bitrate. */
    RESOLUTION_VARIANT,
    /** Datei A ist ein Bildausschnitt / Videoclip aus Datei B. */
    PARTIAL_EXTRACT,
    /** Allgemein visuell ähnlich, ohne klare Zuordnung zu einem der obigen Fälle. */
    SIMILAR_CONTENT
}
