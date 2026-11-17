package de.raindancer.modules.conflict.preference;

import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.conflict.ConflictServices;
import de.raindancer.modules.conflict.StanceChange;
import de.raindancer.modules.conflict.model.Stance;

import java.util.List;

/** {@code /pvp} and {@code /raid} on RainsCore's {@code /preferences} dialog. Needs RainsCore 2.103.0. */
public final class StancePreferences {

    private StancePreferences() {
    }

    public static List<Preference> of(ConflictServices live) {
        return List.of(
                of(live, Stance.Kind.PVP, "conflict:pvp", "Fight other players",
                        "Whether other players may attack you, where the server leaves it to you."),
                of(live, Stance.Kind.RAID, "conflict:raid", "Raid bases",
                        "Whether you may break into the claims of other raiders, and they into yours."));
    }

    static Preference of(ConflictServices live, Stance.Kind kind, String id, String title, String description) {
        StanceChange change = new StanceChange(kind, live);
        return Preference.toggle(id, "PvP", title, description,
                player -> live.stances().of(player.getUniqueId()).chose(kind),
                change::choose)
                .shownTo(player -> change.available());
    }
}
