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

// A viewer's request to trust a player by name. Untrusted client input: the menu checks that it is still open and
// bound to the named machine; the name, Manage and the target are checked on the server (E14).
public record AddTrustedPlayerPayload(int containerId, String machineId, String name) implements CustomPacketPayload
{
    public static final Type<AddTrustedPlayerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "add_trusted_player"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddTrustedPlayerPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, AddTrustedPlayerPayload::containerId,
                    ByteBufCodecs.STRING_UTF8, AddTrustedPlayerPayload::machineId,
                    ByteBufCodecs.STRING_UTF8, AddTrustedPlayerPayload::name,
                    AddTrustedPlayerPayload::new
            );

    // Runs on the server thread (NeoForge's default for payload handlers).
    public static void handleOnServer(AddTrustedPlayerPayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof ArrivalAnchorMenu menu
                && menu.containerId == payload.containerId())
        {
            menu.requestAddTrusted(payload.machineId(), payload.name());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
