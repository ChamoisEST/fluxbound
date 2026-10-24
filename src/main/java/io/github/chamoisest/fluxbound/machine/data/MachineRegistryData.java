
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
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
import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.ChargingTransfer;
import io.github.chamoisest.fluxbound.machine.data.MachineRestructuring.MergePlan;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
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
                            .forGetter(data -> List.copyOf(data.packedMachines.values())),
                    ChargingEntry.CODEC.listOf().optionalFieldOf("charging_items", List.of())
                            .forGetter(MachineRegistryData::chargingEntries)
            ).apply(instance, MachineRegistryData::new));

    // One entry of the saved charging items. The item is read leniently: an entry that can't be decoded (for example
    // a removed mod's item) is skipped with an error instead of failing the whole registry (M3-10 a, P5).
    private record ChargingEntry(String machineId, Optional<ItemStack> item)
    {
        static final Codec<ChargingEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("machine_id").forGetter(ChargingEntry::machineId),
                        ItemStack.CODEC.lenientOptionalFieldOf("item").forGetter(ChargingEntry::item)
                ).apply(instance, ChargingEntry::new));
    }


    public static final SavedDataType<MachineRegistryData> TYPE =
            new SavedDataType<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "machines"),
                    MachineRegistryData::new, CODEC);

    private final Map<String, MachineRecord> machines = new HashMap<>();
    private final Map<String, PackedMachine> packedMachines = new HashMap<>();
    private final Map<String, MachineEnergyStorage> energyHandlers = new HashMap<>();
    private final Map<String, Long> inputSuspensions = new HashMap<>();

    // The shared charging slot of each machine (M3-1 a), never empty stacks. Its key set is also the set of machines
    // the charging tick works on (C7).
    private final Map<String, ItemStack> chargingItems = new HashMap<>();

    // What charging moved last, for the screen's indicator (M3-5 c). Not saved; stale entries just read as "not
    // charging". Removed with the machine's handler (retireHandler).
    private record LastCharge(long gameTime, int amount) {}
    private final Map<String, LastCharge> lastCharges = new HashMap<>();

    public MachineRegistryData() {}


    private MachineRegistryData(List<MachineRecord> records, List<PackedMachine> packedRecords,
                                List<ChargingEntry> chargingRecords)
    {
        for (MachineRecord record : records) {
            machines.put(record.id(), record);
        }

        for (PackedMachine packed : packedRecords) {
            packedMachines.put(packed.token(), packed);
        }

        for (ChargingEntry entry : chargingRecords)
        {
            if (entry.item().isEmpty())
            {
                Fluxbound.LOGGER.error("The charging item of machine {} could not be loaded and was skipped",
                        entry.machineId());
            }
            else if (!machines.containsKey(entry.machineId()))
            {
                // Never written by this code (C1); seen only with damaged or edited save data.
                Fluxbound.LOGGER.error("Skipped charging item {} of unknown machine {}",
                        entry.item().get(), entry.machineId());
            }
            else
            {
                chargingItems.put(entry.machineId(), entry.item().get());
            }
        }
    }

    public ItemStack getChargingItem(String id)
    {
        return chargingItems.getOrDefault(id, ItemStack.EMPTY);
    }

    // Only the menu writes here, after its checks (C5). An unknown machine is refused so that no entry outlives its
    // machine (C1); vanilla checks the menu's validity before every click, so this is never reached with a real item.
    public void setChargingItem(String id, ItemStack item)
    {
        if (!machines.containsKey(id))
        {
            Fluxbound.LOGGER.error("Refused charging item {} for unknown machine {}", item, id);
            return;
        }

        if (item.isEmpty())
        {
            chargingItems.remove(id);
        }
        else
        {
            chargingItems.put(id, item);
        }

        setDirty();
    }

    // The stored stack was changed in place (for example split by the slot).
    public void chargingItemChanged()
    {
        setDirty();
    }

    private List<ChargingEntry> chargingEntries()
    {
        return chargingItems.entrySet().stream()
                .map(entry -> new ChargingEntry(entry.getKey(), Optional.of(entry.getValue())))
                .toList();
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
        return storageFor(id, level, capacity, inputPerTick);
    }

    // The machine's cached energy storage, created on first use; null if the machine doesn't exist.
    private @Nullable MachineEnergyStorage storageFor(String id, ServerLevel level, int capacity, int inputPerTick)
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

    // Charges the items of this level's machines once per tick (M3-2 a). Only machines with an item are visited (C7),
    // and only while at least one of their blocks is block-ticking, which never loads a chunk (M3-3 a, C6).
    public void tickCharging(ServerLevel level)
    {
        String dimension = level.dimension().identifier().toString();

        for (Map.Entry<String, ItemStack> entry : chargingItems.entrySet())
        {
            MachineRecord record = machines.get(entry.getKey());

            if (record == null || !record.dimension().equals(dimension) || !anyMemberTicking(level, record))
            {
                continue;
            }

            EnergyHandler item = ItemAccess.forStack(entry.getValue()).getCapability(Capabilities.Energy.ITEM);

            if (item == null)
            {
                continue;
            }

            int blocks = record.blockCount();
            int rate = Math.toIntExact(MachineEnergyRules.inputPerTick(blocks));
            int moved = storageFor(record.id(), level, Math.toIntExact(MachineEnergyRules.capacity(blocks)), rate)
                    .chargeInto(item, rate);

            // The stack's stored energy changed in place.
            if (moved > 0)
            {
                lastCharges.put(record.id(), new LastCharge(level.getGameTime(), moved));
                setDirty();
            }
        }
    }

    // The FE moved into the machine's item at most one tick ago, or 0. One tick of tolerance, because menus are synced
    // in the player's tick, which can run before the level's tick.
    public int chargedRecently(String id, long gameTime)
    {
        LastCharge last = lastCharges.get(id);
        return last != null && last.gameTime() >= gameTime - 1 ? last.amount() : 0;
    }

    private static boolean anyMemberTicking(ServerLevel level, MachineRecord record)
    {
        for (long member : record.members())
        {
            if (level.shouldTickBlocksAt(BlockPos.of(member)))
            {
                return true;
            }
        }

        return false;
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
        lastCharges.remove(id);
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
            applyMerge(level, plan, pos);
            Fluxbound.LOGGER.info("Placement at {} merged {} machine(s) into {} ({} blocks)",
                    pos, plan.absorbedIds().size(), plan.result().id(), plan.result().blockCount());
        }
    }

    public boolean tryManualJoin(ServerLevel level, String firstId, String secondId, BlockPos trigger)
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

        applyMerge(level, plan, trigger);
        Fluxbound.LOGGER.info("Manually joined machines {} and {} into {}", firstId, secondId, plan.result().id());
        return true;
    }

    // Registry first, then the loaded blocks (P6). Every involved machine's energy handler is retired (I7).
    private void applyMerge(ServerLevel level, MergePlan plan, BlockPos trigger)
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

        // M3-9 a, C2. Computed before the map changes; the drops below happen after the registry update (P6).
        ChargingTransfer transfer = MachineRestructuring.planChargingItems(plan, chargingItems.keySet());

        if (transfer.movedFrom() != null)
        {
            chargingItems.put(result.id(), chargingItems.remove(transfer.movedFrom()));
        }

        List<ItemStack> dropped = new ArrayList<>();

        for (String id : transfer.droppedFrom())
        {
            dropped.add(chargingItems.remove(id));
        }

        setDirty();
        synchronizeLoadedMembers(level, List.of(result));

        // On top of the triggering block, because inside it the item entity would be pushed out in a random direction.
        BlockPos dropAt = trigger.above();

        for (ItemStack item : dropped)
        {
            Containers.dropItemStack(level, dropAt.getX(), dropAt.getY(), dropAt.getZ(), item);
        }
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

        // Only the removal of the last block retires the machine; otherwise the ID-keeping piece keeps the item (C2).
        ItemStack chargingItem = plan.pieces().isEmpty() ? chargingItems.remove(record.id()) : null;

        setDirty();
        synchronizeLoadedMembers(level, replacements);
        level.invalidateCapabilities(BlockPos.of(removedPosition));

        // Called from preRemoveSideEffects, where vanilla drops container contents as well.
        if (chargingItem != null)
        {
            BlockPos at = BlockPos.of(removedPosition);
            Containers.dropItemStack(level, at.getX(), at.getY(), at.getZ(), chargingItem);
        }

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
