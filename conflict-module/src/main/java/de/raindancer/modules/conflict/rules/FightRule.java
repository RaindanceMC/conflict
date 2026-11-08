package de.raindancer.modules.conflict.rules;

import de.raindancer.core.world.combat.Attack;
import de.raindancer.core.world.combat.Verdict;
import de.raindancer.modules.conflict.model.Stances;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Core's {@code Combat.alsoAsk}: two players who did not both choose PvP may not hurt each other. Both chose: no
 * opinion, so a claim's safe flag or a peaceful world still has its say.
 */
public final class FightRule implements Function<Attack, Verdict> {

    private final Stances stances;
    private final BooleanSupplier enabled;

    public FightRule(Stances stances, BooleanSupplier enabled) {
        this.stances = stances;
        this.enabled = enabled;
    }

    @Override
    public Verdict apply(Attack attack) {
        if (!enabled.getAsBoolean() || attack == null || !attack.isPlayerVersusPlayer()) {
            return null;
        }
        UUID attacker = attack.attackerOf().orElse(null);
        UUID victim = attack.victimOf().orElse(null);
        if (attacker == null || victim == null) {
            return null;
        }
        return Stances.mayFight(stances, attacker, victim) ? null : Verdict.NO_PVP;
    }
}
