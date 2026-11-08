package de.raindancer.modules.conflict.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Everybody's choice, in memory: Core asks on every hit and every refused block, from any region thread. The store
 * writes behind it.
 *
 * <p>A switch takes effect at once and is then locked for the cooldown (a week), so nobody switches raiding off
 * right after being attacked. Raiding includes PvP: a raider anyone could not hit back would raid for free.
 */
public final class Stances {

    public enum Outcome { CHANGED, ALREADY, LOCKED }

    /** @param lockedUntil when it may change again (LOCKED), else 0 */
    public record Result(Outcome outcome, Stance stance, long lockedUntil) {
    }

    private final Map<UUID, Stance> byPlayer = new ConcurrentHashMap<>();

    public Stance of(UUID player) {
        Stance stance = byPlayer.get(player);
        return stance == null ? Stance.none(player) : stance;
    }

    /** PvP in effect: chosen, or included in raiding. */
    public boolean pvp(UUID player) {
        Stance stance = byPlayer.get(player);
        return stance != null && (stance.pvp() || stance.raid());
    }

    /** The PvP switch itself, without raiding's. */
    public boolean choseOwnPvp(UUID player) {
        return of(player).pvp();
    }

    public boolean raids(UUID player) {
        Stance stance = byPlayer.get(player);
        return stance != null && stance.raid();
    }

    /**
     * @param force staff, past the lock
     */
    public synchronized Result change(UUID player, Stance.Kind kind, boolean on, long now, long cooldownMillis,
                                      boolean force) {
        Stance before = of(player);
        if (before.chose(kind) == on) {
            return new Result(Outcome.ALREADY, before, 0);
        }
        long last = before.changedAt(kind);
        if (!force && last > 0 && now < last + cooldownMillis) {
            return new Result(Outcome.LOCKED, before, last + cooldownMillis);
        }
        Stance after = before.with(kind, on, now);
        byPlayer.put(player, after);
        return new Result(Outcome.CHANGED, after, 0);
    }

    public void replaceAll(Collection<Stance> loaded) {
        Map<UUID, Stance> fresh = new ConcurrentHashMap<>();
        loaded.forEach(stance -> fresh.put(stance.player(), stance));
        synchronized (this) {
            byPlayer.clear();
            byPlayer.putAll(fresh);
        }
    }

    public static boolean mayFight(Stances stances, UUID attacker, UUID victim) {
        return stances.pvp(attacker) && stances.pvp(victim);
    }

    /** Open to this raider: every owner raids too. Never their own claim (they have it already), never ownerless ground. */
    public static boolean mayRaid(Stances stances, UUID raider, List<UUID> owners) {
        if (owners == null || owners.isEmpty() || owners.contains(raider) || !stances.raids(raider)) {
            return false;
        }
        for (UUID owner : owners) {
            if (!stances.raids(owner)) {
                return false;
            }
        }
        return true;
    }
}
