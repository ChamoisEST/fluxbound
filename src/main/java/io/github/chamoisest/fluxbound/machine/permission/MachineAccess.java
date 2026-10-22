
package io.github.chamoisest.fluxbound.machine.permission;

import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import net.minecraft.server.level.ServerPlayer;

public final class MachineAccess
{
    private MachineAccess() {}

    public static boolean has(ServerPlayer player, MachineRecord machine, MachineRight right)
    {
        if (machine == null || machine.owner().isEmpty())
        {
            return false;
        }

        String playerId = player.getUUID().toString();

        return grants(machine.permissions().mode(right), machine.owner().equals(playerId),
                machine.permissions().trusted().contains(playerId));
    }

    // The access rule itself. Common code, so the client can warn before a change would remove the viewer's own
    // Manage right; that prediction is display only, the server decides.
    public static boolean grants(AccessMode mode, boolean owner, boolean trusted)
    {
        if (owner)
        {
            return true;
        }

        return switch (mode)
        {
            case OWNER -> false;
            case TRUSTED -> trusted;
            case EVERYONE -> true;
            case TEAM -> false;
        };
    }
}
