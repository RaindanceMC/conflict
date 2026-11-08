package de.raindancer.modules.conflict;

import de.raindancer.core.data.settings.Describe;
import de.raindancer.core.data.settings.In;
import de.raindancer.core.data.settings.Key;
import de.raindancer.core.data.settings.Range;
import de.raindancer.core.data.settings.Settings;
import de.raindancer.core.data.settings.Title;
import de.raindancer.core.data.settings.Topic;
import org.bukkit.Material;

/** What an owner decides about opt-in PvP and raiding. The schema of the file and of the /settings page. */
@Settings(id = "conflict", topics = {
        @Topic(path = "conflict", title = "PvP and raiding", icon = Material.TNT),
})
public record ConflictSettings(

        @In("conflict") @Title("PvP and raiding are opt-in")
        @Describe("On: two players hurt each other only when both chose PvP (/pvp), and raiders (/raid) may do "
                + "everything an owner may in the claims of other raiders - their own claims are open to them in "
                + "return. Off: the server's and the claims' own PvP rules apply, and claims are closed as usual.")
        @Key("enabled")
        boolean enabled,

        @In("conflict") @Title("PvP is opt-in (/pvp)")
        @Describe("On: two players fight only when both chose PvP with /pvp. Off: /pvp is switched off and PvP "
                + "follows the server's and the claims' own rules again, for everybody; raiding stays opt-in.")
        @Key("pvp-opt-in")
        boolean pvpOptIn,

        @In("conflict") @Title("Days between two changes") @Range(min = 0, max = 60)
        @Describe("How long a switch stays as it is after somebody changed it, so nobody switches raiding off "
                + "right after an attack. Each switch has its own lock. Staff may change it for somebody anyway.")
        @Key("change-every-days")
        int changeEveryDays) {

    public static final ConflictSettings DEFAULTS = new ConflictSettings(false, true, 7);

    public long cooldownMillis() {
        return changeEveryDays * 24L * 3600 * 1000;
    }
}
