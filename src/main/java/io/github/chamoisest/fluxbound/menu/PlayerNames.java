package io.github.chamoisest.fluxbound.menu;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;

import java.util.Optional;
import java.util.UUID;

// Display names for stored player IDs, read from the server's name cache (usercache.json). A lookup by UUID
// only reads memory: it never contacts Mojang and never blocks, so menus can call it every tick.
public final class PlayerNames
{
    private PlayerNames() {}

    // Empty if the ID is not a UUID or the server has no name for it; callers decide how to show that.
    public static Optional<String> find(MinecraftServer server, String storedId)
    {
        UUID id;

        try
        {
            id = UUID.fromString(storedId);
        }
        catch (IllegalArgumentException exception)
        {
            return Optional.empty();
        }

        return server.services().nameToIdCache().get(id).map(NameAndId::name);
    }
}
