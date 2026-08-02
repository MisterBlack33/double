package duplicatefinder.match;

import java.nio.file.Path;

/** Vorberechnete visuelle Merkmale eines Bildes für den Ähnlichkeitsvergleich. */
record VisualFingerprint(Path path, long pHash, int width, int height, double[] histogram) {}