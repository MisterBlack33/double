package duplicatefinder;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeletionConfirmationDialogTest {

    @Test
    void buildMessageListsFilesWithGroupLabel() {
        Path file = Paths.get("a.txt");
        var group = new DeletionConfirmationDialog.Group("[Quelle] ", List.of(file));

        String message = DeletionConfirmationDialog.buildMessage(List.of(group), 1);

        assertTrue(message.contains("1 Datei(en)"));
        assertTrue(message.contains("[Quelle]"));
        assertTrue(message.contains(file.toAbsolutePath().toString()));
    }

    @Test
    void buildMessageTruncatesAfterTwentyEntries() {
        List<Path> many = java.util.stream.IntStream.range(0, 25)
                .mapToObj(i -> Paths.get("file" + i + ".txt"))
                .toList();
        var group = new DeletionConfirmationDialog.Group("", many);

        String message = DeletionConfirmationDialog.buildMessage(List.of(group), many.size());

        assertTrue(message.contains("… und weitere"));
    }

    @Test
    void confirmReturnsFalseWithoutShowingDialogWhenNoFilesSelected() {
        var group = new DeletionConfirmationDialog.Group("", List.of());

        assertFalse(DeletionConfirmationDialog.confirm(null, List.of(group)));
    }
}