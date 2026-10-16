package io.github.chamoisest.fluxbound.machine.blockentity;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.UUID;

public class ArrivalAnchorBlockEntity extends BlockEntity
{
    private String machineId;

    public ArrivalAnchorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.ARRIVAL_ANCHOR.get(), pos, state);
    }

    public String getMachineId()
    {
        return machineId;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        initializeMachine();
    }

    public void initializeMachine()
    {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        MachineRegistryData registry = MachineRegistryData.get(serverLevel);
        String dimension = serverLevel.dimension().identifier().toString();

        MachineRecord existing = machineId == null ? null : registry.find(machineId);

        if (existing != null &&
                (!existing.dimension().equals(dimension) || existing.position() != worldPosition.asLong())) {
            Fluxbound.LOGGER.warn("Duplicate machine identity detected at {}; assigning a new UUID.", worldPosition);
            machineId = null;
        }

        if (machineId == null) {
            machineId = UUID.randomUUID().toString();
            setChanged();
        }

        boolean created = registry.ensureRegistered(machineId, dimension, worldPosition.asLong());

        Fluxbound.LOGGER.info("{} Arrival Anchor {} at {}",
                created ? "Registered" : "Loaded", machineId, worldPosition);
    }

    @Override
    protected void loadAdditional(ValueInput input)
    {
        super.loadAdditional(input);
        machineId = input.getString("MachineId").orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output)
    {
        super.saveAdditional(output);

        if (machineId != null) {
            output.putString("MachineId", machineId);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state)
    {
        if (level instanceof ServerLevel serverLevel && machineId != null) {
            MachineRegistryData.get(serverLevel).remove(
                    machineId, serverLevel.dimension().identifier().toString(), pos.asLong());
        }

        super.preRemoveSideEffects(pos, state);
    }


    public EnergyHandler getEnergyHandler()
    {
        if (!(level instanceof ServerLevel serverLevel) || machineId == null) {
            return null;
        }

        MachineRegistryData registry = MachineRegistryData.get(serverLevel);
        MachineRecord record = registry.find(machineId);

        if (record == null ||
                !record.dimension().equals(serverLevel.dimension().identifier().toString()) ||
                record.position() != worldPosition.asLong()) {
            return null;
        }

        return registry.getEnergyHandler(
                machineId,
                serverLevel,
                ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get(),
                ModConfigs.ARRIVAL_ANCHOR_INPUT.get()
        );
    }

}
