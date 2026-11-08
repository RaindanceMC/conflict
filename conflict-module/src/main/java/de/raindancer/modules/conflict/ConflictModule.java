package de.raindancer.modules.conflict;

import de.raindancer.core.data.settings.SettingsStore;
import de.raindancer.core.platform.util.Scheduling;
import de.raindancer.modules.api.FlexModule;
import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.api.ModuleContext;
import de.raindancer.modules.api.ModuleInfo;
import de.raindancer.modules.conflict.model.Stances;
import de.raindancer.modules.conflict.rules.FightRule;
import de.raindancer.modules.conflict.rules.RaidRule;
import de.raindancer.modules.conflict.store.StanceStore;
import de.raindancer.modules.conflict.util.PermissionNodes;
import de.raindancer.core.RainsCore;

import java.util.List;

/**
 * PvP and raiding, opted into by each player. Two rules handed to Core — who may fight ({@code Combat.alsoAsk}) and
 * who may do what in whose claim ({@code Land.alsoAllow}) — both answered from memory; the choices on disk behind.
 */
public final class ConflictModule implements FlexModule {

    private static final ModuleInfo INFO = ModuleInfo.of("conflict", "Conflict", "0.1.0")
            .describedAs("PvP and raiding, opt-in: /pvp and /raid")
            .by("Raindancer118");

    private RainsCore core;
    private FightRule fights;
    private RaidRule raids;

    @Override
    public ModuleInfo info() {
        return INFO;
    }

    @Override
    public void enable(ModuleContext context) {
        SettingsStore<ConflictSettings> settings = context.settings(ConflictSettings.class, ConflictSettings.DEFAULTS);
        core = context.core();
        core.messages().defineFrom(ConflictModule.class.getResourceAsStream("messages.yml"),
                context.chat().brand()::chatPrefix);
        core.messages().seriousFrom(ConflictModule.class.getResourceAsStream("messages-serious.yml"));
        int registered = PermissionNodes.register(context.plugin().getServer());
        if (registered > 0) {
            context.log().info("{} permission(s) registered.", registered);
        }
        Stances stances = new Stances();
        StanceStore store = new StanceStore(core.databases().of(StanceStore.NAME, StanceStore.SCHEMA));
        // Until the choices are read nobody fights or raids: the safe side of not knowing.
        Scheduling.async(context.plugin(), () -> store.all().ifPresentOrElse(loaded -> {
            stances.replaceAll(loaded);
            context.log().info("{} player(s) chose PvP or raiding.", loaded.size());
        }, () -> context.log().warn("Could not read who chose PvP or raiding; nobody fights or raids until a restart reads it.")));
        fights = new FightRule(stances, () -> settings.current().enabled());
        raids = new RaidRule(stances, () -> settings.current().enabled());
        core.combat().alsoAsk(context.plugin().getName(), fights);
        core.land().alsoAllow(raids);
        core.messages().overrideFor(context.plugin().getName(), "combat.no-pvp", "conflict.no-pvp");
        ConflictCommands.ready(new ConflictServices(context.plugin(), context.plugin().getServer(), context.log(),
                core.messages(), settings::current, stances, store, System::currentTimeMillis));
        context.log().info("Conflict is up: PvP and raiding are {}, a switch changes every {} day(s).",
                settings.current().enabled() ? "opt-in" : "off (the server's own rules apply)",
                settings.current().changeEveryDays());
    }

    @Override
    public List<ModuleCommand> commands() {
        return ConflictCommands.declared();
    }

    @Override
    public void disable() {
        ConflictCommands.stopped();
        if (core != null) {
            core.combat().stopAsking(fights);
            core.land().stopAllowing(raids);
        }
    }
}
