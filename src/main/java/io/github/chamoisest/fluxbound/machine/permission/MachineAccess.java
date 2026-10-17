
package io.github.chamoisest.fluxbound.machine.permission;

import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import net.minecraft.server.level.ServerPlayer;

public final class MachineAccess
{
    private MachineAccess() {}

    public static boolean has(ServerPlayer player, MachineRecord machine, MachineRight right)
    {
        if (machine == null || machine.owner().isEmpty()) {
            return false;
        }

        String playerId = player.getUUID().toString();

        if (machine.owner().equals(playerId)) {
            return true;
        }

        AccessMode mode = machine.permissions().mode(right);

        return switch (mode) {
            case OWNER -> false;
            case TRUSTED -> machine.permissions().trusted().contains(playerId);
            case EVERYONE -> true;
            case TEAM -> false;
        };
    }
}
