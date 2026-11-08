package de.raindancer.modules.conflict;

import de.raindancer.core.world.combat.Attack;
import de.raindancer.core.world.combat.Verdict;
import de.raindancer.core.world.protection.LandAction;
import de.raindancer.core.world.protection.ProtectedArea;
import de.raindancer.modules.conflict.model.Stance;
import de.raindancer.modules.conflict.model.Stances;
import de.raindancer.modules.conflict.rules.FightRule;
import de.raindancer.modules.conflict.rules.RaidRule;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The two rules Core asks: may these two fight, may this raider do that in this claim. */
class RulesTest {

    private static final long WEEK = 7L * 24 * 3600 * 1000;
    private static final UUID ADA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BO = UUID.fromString("00000000-0000-0000-0000-0000000000b0");

    private final Stances stances = new Stances();
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicBoolean pvpOptIn = new AtomicBoolean(true);
    private final FightRule fights = new FightRule(stances, enabled::get, pvpOptIn::get);
    private final RaidRule raids = new RaidRule(stances, enabled::get);

    private static Player player(UUID id) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(id);
        return player;
    }

    private static ProtectedArea ownedBy(UUID... owners) {
        ProtectedArea area = mock(ProtectedArea.class);
        when(area.owners()).thenReturn(List.of(owners));
        return area;
    }

    @Test
    @DisplayName("two players who did not both choose PvP: refused; both chose: no opinion (claims and worlds still decide)")
    void fight() {
        Attack attack = Attack.between(ADA, BO, "world");
        assertThat(fights.apply(attack)).isEqualTo(Verdict.NO_PVP);
        stances.change(ADA, Stance.Kind.PVP, true, 1, WEEK, false);
        assertThat(fights.apply(attack)).isEqualTo(Verdict.NO_PVP);
        stances.change(BO, Stance.Kind.PVP, true, 1, WEEK, false);
        assertThat(fights.apply(attack)).isNull();
    }

    @Test
    @DisplayName("mobs are none of its business, and switched off it has no opinion at all")
    void notItsBusiness() {
        assertThat(fights.apply(Attack.onMob(ADA, UUID.randomUUID(), "world"))).isNull();
        assertThat(fights.apply(Attack.byMob(UUID.randomUUID(), ADA, "world"))).isNull();
        enabled.set(false);
        assertThat(fights.apply(Attack.between(ADA, BO, "world"))).isNull();
    }

    @Test
    @DisplayName("a raider in another raider's claim may do everything the owner may - every action")
    void raid() {
        stances.change(ADA, Stance.Kind.RAID, true, 1, WEEK, false);
        stances.change(BO, Stance.Kind.RAID, true, 1, WEEK, false);
        for (LandAction action : LandAction.values()) {
            assertThat(raids.allows(player(ADA), ownedBy(BO), action)).as(action.name()).isTrue();
        }
        assertThat(raids.allows(player(ADA), ownedBy(UUID.randomUUID()), LandAction.BREAK)).isFalse();
        enabled.set(false);
        assertThat(raids.allows(player(ADA), ownedBy(BO), LandAction.BREAK)).isFalse();
    }

    @Test
    @DisplayName("the PvP switch turned off server-side: no opinion on any fight (server and claims decide), raids still work")
    void pvpSwitchOff() {
        pvpOptIn.set(false);
        assertThat(fights.apply(Attack.between(ADA, BO, "world"))).isNull();
        stances.change(ADA, Stance.Kind.RAID, true, 1, WEEK, false);
        stances.change(BO, Stance.Kind.RAID, true, 1, WEEK, false);
        assertThat(raids.allows(player(ADA), ownedBy(BO), LandAction.BREAK)).isTrue();
    }
}
