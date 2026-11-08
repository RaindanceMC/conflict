package de.raindancer.modules.conflict;

import de.raindancer.modules.api.WordingContract;

import java.nio.file.Path;

/** The wording rules, kept by this module. See {@code WordingContract} for what is actually checked. */
class WordingTest implements WordingContract {

    @Override
    public Path moduleSource() {
        return Path.of("src/main/java/de/raindancer/modules/conflict");
    }

    @Override
    public Path messagesFile() {
        return Path.of("src/main/resources/de/raindancer/modules/conflict/messages.yml");
    }

    @Override
    public int fewestWordingLines() {
        return 20;
    }

    @Override
    public int fewestSourceFiles() {
        return 10;
    }
}
