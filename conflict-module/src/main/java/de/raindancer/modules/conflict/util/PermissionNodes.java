package de.raindancer.modules.conflict.util;

import org.bukkit.Server;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

import java.util.List;

/** What this module asks about somebody. Registered in code, since a hosted module has no descriptor. */
public final class PermissionNodes {

    /** Choosing PvP and raiding for oneself. */
    public static final String USE = "rainsconflict.use";

    /** Changing it for somebody else, past the weekly lock. */
    public static final String OTHERS = "rainsconflict.others";

    private PermissionNodes() {
    }

    public static List<Permission> declared() {
        return List.of(
                new Permission(USE, "Choose PvP (/pvp) and raiding (/raid) for yourself", PermissionDefault.TRUE),
                new Permission(OTHERS, "Change somebody's PvP or raiding, also inside the weekly lock", PermissionDefault.OP));
    }

    /** Registers whatever is not registered yet. @return how many were added */
    public static int register(Server server) {
        if (server == null) {
            return 0;
        }
        int added = 0;
        for (Permission permission : declared()) {
            if (server.getPluginManager().getPermission(permission.getName()) != null) {
                continue;
            }
            try {
                server.getPluginManager().addPermission(permission);
                added++;
            } catch (IllegalArgumentException alreadyThere) {
                // registered by somebody between the check and the add
            }
        }
        return added;
    }
}
