
package io.github.chamoisest.fluxbound.integration.jade;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

public final class MachineEnergyJadeProvider implements StreamServerDataProvider<BlockAccessor, MachineEnergyJadeProvider.EnergyInfo>
{
    public static final MachineEnergyJadeProvider INSTANCE = new MachineEnergyJadeProvider();
    public static final Identifier UID = Identifier.fromNamespaceAndPath(Fluxbound.MODID, "machine_energy");

    public record EnergyInfo(int stored, int capacity) {}

    private static final StreamCodec<RegistryFriendlyByteBuf, EnergyInfo> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnergyInfo::stored,
            ByteBufCodecs.VAR_INT, EnergyInfo::capacity,
            EnergyInfo::new
    );

    private MachineEnergyJadeProvider() {}

    @Override
    public EnergyInfo streamData(BlockAccessor accessor)
    {
        if (!(accessor.getBlockEntity() instanceof ArrivalAnchorBlockEntity anchor)
                || !(anchor.getLevel() instanceof ServerLevel level)
                || anchor.getMachineId() == null) {
            return null;
        }

        MachineRecord record = MachineRegistryData.get(level).find(anchor.getMachineId());

        if (record == null) {
            return null;
        }

        return new EnergyInfo(Math.toIntExact(record.energy()), ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get());
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, EnergyInfo> streamCodec()
    {
        return CODEC;
    }

    @Override
    public Identifier getUid()
    {
        return UID;
    }
}
