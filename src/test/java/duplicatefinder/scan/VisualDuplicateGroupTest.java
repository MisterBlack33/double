package duplicatefinder.scan;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VisualDuplicateGroupTest {

    @Test
    void exposesPathsAndMaxHammingDistance() {
        List<Path> paths = List.of(Paths.get("a.jpg"), Paths.get("b.png"));

        var group = new VisualDuplicateGroup(paths, 4);

        assertEquals(paths, group.getPaths());
        assertEquals(4, group.getMaxHammingDistance());
    }

    @Test
    void pathsAreUnmodifiable() {
        var group = new VisualDuplicateGroup(List.of(Paths.get("a.jpg")), 0);

        assertThrows(UnsupportedOperationException.class, () -> group.getPaths().add(Paths.get("x")));
    }
}