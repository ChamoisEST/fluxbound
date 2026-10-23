package io.github.chamoisest.fluxbound.machine.permission;

import com.mojang.logging.LogUtils;
import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

// Adds trusted players by name for the GUI and commands. A name that isn't online goes to vanilla's name cache
// on a background thread, because a cache miss asks Mojang and blocks (M1 E4); vanilla resolves profiles on the
// same pool. Results always come back on the server thread.
@EventBusSubscriber(modid = Fluxbound.MODID)
public final class TrustRequests
{
    private static final Logger LOGGER = LogUtils.getLogger();

    // Per player, so one player can't use up the server's Mojang lookup allowance for everyone (M1 B6a-1).
    private static final long LOOKUP_COOLDOWN_MS = 3000;

    // Only touched on the server thread. The token identifies the lookup that is in flight.
    private static final Map<UUID, Object> PENDING = new HashMap<>();
    private static final Map<UUID, Long> LAST_LOOKUP = new HashMap<>();

    private TrustRequests() {}

    // How a caller reports a result. The requester is passed again because a lookup can outlive the original
    // player object (relogging creates a new one).
    @FunctionalInterface
    public interface Reply
    {
        void send(ServerPlayer requester, TrustChange result, String name);
    }

    // Online players resolve at once. After a background lookup nothing happens unless the requester is still
    // online and stillWanted accepts them (M1 B6-4); the registry then checks Manage again (E14).
    public static void add(ServerPlayer requester, String machineId, String name,
                           Predicate<ServerPlayer> stillWanted, Reply reply)
    {
        // isValidPlayerName accepts the empty string.
        if (name.isEmpty() || !StringUtil.isValidPlayerName(name))
        {
            reply.send(requester, TrustChange.INVALID_NAME, name);
            return;
        }

        // Also checked before the lookup, so players without Manage can't make the server query Mojang.
        MachineRecord machine = MachineRegistryData.get(requester.level()).find(machineId);

        if (!MachineAccess.has(requester, machine, MachineRight.MANAGE))
        {
            reply.send(requester, TrustChange.DENIED, name);
            return;
        }

        MinecraftServer server = requester.level().getServer();
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);

        if (online != null)
        {
            apply(requester, machineId, online.nameAndId(), reply);
            return;
        }

        UUID requesterId = requester.getUUID();
        long now = System.currentTimeMillis();
        Long last = LAST_LOOKUP.get(requesterId);

        if (PENDING.containsKey(requesterId) || (last != null && now - last < LOOKUP_COOLDOWN_MS))
        {
            reply.send(requester, TrustChange.LOOKUP_BUSY, name);
            return;
        }

        Object token = new Object();
        PENDING.put(requesterId, token);
        LAST_LOOKUP.put(requesterId, now);

        CompletableFuture.supplyAsync(() -> server.services().nameToIdCache().get(name), Util.nonCriticalIoPool())
                .whenCompleteAsync((found, error) ->
                {
                    // False if the server stopped in between (singleplayer can start a new world session).
                    if (!PENDING.remove(requesterId, token))
                    {
                        return;
                    }

                    ServerPlayer current = server.getPlayerList().getPlayer(requesterId);

                    if (current == null || !stillWanted.test(current))
                    {
                        return;
                    }

                    if (error != null)
                    {
                        LOGGER.warn("Looking up player name '{}' failed", name, error);
                        reply.send(current, TrustChange.LOOKUP_FAILED, name);
                        return;
                    }

                    if (found.isEmpty())
                    {
                        reply.send(current, TrustChange.NOT_FOUND, name);
                        return;
                    }

                    apply(current, machineId, found.get(), reply);
                }, server);
    }

    // The stored name, so the reply uses the player's real capitalisation rather than what was typed.
    private static void apply(ServerPlayer requester, String machineId, NameAndId player, Reply reply)
    {
        TrustChange result = MachineRegistryData.get(requester.level()).addTrusted(requester, machineId, player.id());
        reply.send(requester, result, player.name());
    }

    // Lookups of a stopped server never call back, so their entries would otherwise block the player forever
    // in later singleplayer sessions.
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        PENDING.clear();
        LAST_LOOKUP.clear();
    }
}
