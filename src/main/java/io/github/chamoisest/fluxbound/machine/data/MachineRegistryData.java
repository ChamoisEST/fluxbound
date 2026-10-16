
package io.github.chamoisest.fluxbound.machine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import io.github.chamoisest.fluxbound.machine.energy.MachineEnergyStorage;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineRegistryData extends SavedData
{
    private static final Codec<MachineRegistryData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MachineRecord.CODEC.listOf().fieldOf("machines")
                            .forGetter(data -> List.copyOf(data.machines.values()))
            ).apply(instance, MachineRegistryData::new));

    public static final SavedDataType<MachineRegistryData> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "machines"),
                    MachineRegistryData::new, CODEC);

    private final Map<String, MachineRecord> machines = new HashMap<>();
    private final Map<String, MachineEnergyStorage> energyHandlers = new HashMap<>();

    public MachineRegistryData() {}

    private MachineRegistryData(List<MachineRecord> records)
    {
        for (MachineRecord record : records) {
            machines.put(record.id(), record);
        }
    }

    public static MachineRegistryData get(ServerLevel level)
    {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public MachineRecord find(String id)
    {
        return machines.get(id);
    }

    public boolean ensureRegistered(String id, String dimension, long position)
    {
        if (machines.containsKey(id)) {
            return false;
        }

        machines.put(id, new MachineRecord(id, dimension, position, "", 0));
        setDirty();
        return true;
    }

    public void assignOwner(String id, String owner)
    {
        MachineRecord record = machines.get(id);

        if (record != null && record.owner().isEmpty()) {
            machines.put(id, record.withOwner(owner));
            setDirty();
        }
    }

    public void remove(String id, String dimension, long position)
    {
        MachineRecord record = machines.get(id);

        if (record != null && record.dimension().equals(dimension) && record.position() == position) {
            machines.remove(id);
            energyHandlers.remove(id);
            setDirty();
        }
    }


    public EnergyHandler getEnergyHandler(String id, ServerLevel level, int capacity, int inputPerTick)
    {
        MachineRecord record = machines.get(id);

        if (record == null) {
            return null;
        }

        return energyHandlers.computeIfAbsent(id, key -> new MachineEnergyStorage(
                capacity,
                inputPerTick,
                Math.toIntExact(record.energy()),
                level::getGameTime,
                () -> machines.containsKey(id),
                amount -> updateEnergy(id, amount)
        ));
    }

    private void updateEnergy(String id, int amount)
    {
        MachineRecord record = machines.get(id);

        if (record != null && record.energy() != amount) {
            machines.put(id, record.withEnergy(amount));
            setDirty();
        }
    }

}
