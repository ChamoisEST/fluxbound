
package io.github.chamoisest.fluxbound.machine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import io.github.chamoisest.fluxbound.machine.energy.MachineEnergyStorage;
import io.github.chamoisest.fluxbound.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachinePermissions;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.machine.permission.MachineRights;
import io.github.chamoisest.fluxbound.machine.permission.PermissionChange;
import io.github.chamoisest.fluxbound.machine.permission.TrustChange;
import net.minecraft.server.level.ServerPlayer;
import io.github.chamoisest.fluxbound.machine.block.ArrivalAnchorBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class MachineRegistryData extends SavedData
{
    private static final int MAX_SIDE = 8;


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
        MachineRecord record = findAt(dimension, pos.asLong());

        if (record != null && record.autoJoin() && !record.owner().isEmpty()) {
            rebuildRegion(level, record, null, 0);
        }
    }

    public boolean tryManualJoin(ServerLevel level, String firstId, String secondId)
    {
        MachineRecord first = machines.get(firstId);
        MachineRecord second = machines.get(secondId);

        if (first == null || second == null || first.id().equals(second.id())
                || !first.dimension().equals(second.dimension())
                || !first.dimension().equals(level.dimension().identifier().toString())
                || first.owner().isEmpty() || !first.owner().equals(second.owner())
                || !first.permissions().sameFor(MachineRights.of(first), second.permissions())) {
            return false;
        }

        if (!areAdjacent(first, second)) {
            return false;
        }

        Set<Long> combined = new HashSet<>(first.members());
        combined.addAll(second.members());

        if (combined.size() != first.blockCount() + second.blockCount()
                || !MultiblockLayout.isRectangle(combined, MAX_SIDE)) {
            return false;
        }

        MachineRecord primary = first.blockCount() >= second.blockCount() ? first : second;
        long combinedEnergy = first.energy() + second.energy();

        retireHandler(first.id());
        retireHandler(second.id());

        machines.remove(first.id());
        machines.remove(second.id());

        MachineRecord merged = primary.withMembers(combined.stream().sorted().toList())
                .withEnergy(combinedEnergy)
                .withAutoJoin(true);

        machines.put(merged.id(), merged);
        inputSuspensions.put(merged.id(), level.getGameTime());

        setDirty();
        synchronizeLoadedMembers(level, List.of(merged));

        Fluxbound.LOGGER.info("Manually joined machines {} and {} into {}",
                firstId, secondId, merged.id());

        return true;
    }

    private boolean areAdjacent(MachineRecord first, MachineRecord second)
    {
        for (long packed : first.members()) {
            BlockPos pos = BlockPos.of(packed);

            for (Direction direction : new Direction[]{
                    Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                if (second.contains(first.dimension(), pos.relative(direction).asLong())) {
                    return true;
                }
            }
        }

        return false;
    }

    public PackedMachine removeMember(ServerLevel level, BlockPos pos)
    {
        PackedMachine packed = previewPackedForRemoval(level, pos);

        if (packed == null) {
            return null;
        }

        String dimension = level.dimension().identifier().toString();
        MachineRecord record = findAt(dimension, pos.asLong());

        rebuildRegion(level, record, pos.asLong(), packed.energy());
        return packed;
    }

    private Map<String, MachineRecord> collectRegion(ServerLevel level, MachineRecord seed, Long removedPosition)
    {
        Map<String, MachineRecord> region = new LinkedHashMap<>();
        ArrayDeque<MachineRecord> pending = new ArrayDeque<>();

        region.put(seed.id(), seed);
        pending.add(seed);

        if (!seed.autoJoin() || seed.owner().isEmpty()) {
            return region;
        }

        String dimension = seed.dimension();

        while (!pending.isEmpty()) {
            MachineRecord current = pending.removeFirst();

            for (long packed : current.members()) {
                if (removedPosition != null && packed == removedPosition.longValue()) {
                    continue;
                }

                BlockPos pos = BlockPos.of(packed);

                for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    BlockPos neighbor = pos.relative(direction);

                    if (removedPosition != null && neighbor.asLong() == removedPosition.longValue()) {
                        continue;
                    }

                    if (level.isLoaded(neighbor)
                            && !level.getBlockState(neighbor).is(ModBlocks.ARRIVAL_ANCHOR.get())) {
                        continue;
                    }

                    MachineRecord nearby = findAt(dimension, neighbor.asLong());

                    if (nearby == null || region.containsKey(nearby.id())
                            || !nearby.autoJoin() || !seed.owner().equals(nearby.owner())
                            || !seed.permissions().sameFor(MachineRights.of(seed), nearby.permissions())) {
                        continue;
                    }

                    region.put(nearby.id(), nearby);
                    pending.addLast(nearby);
                }
            }
        }

        return region;
    }

    private boolean layoutUnchanged(Map<String, MachineRecord> region, List<List<Long>> parts)
    {
        if (region.size() != parts.size()) {
            return false;
        }

        for (List<Long> part : parts) {
            boolean found = region.values().stream().anyMatch(record ->
                    record.blockCount() == part.size() && record.members().containsAll(part));

            if (!found) {
                return false;
            }
        }

        return true;
    }

    private void rebuildRegion(ServerLevel level, MachineRecord seed, Long removedPosition, long reservedEnergy)
    {
        Map<String, MachineRecord> region = collectRegion(level, seed, removedPosition);

        Set<Long> survivingMembers = new HashSet<>();
        long totalEnergy = 0;

        for (MachineRecord record : region.values()) {
            survivingMembers.addAll(record.members());
            totalEnergy += record.energy();
        }

        if (removedPosition != null) {
            survivingMembers.remove(removedPosition);
        }

        List<List<Long>> parts = MultiblockLayout.partition(survivingMembers, MAX_SIDE);

        if (removedPosition == null && layoutUnchanged(region, parts)) {
            return;
        }

        long capacityPerBlock = ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get();
        long totalCapacity = capacityPerBlock * survivingMembers.size();
        long availableEnergy = totalEnergy - reservedEnergy;
        long retainedEnergy = Math.min(availableEnergy, totalCapacity);
        long voidedEnergy = availableEnergy - retainedEnergy;

        List<MachineRecord> previousRecords = new ArrayList<>(region.values());
        previousRecords.sort(Comparator.comparing(MachineRecord::id));

        for (MachineRecord record : previousRecords) {
            retireHandler(record.id());
            machines.remove(record.id());
        }

        Set<String> reusedIds = new HashSet<>();
        List<MachineRecord> replacements = new ArrayList<>();

        long energyRemaining = retainedEnergy;
        int blocksRemaining = survivingMembers.size();

        for (int i = 0; i < parts.size(); i++) {
            List<Long> members = parts.get(i);

            MachineRecord bestMatch = null;
            int bestOverlap = 0;

            for (MachineRecord previous : previousRecords) {
                if (reusedIds.contains(previous.id())) {
                    continue;
                }

                int overlap = 0;

                for (long packed : members) {
                    if (previous.members().contains(packed)) {
                        overlap++;
                    }
                }

                if (overlap > bestOverlap) {
                    bestMatch = previous;
                    bestOverlap = overlap;
                }
            }

            String id = bestMatch != null ? bestMatch.id() : UUID.randomUUID().toString();
            reusedIds.add(id);

            long assignedEnergy = i == parts.size() - 1
                    ? energyRemaining
                    : energyRemaining * members.size() / blocksRemaining;

            MachineRecord replacement = new MachineRecord(
                    id,
                    seed.dimension(),
                    members,
                    seed.owner(),
                    assignedEnergy,
                    seed.autoJoin(),
                    seed.permissions()
            );

            machines.put(id, replacement);
            inputSuspensions.put(id, level.getGameTime());
            replacements.add(replacement);

            energyRemaining -= assignedEnergy;
            blocksRemaining -= members.size();
        }

        setDirty();
        synchronizeLoadedMembers(level, replacements);

        if (removedPosition != null) {
            level.invalidateCapabilities(BlockPos.of(removedPosition));
        }

        if (voidedEnergy > 0) {
            Fluxbound.LOGGER.info("Voided {} excess FE during multiblock rebuild", voidedEnergy);
        }

        Fluxbound.LOGGER.info("Rebuilt {} Arrival Anchor blocks into {} machine(s)",
                survivingMembers.size(), replacements.size());
    }

    private void reconnectAfterSplit(ServerLevel level, Set<Long> remaining)
    {
        List<Long> positions = remaining.stream().sorted().toList();

        boolean changed;

        do {
            int previousMachineCount = machines.size();

            for (long packed : positions) {
                BlockPos pos = BlockPos.of(packed);

                if (level.isLoaded(pos)) {
                    tryMergeAt(level, pos);
                }
            }

            changed = machines.size() < previousMachineCount;
        } while (changed);
    }

    public void registerPacked(PackedMachine packed)
    {
        if (packedMachines.containsKey(packed.token())) {
            throw new IllegalStateException("Duplicate packed machine token: " + packed.token());
        }

        packedMachines.put(packed.token(), packed);
        setDirty();
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

        long capacity = ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get();
        long restoredEnergy = Math.min(packed.energy(), capacity);

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

    public PackedMachine previewPackedForRemoval(ServerLevel level, BlockPos pos)
    {
        String dimension = level.dimension().identifier().toString();
        MachineRecord record = findAt(dimension, pos.asLong());

        if (record == null) {
            return null;
        }

        Map<String, MachineRecord> region = collectRegion(level, record, pos.asLong());

        long totalEnergy = 0;
        int remainingBlocks = -1;

        for (MachineRecord machine : region.values()) {
            totalEnergy += machine.energy();
            remainingBlocks += machine.blockCount();
        }

        long remainingCapacity = (long) ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get() * remainingBlocks;
        long packedEnergy = Math.max(0, totalEnergy - remainingCapacity);

        String packedId = record.blockCount() == 1
                ? record.id()
                : UUID.randomUUID().toString();

        return new PackedMachine(
                UUID.randomUUID().toString(),
                packedId,
                record.owner(),
                packedEnergy,
                true,
                record.permissions()
        );
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
