package de.raindancer.modules.conflict.command;

import de.raindancer.core.platform.util.Times;
import de.raindancer.modules.conflict.ConflictServices;
import de.raindancer.modules.conflict.model.Stance;
import de.raindancer.modules.conflict.model.Stances;
import de.raindancer.modules.conflict.util.PermissionNodes;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** {@code /pvp} and {@code /raid}: where you stand, {@code on|off}; staff {@code <player> on|off}, past the lock. */
public final class StanceCommand implements BasicCommand {

    private final Stance.Kind kind;
    private final Supplier<ConflictServices> services;

    public StanceCommand(Stance.Kind kind, Supplier<ConflictServices> services) {
        this.kind = kind;
        this.services = services;
    }

    private String k(String key) {
        return "conflict." + (kind == Stance.Kind.PVP ? "pvp-" : "raid-") + key;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        ConflictServices live = services.get();
        CommandSender sender = source.getSender();
        if (!live.settings().get().enabled()) {
            live.messages().send(sender, "conflict.not-enabled");
            return;
        }
        String first = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (first.equals("on") || first.equals("off") || first.equals("status")) {
            if (!(sender instanceof Player self)) {
                live.messages().send(sender, "conflict.only-a-player");
                return;
            }
            if (first.equals("status")) {
                status(live, self);
            } else {
                change(live, sender, self, first.equals("on"), false);
            }
            return;
        }
        if (!sender.hasPermission(PermissionNodes.OTHERS) || args.length < 2
                || !(args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            live.messages().send(sender, k("usage"));
            return;
        }
        OfflinePlayer target = live.server().getOfflinePlayerIfCached(args[0]);
        if (target == null) {
            live.messages().send(sender, "conflict.unknown-player", "player", args[0]);
            return;
        }
        change(live, sender, target, args[1].equalsIgnoreCase("on"), true);
    }

    private void status(ConflictServices live, Player self) {
        Stances stances = live.stances();
        Stance stance = stances.of(self.getUniqueId());
        boolean on = kind == Stance.Kind.PVP ? stances.pvp(self.getUniqueId()) : stance.raid();
        String key = on ? k("status-on") : k("status-off");
        if (kind == Stance.Kind.PVP && on && !stance.pvp()) {
            key = "conflict.pvp-status-raiding";
        }
        long next = stance.changedAt(kind) + live.settings().get().cooldownMillis();
        long now = live.clock().getAsLong();
        live.messages().send(self, key);
        if (stance.changedAt(kind) > 0 && now < next) {
            live.messages().send(self, "conflict.locked-until", "time", Times.describe(Duration.ofMillis(next - now)));
        }
    }

    private void change(ConflictServices live, CommandSender sender, OfflinePlayer target, boolean on, boolean force) {
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
                if (kind == Stance.Kind.PVP && !on && live.stances().raids(target.getUniqueId())) {
                    live.messages().send(sender, "conflict.pvp-held-by-raiding");
                }
                live.log().info("{} chose {} {} for {}{}.", sender.getName(), kind == Stance.Kind.PVP ? "PvP" : "raiding",
                        on ? "on" : "off", name, force ? " (staff, past the lock)" : "");
            }
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (args.length <= 1) {
            options.addAll(List.of("on", "off", "status"));
            if (sender.hasPermission(PermissionNodes.OTHERS)) {
                services.get().server().getOnlinePlayers().forEach(player -> options.add(player.getName()));
            }
        } else if (args.length == 2 && sender.hasPermission(PermissionNodes.OTHERS)) {
            options.addAll(List.of("on", "off"));
        }
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(typed)).toList();
    }

    @Override
    public String permission() {
        return PermissionNodes.USE;
    }
}
