package de.raindancer.modules.conflict;

import de.raindancer.core.ui.help.HelpLint;
import de.raindancer.modules.api.ModuleCommand;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GuideTest {

    @Test
    void theGuidesAreComplete() {
        assertThat(HelpLint.of(Path.of("src/main/resources/rains-help"))
                .module("conflict")
                .settings(ConflictSettings.class, ConflictSettings.DEFAULTS)
                .commands(new ConflictModule().commands().stream().map(ModuleCommand::name).toList())
                .problems()).isEmpty();
    }
}
