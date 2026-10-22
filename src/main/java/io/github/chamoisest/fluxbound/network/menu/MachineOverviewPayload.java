package io.github.chamoisest.fluxbound.network.menu;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.menu.arrivalanchor.ArrivalAnchorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Public machine information shown on the Overview tab. The container ID ties it to one opened menu,
// because IDs are reused and a late payload must not reach a different menu.
public record MachineOverviewPayload(int containerId, String machineId, long energy, long capacity,
                                     long inputPerTick, int memberCount, String ownerName,
                                     String dimension, BlockPos center)
        implements CustomPacketPayload
{
    public static final Type<MachineOverviewPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "machine_overview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineOverviewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MachineOverviewPayload::containerId,
                    ByteBufCodecs.STRING_UTF8, MachineOverviewPayload::machineId,
                    ByteBufCodecs.VAR_LONG, MachineOverviewPayload::energy,
                    ByteBufCodecs.VAR_LONG, MachineOverviewPayload::capacity,
                    ByteBufCodecs.VAR_LONG, MachineOverviewPayload::inputPerTick,
                    ByteBufCodecs.VAR_INT, MachineOverviewPayload::memberCount,
                    ByteBufCodecs.STRING_UTF8, MachineOverviewPayload::ownerName,
                    ByteBufCodecs.STRING_UTF8, MachineOverviewPayload::dimension,
                    BlockPos.STREAM_CODEC, MachineOverviewPayload::center,
                    MachineOverviewPayload::new
            );

    // Uses only common classes, so it can be registered in common code.
    public static void handleOnClient(MachineOverviewPayload payload, IPayloadContext context)
    {
        if (context.player().containerMenu instanceof ArrivalAnchorMenu menu
                && menu.containerId == payload.containerId())
        {
            menu.setOverview(payload);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
