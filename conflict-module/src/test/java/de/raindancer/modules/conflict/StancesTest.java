package de.raindancer.modules.conflict;

import de.raindancer.modules.conflict.model.Stance;
import de.raindancer.modules.conflict.model.Stances;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Who chose PvP and raiding, and the week-long lock on changing it. */
class StancesTest {

    private static final long WEEK = 7L * 24 * 3600 * 1000;
    private static final UUID ADA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID BO = UUID.fromString("00000000-0000-0000-0000-0000000000b0");
    private static final UUID CY = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    private final Stances stances = new Stances();

    @Test
    @DisplayName("nobody chose anything: no PvP, no raids")
    void defaults() {
        assertThat(stances.pvp(ADA)).isFalse();
        assertThat(stances.raids(ADA)).isFalse();
        assertThat(Stances.mayFight(stances, ADA, BO)).isFalse();
    }

    @Test
    @DisplayName("PvP only between two who both chose it")
    void pvpBothWays() {
        stances.change(ADA, Stance.Kind.PVP, true, 1_000, WEEK, false);
        assertThat(Stances.mayFight(stances, ADA, BO)).isFalse();
        stances.change(BO, Stance.Kind.PVP, true, 1_000, WEEK, false);
        assertThat(Stances.mayFight(stances, ADA, BO)).isTrue();
        assertThat(Stances.mayFight(stances, BO, ADA)).isTrue();
    }

    @Test
    @DisplayName("a change takes effect at once, then the switch is locked for the week; the same choice again costs nothing")
    void weeklyLock() {
        assertThat(stances.change(ADA, Stance.Kind.RAID, true, 1_000, WEEK, false).outcome())
                .isEqualTo(Stances.Outcome.CHANGED);
        assertThat(stances.raids(ADA)).isTrue();
        assertThat(stances.change(ADA, Stance.Kind.RAID, true, 2_000, WEEK, false).outcome())
                .isEqualTo(Stances.Outcome.ALREADY);
        Stances.Result locked = stances.change(ADA, Stance.Kind.RAID, false, 2_000, WEEK, false);
        assertThat(locked.outcome()).isEqualTo(Stances.Outcome.LOCKED);
        assertThat(locked.lockedUntil()).isEqualTo(1_000 + WEEK);
        assertThat(stances.raids(ADA)).as("still a raider after an attack").isTrue();

        assertThat(stances.change(ADA, Stance.Kind.RAID, false, 1_000 + WEEK, WEEK, false).outcome())
                .isEqualTo(Stances.Outcome.CHANGED);
        assertThat(stances.raids(ADA)).isFalse();
    }

    @Test
    @DisplayName("staff may change it inside the lock (somebody chose by mistake)")
    void staffForce() {
        stances.change(ADA, Stance.Kind.PVP, true, 1_000, WEEK, false);
        assertThat(stances.change(ADA, Stance.Kind.PVP, false, 2_000, WEEK, true).outcome())
                .isEqualTo(Stances.Outcome.CHANGED);
        assertThat(stances.pvp(ADA)).isFalse();
    }

    @Test
    @DisplayName("the two switches lock separately")
    void separateLocks() {
        stances.change(ADA, Stance.Kind.PVP, true, 1_000, WEEK, false);
        assertThat(stances.change(ADA, Stance.Kind.RAID, true, 2_000, WEEK, false).outcome())
                .isEqualTo(Stances.Outcome.CHANGED);
    }

    @Test
    @DisplayName("a raider fights whoever fights: raiding includes PvP, whatever the PvP switch says")
    void raidIncludesPvp() {
        stances.change(ADA, Stance.Kind.RAID, true, 1_000, WEEK, false);
        stances.change(BO, Stance.Kind.PVP, true, 1_000, WEEK, false);
        assertThat(stances.pvp(ADA)).isTrue();
        assertThat(stances.choseOwnPvp(ADA)).isFalse();
        assertThat(Stances.mayFight(stances, ADA, BO)).isTrue();
    }

    @Test
    @DisplayName("a claim is open to a raider when every owner raids too - never their own, never one without owners")
    void raidable() {
        stances.change(ADA, Stance.Kind.RAID, true, 1_000, WEEK, false);
        stances.change(BO, Stance.Kind.RAID, true, 1_000, WEEK, false);
        assertThat(Stances.mayRaid(stances, ADA, List.of(BO))).isTrue();
        assertThat(Stances.mayRaid(stances, ADA, List.of(BO, CY))).as("a co-owner who does not raid").isFalse();
        assertThat(Stances.mayRaid(stances, CY, List.of(BO))).as("not a raider").isFalse();
        assertThat(Stances.mayRaid(stances, ADA, List.of())).as("server ground").isFalse();
        assertThat(Stances.mayRaid(stances, ADA, List.of(ADA, BO))).as("their own").isFalse();
    }

    @Test
    @DisplayName("loaded rows replace what was remembered")
    void load() {
        stances.change(CY, Stance.Kind.PVP, true, 1, WEEK, false);
        stances.replaceAll(List.of(new Stance(ADA, true, false, 5, 0)));
        assertThat(stances.pvp(ADA)).isTrue();
        assertThat(stances.pvp(CY)).isFalse();
        assertThat(stances.of(ADA).pvpChangedAt()).isEqualTo(5);
    }
}
