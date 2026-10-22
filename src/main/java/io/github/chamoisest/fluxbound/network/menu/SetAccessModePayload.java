package io.github.chamoisest.fluxbound.network.menu;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.menu.arrivalanchor.ArrivalAnchorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// A viewer's request to change one access mode. Untrusted client input: the menu checks that it is still open
// and bound to the named machine, and the registry checks Manage, the right and the mode (E14).
public record SetAccessModePayload(int containerId, String machineId, MachineRight right, AccessMode mode)
        implements CustomPacketPayload
{
    public static final Type<SetAccessModePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "set_access_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetAccessModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SetAccessModePayload::containerId,
                    ByteBufCodecs.STRING_UTF8, SetAccessModePayload::machineId,
                    NeoForgeStreamCodecs.enumCodec(MachineRight.class), SetAccessModePayload::right,
                    NeoForgeStreamCodecs.enumCodec(AccessMode.class), SetAccessModePayload::mode,
                    SetAccessModePayload::new
            );

    // Runs on the server thread (NeoForge's default for payload handlers).
    public static void handleOnServer(SetAccessModePayload payload, IPayloadContext context)
    {
        if (context.player() instanceof ServerPlayer player
                && player.containerMenu instanceof ArrivalAnchorMenu menu
                && menu.containerId == payload.containerId())
        {
            menu.requestAccessMode(payload.machineId(), payload.right(), payload.mode());
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
