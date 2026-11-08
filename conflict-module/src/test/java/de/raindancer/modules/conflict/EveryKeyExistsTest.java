package de.raindancer.modules.conflict;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every key the code names is in messages.yml, read the way the server reads it — including the ones StanceCommand
 * builds as conflict.pvp-* / conflict.raid-*.
 */
class EveryKeyExistsTest {

    @Test
    @DisplayName("every conflict.* key in the source is defined, both switches' too")
    void defined() throws IOException {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                Path.of("src/main/resources/de/raindancer/modules/conflict/messages.yml").toFile());
        Pattern literal = Pattern.compile("\"(conflict\\.[a-z0-9.-]+)\"");
        Pattern built = Pattern.compile("k\\(\"([a-z-]+)\"\\)");
        List<String> used = new ArrayList<>();
        try (Stream<Path> sources = Files.walk(Path.of("src/main/java"))) {
            for (Path path : sources.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(path);
                literal.matcher(text).results().forEach(found -> used.add(found.group(1)));
                built.matcher(text).results().forEach(found -> {
                    used.add("conflict.pvp-" + found.group(1));
                    used.add("conflict.raid-" + found.group(1));
                });
            }
        }
        assertThat(used).as("the scan found no keys, so it checks nothing").hasSizeGreaterThan(20);
        assertThat(used.stream().distinct()).allSatisfy(name -> assertThat(yaml.isString(name)).as(name).isTrue());
    }
}
