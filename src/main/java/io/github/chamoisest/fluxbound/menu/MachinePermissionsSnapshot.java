package io.github.chamoisest.fluxbound.menu;

import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachinePermissions;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload.Content;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload.TrustedPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Builds the Permissions sync section for one viewer. Shared by all machine menus, so every machine applies
// the same Manage rule, name lookup and order.
public final class MachinePermissionsSnapshot
{
    // Known names first, alphabetically ignoring case, unknown entries last. The stored ID breaks ties, so the
    // order stays fixed even when two entries share a cached name after a rename.
    private static final Comparator<TrustedPlayer> ORDER = Comparator
            .comparing((TrustedPlayer player) -> player.name().isEmpty())
            .thenComparing(player -> player.name().orElse(""), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(TrustedPlayer::id);

    private MachinePermissionsSnapshot() {}

    // Checked on every snapshot, so gaining or losing Manage while the menu is open takes effect on the next tick.
    public static MachinePermissionsPayload create(int containerId, MachineRecord record, ServerPlayer viewer)
    {
        if (!MachineAccess.has(viewer, record, MachineRight.MANAGE))
        {
            return new MachinePermissionsPayload(containerId, record.id(), Optional.empty());
        }

        MachinePermissions permissions = record.permissions();
        MinecraftServer server = viewer.level().getServer();
        List<TrustedPlayer> trusted = permissions.trusted().stream()
                .map(id -> new TrustedPlayer(id, PlayerNames.find(server, id)))
                .sorted(ORDER)
                .toList();

        boolean viewerIsOwner = record.owner().equals(viewer.getUUID().toString());

        return new MachinePermissionsPayload(containerId, record.id(), Optional.of(new Content(viewerIsOwner,
                permissions.use(), permissions.arrive(), permissions.manage(), permissions.charge(), trusted)));
    }
}
