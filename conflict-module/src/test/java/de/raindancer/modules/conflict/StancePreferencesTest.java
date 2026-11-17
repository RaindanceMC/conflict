package de.raindancer.modules.conflict;

import de.raindancer.core.platform.log.LogChannel;
import de.raindancer.core.ui.messages.Messages;
import de.raindancer.core.ui.preference.Preference;
import de.raindancer.modules.api.ModuleCommand;
import de.raindancer.modules.conflict.model.Stance;
import de.raindancer.modules.conflict.model.Stances;
import de.raindancer.modules.conflict.preference.StancePreferences;
import de.raindancer.modules.conflict.store.StanceStore;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The /preferences switches change a stance through exactly what /pvp and /raid use. */
class StancePreferencesTest {

    private static final UUID ADA = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final long DAY = 24L * 3600 * 1000;

    private final Stances stances = new Stances();
    private final Messages messages = mock(Messages.class);
    private final AtomicReference<ConflictSettings> settings = new AtomicReference<>(new ConflictSettings(true, true, 7));
    private final AtomicReference<Long> now = new AtomicReference<>(10 * DAY);
    private final Player ada = mock(Player.class);
    private ConflictServices live;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        when(ada.getUniqueId()).thenReturn(ADA);
        when(ada.getName()).thenReturn("Ada");
        live = new ConflictServices(plugin, mock(Server.class), mock(LogChannel.class), messages, settings::get, stances,
                mock(StanceStore.class), now::get);
    }

    private Preference preference(String id) {
        return StancePreferences.of(live).stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("PvP and raiding are both offered, as On/Off switches")
    void offered() {
        List<Preference> all = StancePreferences.of(live);

        assertThat(all).extracting(Preference::id).containsExactly("conflict:pvp", "conflict:raid");
        assertThat(all).allSatisfy(p -> assertThat(p.options()).containsExactly("On", "Off"));
        assertThat(preference("conflict:pvp").title()).isEqualTo("Fight other players");
    }

    @Test
    @DisplayName("the switch shows what the player chose")
    void readsTheChoice() {
        assertThat(preference("conflict:pvp").state(ada)).isEqualTo("Off");
        stances.change(ADA, Stance.Kind.PVP, true, now.get(), 0, false);

        assertThat(preference("conflict:pvp").state(ada)).isEqualTo("On");
        assertThat(preference("conflict:raid").state(ada)).isEqualTo("Off");
    }

    @Test
    @DisplayName("choosing On changes the stance and says what /pvp on says")
    void choosingOn() {
        preference("conflict:pvp").next(ada);

        assertThat(stances.choseOwnPvp(ADA)).isTrue();
        verify(messages).send(eq(ada), eq("conflict.pvp-switched-on"), any(Object[].class));
    }

    @Test
    @DisplayName("the lock holds: a change inside the cooldown is refused with the command's message")
    void lockHolds() {
        preference("conflict:raid").next(ada);
        now.set(now.get() + DAY);

        preference("conflict:raid").next(ada);

        assertThat(stances.raids(ADA)).isTrue();
        verify(messages).send(eq(ada), eq("conflict.locked"), any(Object[].class));
    }

    @Test
    @DisplayName("raiding is not offered when Conflict is off; PvP not when only the PvP switch is off")
    void shownOnlyWhereTheServerLetsThemChoose() {
        assertThat(preference("conflict:pvp").shown().test(ada)).isTrue();
        assertThat(preference("conflict:raid").shown().test(ada)).isTrue();

        settings.set(new ConflictSettings(true, false, 7));
        assertThat(preference("conflict:pvp").shown().test(ada)).isFalse();
        assertThat(preference("conflict:raid").shown().test(ada)).isTrue();

        settings.set(new ConflictSettings(false, true, 7));
        assertThat(preference("conflict:pvp").shown().test(ada)).isFalse();
        assertThat(preference("conflict:raid").shown().test(ada)).isFalse();
    }

    @Test
    @DisplayName("a switch the server has turned off cannot be changed through a stale dialog either")
    void refusedWhenOff() {
        settings.set(new ConflictSettings(false, true, 7));

        preference("conflict:pvp").next(ada);

        assertThat(stances.choseOwnPvp(ADA)).isFalse();
        verify(messages).send(ada, "conflict.not-enabled");
    }

    @Test
    @DisplayName("/pvp and /raid are still Conflict's")
    void commandsStay() {
        assertThat(ConflictCommands.declared()).extracting(ModuleCommand::name).containsExactly("pvp", "raid");
    }
}
