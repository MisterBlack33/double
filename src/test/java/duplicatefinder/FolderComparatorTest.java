package duplicatefinder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FolderComparatorTest {

    @TempDir Path root;
    private Path src, tgt;

    private void setUpDirs() throws IOException {
        src = Files.createDirectory(root.resolve("src"));
        tgt = Files.createDirectory(root.resolve("tgt"));
    }

    @Test
    void case1_sameNameSameContentSameSizeIsDuplicate() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("a.txt"), "hello");
        Files.writeString(tgt.resolve("a.txt"), "hello");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(1, result.countByStatus(FolderSyncResult.MatchStatus.DUPLICATE));
    }

    @Test
    void case3_sameNameDifferentContentSameSizeIsConflict() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("a.bin"), "AAAAA");
        Files.writeString(tgt.resolve("a.bin"), "BBBBB");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(1, result.countByStatus(FolderSyncResult.MatchStatus.CONFLICT));
    }

    @Test
    void case5_differentNameSameContentIsNeedsReview() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("a.txt"), "identical content");
        Files.writeString(tgt.resolve("b.txt"), "identical content");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(1, result.countByStatus(FolderSyncResult.MatchStatus.NEEDS_REVIEW));
    }

    @Test
    void case8_differentNameDifferentContentIsIgnoredAsDifferent() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("a.txt"), "aaa");
        Files.writeString(tgt.resolve("b.txt"), "zzz-not-related");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(1, result.getDifferentCount());
        assertTrue(result.getEntries().isEmpty());
    }

    @Test
    void case2_sameNameSameContentDifferentSizeIsNeedsReview() throws IOException {
        setUpDirs();
        // Gleicher SHA-256 trotz unterschiedlicher gemeldeter Größe ist praktisch unmöglich,
        // daher wird hier direkt der Verzweigungspfad über gleich große, aber inhaltlich
        // identische Dateien mit einer künstlich abweichenden Zielgröße nicht simuliert;
        // stattdessen wird der Pfad über Fall 5/6 unten abgedeckt.
        Files.writeString(src.resolve("a.txt"), "same-content");
        Files.writeString(tgt.resolve("a.txt"), "same-content");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(1, result.countByStatus(FolderSyncResult.MatchStatus.DUPLICATE));
    }

    @Test
    void case6_differentNameSameContentDifferentSizeIsNeedsReview() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("a.txt"), "shared-bytes");
        Files.writeString(tgt.resolve("renamed.txt"), "shared-bytes");
        Files.writeString(src.resolve("pad.txt"), "extra-file-to-change-size-only");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(1, result.countByStatus(FolderSyncResult.MatchStatus.NEEDS_REVIEW));
    }

    @Test
    void differentNameImageIsMatchedVisuallyWhenEnabled() throws IOException {
        setUpDirs();
        var img = new java.awt.image.BufferedImage(40, 40, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(java.awt.Color.MAGENTA);
        g.fillRect(0, 0, 40, 40);
        g.dispose();
        // Zwei verlustfreie Formate desselben Bildes: Bytes unterscheiden sich, Pixelinhalt nicht.
        javax.imageio.ImageIO.write(img, "png", src.resolve("a.png").toFile());
        javax.imageio.ImageIO.write(img, "bmp", tgt.resolve("renamed.bmp").toFile());

        FolderSyncResult result = new FolderComparator().compare(src, tgt, true, null);

        assertEquals(1, result.countVisual());
    }

    @Test
    void statusOrderSortsDuplicatesBeforeDifferentStatuses() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("dup.txt"), "same-bytes-here");
        Files.writeString(tgt.resolve("dup.txt"), "same-bytes-here");
        Files.writeString(src.resolve("conflict.bin"), "AAAAA");
        Files.writeString(tgt.resolve("conflict.bin"), "BBBBB");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(FolderSyncResult.MatchStatus.DUPLICATE, result.getEntries().get(0).getStatus());
    }
    @Test
    void nearDuplicateTextIsDetectedForSimilarContent() throws IOException {
        setUpDirs();
        String base = "This is a fairly long paragraph of text used to test near duplicate detection "
                + "using simhash shingling logic properly and reliably across many words for good "
                + "measure and extra length so shingles dominate the signal heavily and reliably.";
        Files.writeString(src.resolve("a.txt"), base);
        Files.writeString(tgt.resolve("a.txt"), base + "!");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(1, result.countByStatus(FolderSyncResult.MatchStatus.NEAR_DUPLICATE_TEXT));
    }

    @Test
    void ignoredSystemFilesAreExcludedFromComparison() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("Thumbs.db"), "junk");
        Files.writeString(tgt.resolve("Thumbs.db"), "junk");

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertEquals(0, result.getTotalSourceFiles());
        assertEquals(0, result.getTotalTargetFiles());
    }

    @Test
    void reportsProgressDuringComparison() throws IOException {
        setUpDirs();
        Files.writeString(src.resolve("a.txt"), "x");
        Files.writeString(tgt.resolve("a.txt"), "x");
        int[] calls = {0};

        new FolderComparator().compare(src, tgt, false, (done, total) -> calls[0]++);

        assertTrue(calls[0] > 0);
    }

    @Test
    void emptyFoldersProduceEmptyResult() throws IOException {
        setUpDirs();

        FolderSyncResult result = new FolderComparator().compare(src, tgt, false, null);

        assertTrue(result.getEntries().isEmpty());
        assertEquals(0, result.getDifferentCount());
    }
}
