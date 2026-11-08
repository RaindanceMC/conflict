package de.raindancer.modules.conflict.rules;

import de.raindancer.core.world.protection.LandAction;
import de.raindancer.core.world.protection.LandRule;
import de.raindancer.core.world.protection.ProtectedArea;
import de.raindancer.modules.conflict.model.Stances;
import org.bukkit.entity.Player;

import java.util.function.BooleanSupplier;

/**
 * Core's {@code Land.alsoAllow}: in a claim whose owners all raid, a raider may do whatever the owner may — every
 * {@link LandAction}. Managing the claim (its settings, members, shape) is not a land action and stays the owners'.
 */
public final class RaidRule implements LandRule {

    private final Stances stances;
    private final BooleanSupplier enabled;

    public RaidRule(Stances stances, BooleanSupplier enabled) {
        this.stances = stances;
        this.enabled = enabled;
    }

    @Override
    public boolean allows(Player player, ProtectedArea area, LandAction action) {
        return enabled.getAsBoolean() && player != null && area != null
                && Stances.mayRaid(stances, player.getUniqueId(), area.owners());
    }
}
