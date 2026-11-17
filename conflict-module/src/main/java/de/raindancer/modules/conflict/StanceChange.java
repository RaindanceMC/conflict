package de.raindancer.modules.conflict;

import de.raindancer.core.platform.util.Times;
import de.raindancer.modules.conflict.model.Stance;
import de.raindancer.modules.conflict.model.Stances;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;

/**
 * One switch, changed with all its rules and words: the lock, the refusals, what is told to whom, the log line.
 * {@code /pvp}, {@code /raid} and the preferences dialog all change a stance through this, so they cannot disagree.
 */
public final class StanceChange {

    private final Stance.Kind kind;
    private final ConflictServices live;

    public StanceChange(Stance.Kind kind, ConflictServices live) {
        this.kind = kind;
        this.live = live;
    }

    public static String prefix(Stance.Kind kind) {
        return "conflict." + (kind == Stance.Kind.PVP ? "pvp-" : "raid-");
    }

    private String k(String key) {
        return prefix(kind) + key;
    }

    /** Whether this switch exists on this server at all. */
    public boolean available() {
        ConflictSettings settings = live.settings().get();
        return settings.enabled() && (kind != Stance.Kind.PVP || settings.pvpOptIn());
    }

    /** Tells {@code sender} so and answers true when the switch does not exist here. */
    public boolean refuseIfOff(CommandSender sender) {
        if (!live.settings().get().enabled()) {
            live.messages().send(sender, "conflict.not-enabled");
            return true;
        }
        if (kind == Stance.Kind.PVP && !live.settings().get().pvpOptIn()) {
            live.messages().send(sender, "conflict.pvp-switch-off");
            return true;
        }
        return false;
    }

    /** A player's own choice, as {@code /pvp on|off} makes it. */
    public void choose(Player player, boolean on) {
        if (!refuseIfOff(player)) {
            apply(player, player, on, false);
        }
    }

    public void apply(CommandSender sender, OfflinePlayer target, boolean on, boolean force) {
        Stances.Result result = live.change(target.getUniqueId(), kind, on, force);
        boolean self = sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
        String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        switch (result.outcome()) {
            case ALREADY -> live.messages().send(sender, on ? k("already-on") : k("already-off"), "player", name);
            case LOCKED -> live.messages().send(sender, "conflict.locked", "time",
                    Times.describe(Duration.ofMillis(result.lockedUntil() - live.clock().getAsLong())),
                    "days", String.valueOf(live.settings().get().changeEveryDays()));
            case CHANGED -> {
                if (self) {
                    live.messages().send(sender, on ? k("switched-on") : k("switched-off"),
                            "days", String.valueOf(live.settings().get().changeEveryDays()));
                } else {
                    live.messages().send(sender, on ? k("others-on") : k("others-off"), "player", name);
                    if (target.getPlayer() != null) {
                        live.messages().send(target.getPlayer(), on ? k("switched-on") : k("switched-off"),
                                "days", String.valueOf(live.settings().get().changeEveryDays()));
                    }
                }
                if (kind == Stance.Kind.RAID && on && live.settings().get().pvpOptIn()) {
                    live.messages().send(sender, "conflict.raid-brings-pvp");
                }
                if (kind == Stance.Kind.PVP && !on && live.stances().raids(target.getUniqueId())) {
                    live.messages().send(sender, "conflict.pvp-held-by-raiding");
                }
                live.log().info("{} chose {} {} for {}{}.", sender.getName(), kind == Stance.Kind.PVP ? "PvP" : "raiding",
                        on ? "on" : "off", name, force ? " (staff, past the lock)" : "");
            }
        }
    }
}
