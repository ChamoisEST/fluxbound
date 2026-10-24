
package io.github.chamoisest.fluxbound.machine.blockentity;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.machine.energy.MachineEnergyRules;
import io.github.chamoisest.fluxbound.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import io.github.chamoisest.fluxbound.machine.data.PackedMachine;

import java.util.UUID;

public class ArrivalAnchorBlockEntity extends BlockEntity
{
    private String machineId;
    private PackedMachine pendingPickup;

    // Not saved: the orphan warning is logged once per load of this block.
    private boolean orphanReported;

    public ArrivalAnchorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.ARRIVAL_ANCHOR.get(), pos, state);
    }

    public String getMachineId()
    {
        return machineId;
    }

    public void setMachineId(String id)
    {
        if (!id.equals(machineId)) {
            machineId = id;
            setChanged();
        }
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        initializeMachine();

        if (level instanceof ServerLevel serverLevel) {
            MachineRegistryData.get(serverLevel).refreshVisualAt(serverLevel, worldPosition);
        }
    }

    public void initializeMachine()
    {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        MachineRegistryData registry = MachineRegistryData.get(serverLevel);
        String dimension = serverLevel.dimension().identifier().toString();

        MachineRecord atPosition = registry.findAt(dimension, worldPosition.asLong());

        if (atPosition != null) {
            setMachineId(atPosition.id());

            Fluxbound.LOGGER.info("Loaded Arrival Anchor {} at {} ({} members, {} FE)",
                    atPosition.id(), worldPosition, atPosition.blockCount(), atPosition.energy());
            return;
        }

        // A fresh block has no stored ID yet and becomes a new machine.
        if (machineId == null) {
            machineId = UUID.randomUUID().toString();
            setChanged();
            registry.ensureRegistered(machineId, dimension, worldPosition.asLong());

            Fluxbound.LOGGER.info("Registered Arrival Anchor {} at {}", machineId, worldPosition);
            return;
        }

        // A stored ID without a machine containing this block: lost save data or a copied block (/clone, structures).
        // It stays inert instead of silently becoming a new, ownerless machine (M2 G2-2, spec §4, P5).
        if (!orphanReported) {
            orphanReported = true;
            Fluxbound.LOGGER.warn("Arrival Anchor at {} in {} refers to machine {}, but no machine record contains "
                    + "this position (lost save data or a copied block). It stays inert until it is broken.",
                    worldPosition, dimension, machineId);
        }
    }

    public EnergyHandler getEnergyHandler()
    {
        if (!(level instanceof ServerLevel serverLevel) || machineId == null) {
            return null;
        }

        MachineRecord record = MachineRegistryData.get(serverLevel).find(machineId);
        String dimension = serverLevel.dimension().identifier().toString();

        if (record == null || !record.contains(dimension, worldPosition.asLong())) {
            return null;
        }

        return MachineRegistryData.get(serverLevel).getEnergyHandler(
                machineId,
                serverLevel,
                Math.toIntExact(MachineEnergyRules.capacity(record.blockCount())),
                Math.toIntExact(MachineEnergyRules.inputPerTick(record.blockCount()))
        );
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
        if (level instanceof ServerLevel serverLevel) {
            pendingPickup = MachineRegistryData.get(serverLevel).removeMember(serverLevel, pos);
        }

        super.preRemoveSideEffects(pos, state);
    }

    public PackedMachine takePendingPickup()
    {
        PackedMachine result = pendingPickup;
        pendingPickup = null;
        return result;
    }

}
