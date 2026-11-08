package de.raindancer.modules.conflict.store;

import de.raindancer.core.data.sql.Database;
import de.raindancer.core.data.sql.Schema;
import de.raindancer.modules.conflict.model.Stance;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Everybody's choice on disk. Disk work: call off the main thread. */
public final class StanceStore {

    public static final String NAME = "conflict";

    /** Steps are only ever appended to — see {@link Schema}. */
    public static final Schema SCHEMA = Schema.of(
            "CREATE TABLE stance (player TEXT PRIMARY KEY, pvp INTEGER NOT NULL DEFAULT 0, raid INTEGER NOT NULL DEFAULT 0, "
                    + "pvp_changed_at BIGINT NOT NULL DEFAULT 0, raid_changed_at BIGINT NOT NULL DEFAULT 0)");

    private final Database database;

    public StanceStore(Database database) {
        this.database = database;
    }

    /** Everybody's, or empty when the database could not be asked. */
    public Optional<List<Stance>> all() {
        return database.read(connection -> {
            List<Stance> stances = new ArrayList<>();
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT player, pvp, raid, pvp_changed_at, raid_changed_at FROM stance");
                 ResultSet rows = query.executeQuery()) {
                while (rows.next()) {
                    try {
                        stances.add(new Stance(UUID.fromString(rows.getString(1)), rows.getInt(2) != 0,
                                rows.getInt(3) != 0, rows.getLong(4), rows.getLong(5)));
                    } catch (IllegalArgumentException notAnId) {
                        // nobody plays as it
                    }
                }
            }
            return stances;
        });
    }

    public boolean save(Stance stance) {
        return database.write(connection -> {
            try (PreparedStatement row = connection.prepareStatement(
                    "INSERT INTO stance (player, pvp, raid, pvp_changed_at, raid_changed_at) VALUES (?,?,?,?,?) "
                            + "ON CONFLICT(player) DO UPDATE SET pvp = excluded.pvp, raid = excluded.raid, "
                            + "pvp_changed_at = excluded.pvp_changed_at, raid_changed_at = excluded.raid_changed_at")) {
                row.setString(1, stance.player().toString());
                row.setInt(2, stance.pvp() ? 1 : 0);
                row.setInt(3, stance.raid() ? 1 : 0);
                row.setLong(4, stance.pvpChangedAt());
                row.setLong(5, stance.raidChangedAt());
                row.executeUpdate();
            }
        });
    }
}
