package de.raindancer.modules.conflict;

import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.conflict.command.StanceCommand;
import de.raindancer.modules.conflict.model.Stance;

import java.util.List;

/**
 * Declared at bootstrap, filled in when the module starts — Paper registers commands before any plugin is enabled,
 * so the commands point at {@link #require} until then.
 */
public final class ConflictCommands {

    private static volatile ConflictServices services;

    private ConflictCommands() {
    }

    public static List<ModuleCommand> declared() {
        return List.of(
                ModuleCommand.of("pvp", "Chooses whether other players who chose PvP can fight you",
                                new StanceCommand(Stance.Kind.PVP, ConflictCommands::require))
                        .taking("(nothing) — where you stand", "on | off", "<player> on | off — for staff")
                        .needing("rainsconflict.use"),
                ModuleCommand.of("raid", "Chooses whether you raid bases - and your own can be raided",
                                new StanceCommand(Stance.Kind.RAID, ConflictCommands::require))
                        .taking("(nothing) — where you stand", "on | off", "<player> on | off — for staff")
                        .needing("rainsconflict.use"));
    }

    static void ready(ConflictServices live) {
        services = live;
    }

    static void stopped() {
        services = null;
    }

    private static ConflictServices require() {
        ConflictServices live = services;
        if (live == null) {
            throw new IllegalStateException("the conflict module is not running");
        }
        return live;
    }
}
