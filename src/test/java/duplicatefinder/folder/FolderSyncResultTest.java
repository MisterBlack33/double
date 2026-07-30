package duplicatefinder.folder;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FolderSyncResultTest {

    private FolderSyncResult.FileEntry entry(FolderSyncResult.MatchStatus status) {
        return new FolderSyncResult.FileEntry(Paths.get("a"), Paths.get("b"), 1L, 1L, status, -1);
    }

    @Test
    void classifiesDuplicateEntry() {
        var e = entry(FolderSyncResult.MatchStatus.DUPLICATE);
        assertTrue(e.isDuplicate());
        assertFalse(e.needsReview());
        assertFalse(e.isConflict());
        assertFalse(e.isVisualMatch());
    }

    @Test
    void classifiesNeedsReviewEntry() {
        var e = entry(FolderSyncResult.MatchStatus.NEEDS_REVIEW);
        assertTrue(e.needsReview());
        assertFalse(e.isDuplicate());
    }

    @Test
    void classifiesConflictEntry() {
        var e = entry(FolderSyncResult.MatchStatus.CONFLICT);
        assertTrue(e.isConflict());
    }

    @Test
    void classifiesAllVisualStatusesAsVisualMatch() {
        for (var status : List.of(
                FolderSyncResult.MatchStatus.VISUAL_IDENTICAL,
                FolderSyncResult.MatchStatus.VISUAL_NEAR_IDENTICAL,
                FolderSyncResult.MatchStatus.VISUAL_SIMILAR,
                FolderSyncResult.MatchStatus.VISUAL_POSSIBLY_SIMILAR)) {
            assertTrue(entry(status).isVisualMatch(), status + " sollte als visueller Treffer zählen");
        }
    }

    @Test
    void countByStatusAndCountVisualAggregateCorrectly() {
        var result = new FolderSyncResult(Paths.get("s"), Paths.get("t"),
                List.of(entry(FolderSyncResult.MatchStatus.DUPLICATE),
                        entry(FolderSyncResult.MatchStatus.VISUAL_IDENTICAL),
                        entry(FolderSyncResult.MatchStatus.VISUAL_SIMILAR)),
                3, 3, 0);

        assertEquals(1, result.countByStatus(FolderSyncResult.MatchStatus.DUPLICATE));
        assertEquals(2, result.countVisual());
    }
}
