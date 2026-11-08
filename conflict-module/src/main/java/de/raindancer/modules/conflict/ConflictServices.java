package de.raindancer.modules.conflict;

import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.modules.conflict.model.Stance;
import de.raindancer.modules.conflict.model.Stances;
import de.raindancer.modules.conflict.store.StanceStore;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** What this module built, handed to its commands. */
public record ConflictServices(
        Plugin plugin,
        Server server,
        LogChannel log,
        Messages messages,
        Supplier<ConflictSettings> settings,
        Stances stances,
        StanceStore store,
        LongSupplier clock) {

    /** Changed in memory at once (Core asks from there), written behind. */
    public Stances.Result change(UUID player, Stance.Kind kind, boolean on, boolean force) {
        Stances.Result result = stances.change(player, kind, on, clock.getAsLong(), settings.get().cooldownMillis(), force);
        if (result.outcome() == Stances.Outcome.CHANGED) {
            Stance changed = result.stance();
            Scheduling.async(plugin, () -> {
                if (!store.save(changed)) {
                    log.warn("Could not save {}'s PvP/raiding choice; it holds until the next restart.", player);
                }
            });
        }
        return result;
    }
}
