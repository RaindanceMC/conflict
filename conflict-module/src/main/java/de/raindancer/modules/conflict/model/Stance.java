package de.raindancer.modules.conflict.model;

import java.util.UUID;

/** What one player chose, and when they last changed each switch. Nobody's row = neither. */
public record Stance(UUID player, boolean pvp, boolean raid, long pvpChangedAt, long raidChangedAt) {

    public enum Kind { PVP, RAID }

    public static Stance none(UUID player) {
        return new Stance(player, false, false, 0, 0);
    }

    public boolean chose(Kind kind) {
        return kind == Kind.PVP ? pvp : raid;
    }

    public long changedAt(Kind kind) {
        return kind == Kind.PVP ? pvpChangedAt : raidChangedAt;
    }

    public Stance with(Kind kind, boolean on, long now) {
        return kind == Kind.PVP ? new Stance(player, on, raid, now, raidChangedAt)
                : new Stance(player, pvp, on, pvpChangedAt, now);
    }
}
