package duplicatefinder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StartupModeTest {

    @Test
    void resolvesToGuiWhenNoArguments() {
        assertEquals(StartupMode.GUI, StartupMode.resolve(new String[]{}));
    }

    @Test
    void resolvesToCliWhenArgumentsPresent() {
        assertEquals(StartupMode.CLI, StartupMode.resolve(new String[]{"/some/path"}));
    }
}
