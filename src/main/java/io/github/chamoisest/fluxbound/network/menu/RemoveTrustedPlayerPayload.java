package io.github.chamoisest.fluxbound.network.menu;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.menu.arrivalanchor.ArrivalAnchorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// A viewer's request to remove a trusted entry, named as stored (normally a UUID). Untrusted client input: the
// menu checks that it is still open and bound to the named machine, and the registry checks Manage (E14).
public record RemoveTrustedPlayerPayload(int containerId, String machineId, String storedId)
        implements CustomPacketPayload
{
    public static final Type<RemoveTrustedPlayerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "remove_trusted_player"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoveTrustedPlayerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RemoveTrustedPlayerPayload::containerId,
                    ByteBufCodecs.STRING_UTF8, RemoveTrustedPlayerPayload::machineId,
                    ByteBufCodecs.STRING_UTF8, RemoveTrustedPlayerPayload::storedId,
                    RemoveTrustedPlayerPayload::new
            );

    // Runs on the server thread (NeoForge's default for payload handlers).
    public static void handleOnServer(RemoveTrustedPlayerPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof ArrivalAnchorMenu menu
                && menu.containerId == payload.containerId())
        {
            menu.requestRemoveTrusted(payload.machineId(), payload.storedId());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
