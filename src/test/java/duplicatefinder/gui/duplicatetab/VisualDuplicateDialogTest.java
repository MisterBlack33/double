package duplicatefinder.gui.duplicatetab;

import duplicatefinder.scan.VisualDuplicateGroup;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualDuplicateDialogTest {

    @Test
    void buildMessageListsGroupWithHammingDistanceAndPaths() {
        Path a = Paths.get("a.jpg");
        Path b = Paths.get("b.png");
        var group = new VisualDuplicateGroup(List.of(a, b), 3);

        String message = VisualDuplicateDialog.buildMessage(List.of(group));

        assertTrue(message.contains("1 visuelle Gruppe(n)"));
        assertTrue(message.contains("Hamming-Distanz 3"));
        assertTrue(message.contains(a.toAbsolutePath().toString()));
        assertTrue(message.contains(b.toAbsolutePath().toString()));
    }

    @Test
    void buildMessageTruncatesAfterFifteenGroups() {
        List<VisualDuplicateGroup> groups = IntStream.range(0, 20)
                .mapToObj(i -> new VisualDuplicateGroup(List.of(Paths.get("f" + i + ".jpg")), 0))
                .toList();

        String message = VisualDuplicateDialog.buildMessage(groups);

        assertTrue(message.contains("… und weitere Gruppen"));
    }
}