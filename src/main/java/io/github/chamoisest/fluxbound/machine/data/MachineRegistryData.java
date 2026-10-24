
package io.github.chamoisest.fluxbound.machine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import io.github.chamoisest.fluxbound.machine.energy.MachineEnergyRules;
import io.github.chamoisest.fluxbound.machine.energy.MachineEnergyStorage;
import io.github.chamoisest.fluxbound.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jspecify.annotations.Nullable;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachinePermissions;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.machine.permission.MachineRights;
import io.github.chamoisest.fluxbound.machine.permission.PermissionChange;
import io.github.chamoisest.fluxbound.machine.permission.TrustChange;
import net.minecraft.server.level.ServerPlayer;
import io.github.chamoisest.fluxbound.machine.block.ArrivalAnchorBlock;
import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.MergePlan;
import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.RemovalPlan;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class MachineRegistryData extends SavedData
{
    private static final Codec<MachineRegistryData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MachineRecord.CODEC.listOf().fieldOf("machines")
                            .forGetter(data -> List.copyOf(data.machines.values())),
                    PackedMachine.CODEC.listOf().optionalFieldOf("packed", List.of())
                            .forGetter(data -> List.copyOf(data.packedMachines.values()))
            ).apply(instance, MachineRegistryData::new));


    public static final SavedDataType<MachineRegistryData> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "machines"),
                    MachineRegistryData::new, CODEC);

    private final Map<String, MachineRecord> machines = new HashMap<>();
    private final Map<String, PackedMachine> packedMachines = new HashMap<>();
    private final Map<String, MachineEnergyStorage> energyHandlers = new HashMap<>();
    private final Map<String, Long> inputSuspensions = new HashMap<>();

    public MachineRegistryData() {}


    private MachineRegistryData(List<MachineRecord> records, List<PackedMachine> packedRecords)
    {
        for (MachineRecord record : records) {
            machines.put(record.id(), record);
        }

        for (PackedMachine packed : packedRecords) {
            packedMachines.put(packed.token(), packed);
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

    public MachineRecord findAt(String dimension, long position)
    {
        for (MachineRecord record : machines.values()) {
            if (record.contains(dimension, position)) {
                return record;
            }
        }

        return null;
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

    public void setAutoJoin(String id, boolean enabled)
    {
        MachineRecord record = machines.get(id);

        if (record != null && record.autoJoin() != enabled) {
            machines.put(id, record.withAutoJoin(enabled));
            setDirty();
        }
    }

    public EnergyHandler getEnergyHandler(String id, ServerLevel level, int capacity, int inputPerTick)
    {
        MachineRecord record = machines.get(id);

        if (record == null) {
            return null;
        }

        return energyHandlers.computeIfAbsent(id, key -> {
            MachineEnergyStorage handler = new MachineEnergyStorage(
                    capacity,
                    inputPerTick,
                    Math.toIntExact(record.energy()),
                    level::getGameTime,
                    () -> machines.containsKey(key),
                    amount -> updateEnergy(key, amount)
            );

            handler.suspendInputThrough(inputSuspensions.getOrDefault(key, Long.MIN_VALUE));
            return handler;
        });
    }

    private void updateEnergy(String id, int amount)
    {
        MachineRecord record = machines.get(id);

        if (record != null && record.energy() != amount) {
            machines.put(id, record.withEnergy(amount));
            setDirty();
        }
    }

    private void retireHandler(String id)
    {
        MachineEnergyStorage handler = energyHandlers.remove(id);

        if (handler != null) {
            handler.invalidate();
        }

        inputSuspensions.remove(id);
    }

    private void synchronizeLoadedMembers(ServerLevel level, List<MachineRecord> records)
    {
        for (MachineRecord record : records) {
            for (long packed : record.members()) {
                BlockPos pos = BlockPos.of(packed);

                if (!level.isLoaded(pos)) {
                    continue;
                }

                if (level.getBlockEntity(pos) instanceof ArrivalAnchorBlockEntity anchor) {
                    anchor.setMachineId(record.id());
                    updateVisualState(level, pos, record);
                }

                level.invalidateCapabilities(pos);
            }
        }
    }

    public void tryMergeAt(ServerLevel level, BlockPos pos)
    {
        String dimension = level.dimension().identifier().toString();
        MachineRecord placed = findAt(dimension, pos.asLong());

        if (placed == null) {
            return;
        }

        MergePlan plan = MachineRestructuring.planPlacement(placed, position -> findAt(dimension, position));

        if (plan != null) {
            applyMerge(level, plan);
            Fluxbound.LOGGER.info("Placement at {} merged {} machine(s) into {} ({} blocks)",
                    pos, plan.absorbedIds().size(), plan.result().id(), plan.result().blockCount());
        }
    }

    public boolean tryManualJoin(ServerLevel level, String firstId, String secondId)
    {
        MachineRecord first = machines.get(firstId);
        MachineRecord second = machines.get(secondId);

        if (first == null || second == null
                || !first.dimension().equals(level.dimension().identifier().toString())) {
            return false;
        }

        MergePlan plan = MachineRestructuring.planJoin(first, second);

        if (plan == null) {
            return false;
        }

        applyMerge(level, plan);
        Fluxbound.LOGGER.info("Manually joined machines {} and {} into {}", firstId, secondId, plan.result().id());
        return true;
    }

    // Registry first, then the loaded blocks (P6). Every involved machine's energy handler is retired (I7).
    private void applyMerge(ServerLevel level, MergePlan plan)
    {
        MachineRecord result = plan.result();

        retireHandler(result.id());

        for (String id : plan.absorbedIds())
        {
            retireHandler(id);
            machines.remove(id);
        }

        machines.put(result.id(), result);
        inputSuspensions.put(result.id(), level.getGameTime());

        setDirty();
        synchronizeLoadedMembers(level, List.of(result));
    }

    public PackedMachine removeMember(ServerLevel level, BlockPos pos)
    {
        String dimension = level.dimension().identifier().toString();
        MachineRecord record = findAt(dimension, pos.asLong());

        if (record == null) {
            return null;
        }

        RemovalPlan plan = planRemoval(record, pos.asLong());
        applyRemoval(level, record, plan, pos.asLong());
        return packedFor(record, plan);
    }

    private static RemovalPlan planRemoval(MachineRecord record, long position)
    {
        return MachineRestructuring.planRemoval(record, position, MachineEnergyRules::capacity,
                () -> UUID.randomUUID().toString());
    }

    private static PackedMachine packedFor(MachineRecord record, RemovalPlan plan)
    {
        return new PackedMachine(UUID.randomUUID().toString(), plan.packedId(), record.owner(),
                plan.overflowEnergy(), record.permissions());
    }

    // Registry first, then the loaded blocks (P6). The ID-keeping piece also gets a fresh energy handler, because its
    // capacity changed (I7).
    private void applyRemoval(ServerLevel level, MachineRecord record, RemovalPlan plan, long removedPosition)
    {
        retireHandler(record.id());
        machines.remove(record.id());

        List<MachineRecord> replacements = new ArrayList<>();

        for (MachineRestructuring.Piece piece : plan.pieces())
        {
            MachineRecord replacement = new MachineRecord(piece.id(), record.dimension(), piece.members(),
                    record.owner(), piece.energy(), record.autoJoin(), record.permissions());

            machines.put(piece.id(), replacement);
            inputSuspensions.put(piece.id(), level.getGameTime());
            replacements.add(replacement);
        }

        setDirty();
        synchronizeLoadedMembers(level, replacements);
        level.invalidateCapabilities(BlockPos.of(removedPosition));

        Fluxbound.LOGGER.info("Removed a block from machine {}: {} piece(s) remain, {} FE left the placed machines",
                record.id(), replacements.size(), plan.overflowEnergy());
    }

    public void registerPacked(PackedMachine packed)
    {
        if (packedMachines.containsKey(packed.token())) {
            throw new IllegalStateException("Duplicate packed machine token: " + packed.token());
        }

        packedMachines.put(packed.token(), packed);
        setDirty();
    }

    // A reset by crafting retires a packed identity for good (M2-E3). Returns the removed record, or null if the token
    // was unknown or already redeemed. From M4, links and network membership are revoked here as well.
    public @Nullable PackedMachine retirePacked(String token)
    {
        PackedMachine packed = packedMachines.remove(token);

        if (packed != null)
        {
            setDirty();
        }

        return packed;
    }

    // Whether a packed token can be redeemed: it exists and its identity isn't placed already (I3). Read-only; the
    // item asks before placing, restorePacked redeems afterwards.
    public boolean canRestorePacked(String token)
    {
        PackedMachine packed = packedMachines.get(token);
        return packed != null && !machines.containsKey(packed.machineId());
    }

    public boolean restorePacked(String token, ServerLevel level, BlockPos pos, ArrivalAnchorBlockEntity anchor)
    {
        PackedMachine packed = packedMachines.get(token);

        if (packed == null || machines.containsKey(packed.machineId())) {
            return false;
        }

        String dimension = level.dimension().identifier().toString();
        MachineRecord temporary = findAt(dimension, pos.asLong());

        if (temporary == null || temporary.blockCount() != 1 || temporary.energy() != 0) {
            return false;
        }

        retireHandler(temporary.id());
        machines.remove(temporary.id());

        long restoredEnergy = Math.min(packed.energy(), MachineEnergyRules.capacity(1));

        MachineRecord restored = new MachineRecord(
                packed.machineId(),
                dimension,
                pos.asLong(),
                packed.owner(),
                restoredEnergy
        ).withPermissions(packed.permissions());

        machines.put(restored.id(), restored);
        packedMachines.remove(token);

        anchor.setMachineId(restored.id());
        level.invalidateCapabilities(pos);
        setDirty();

        Fluxbound.LOGGER.info("Restored packed Arrival Anchor {} with {} FE",
                restored.id(), restored.energy());

        return true;
    }

    // Same calculation as removeMember, without changing anything. The explosion path attaches the result to its
    // drop before vanilla removes the block in the same onExplosionHit call (source-verified 26.1.2), so both agree
    // (M2 G3-3).
    public PackedMachine previewPackedForRemoval(ServerLevel level, BlockPos pos)
    {
        String dimension = level.dimension().identifier().toString();
        MachineRecord record = findAt(dimension, pos.asLong());

        return record == null ? null : packedFor(record, planRemoval(record, pos.asLong()));
    }


    public PermissionChange setAccessMode(ServerPlayer player, String id, MachineRight right, AccessMode mode)
    {
        MachineRecord record = machines.get(id);

        if (!MachineAccess.has(player, record, MachineRight.MANAGE))
        {
            return PermissionChange.DENIED;
        }

        if (!MachineRights.of(record).contains(right))
        {
            return PermissionChange.RIGHT_NOT_USED;
        }

        if (!mode.selectable())
        {
            return PermissionChange.MODE_UNAVAILABLE;
        }

        MachinePermissions updated = record.permissions().withMode(right, mode);

        if (!updated.equals(record.permissions()))
        {
            machines.put(id, record.withPermissions(updated));
            setDirty();
        }

        return PermissionChange.APPLIED;
    }

    public TrustChange addTrusted(ServerPlayer player, String id, UUID target)
    {
        MachineRecord record = machines.get(id);

        if (!MachineAccess.has(player, record, MachineRight.MANAGE))
        {
            return TrustChange.DENIED;
        }

        String targetId = target.toString();

        if (record.owner().equals(targetId))
        {
            return TrustChange.IS_OWNER;
        }

        if (record.permissions().trusted().contains(targetId))
        {
            return TrustChange.ALREADY_TRUSTED;
        }

        machines.put(id, record.withPermissions(record.permissions().withTrusted(targetId, true)));
        setDirty();
        return TrustChange.ADDED;
    }

    // Takes the entry as stored, so entries that aren't valid UUIDs can be removed too.
    public TrustChange removeTrusted(ServerPlayer player, String id, String storedId)
    {
        MachineRecord record = machines.get(id);

        if (!MachineAccess.has(player, record, MachineRight.MANAGE))
        {
            return TrustChange.DENIED;
        }

        if (!record.permissions().trusted().contains(storedId))
        {
            return TrustChange.NOT_TRUSTED;
        }

        machines.put(id, record.withPermissions(record.permissions().withTrusted(storedId, false)));
        setDirty();
        return TrustChange.REMOVED;
    }

    private void updateVisualState(ServerLevel level, BlockPos pos, MachineRecord record)
    {
        BlockState current = level.getBlockState(pos);

        if (!current.is(ModBlocks.ARRIVAL_ANCHOR.get())) {
            return;
        }

        String dimension = level.dimension().identifier().toString();

        BlockState updated = current
                .setValue(ArrivalAnchorBlock.NORTH_CONNECTED, record.contains(dimension, pos.north().asLong()))
                .setValue(ArrivalAnchorBlock.EAST_CONNECTED, record.contains(dimension, pos.east().asLong()))
                .setValue(ArrivalAnchorBlock.SOUTH_CONNECTED, record.contains(dimension, pos.south().asLong()))
                .setValue(ArrivalAnchorBlock.WEST_CONNECTED, record.contains(dimension, pos.west().asLong()));

        if (!updated.equals(current)) {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        }
    }

    public void refreshVisualAt(ServerLevel level, BlockPos pos)
    {
        String dimension = level.dimension().identifier().toString();
        MachineRecord record = findAt(dimension, pos.asLong());

        if (record != null) {
            updateVisualState(level, pos, record);
        }
    }



}
