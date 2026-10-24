package io.github.chamoisest.fluxbound.menu.arrivalanchor;

import io.github.chamoisest.fluxbound.machine.charging.ChargingItems;
import io.github.chamoisest.fluxbound.machine.charging.ChargingState;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.machine.energy.MachineEnergyRules;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.machine.permission.TrustChange;
import io.github.chamoisest.fluxbound.machine.permission.TrustRequests;
import io.github.chamoisest.fluxbound.menu.ChargingSlot;
import io.github.chamoisest.fluxbound.menu.ChargingSlotContainer;
import io.github.chamoisest.fluxbound.menu.MachineMenuLayout;
import io.github.chamoisest.fluxbound.menu.MachinePermissionsSnapshot;
import io.github.chamoisest.fluxbound.menu.MenuSyncTracker;
import io.github.chamoisest.fluxbound.menu.PlayerNames;
import io.github.chamoisest.fluxbound.network.menu.MachineChargingPayload;
import io.github.chamoisest.fluxbound.network.menu.MachineOverviewPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.TrustResultPayload;
import io.github.chamoisest.fluxbound.registry.ModBlocks;
import io.github.chamoisest.fluxbound.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class ArrivalAnchorMenu extends AbstractContainerMenu
{
    // Same distance buffer as vanilla container blocks.
    private static final double RANGE_BUFFER = 4.0;

    // Slot indices as added by addStandardInventorySlots: main inventory first, then the hotbar. The charging slot
    // is added after them.
    private static final int HOTBAR_START = 27;
    private static final int INVENTORY_END = 36;
    private static final int CHARGING_SLOT = 36;

    // In the energy bar's column (x 8-26), below the bar; the indicator (M3-B4) goes underneath.
    private static final int CHARGING_SLOT_X = 9;
    private static final int CHARGING_SLOT_Y = 98;

    // Server side only; all null on the client.
    private final ServerPlayer viewer;
    private final ServerLevel level;
    private final String dimension;
    private final BlockPos pos;
    private final String machineId;
    private final MenuSyncTracker sync;

    // Client side only; null until the first Overview payload arrives.
    private MachineOverviewPayload overview;

    // Client side only; null until the first Charging payload arrives.
    private MachineChargingPayload charging;

    // Client side only; null until the first Permissions payload arrives.
    private MachinePermissionsPayload permissions;

    // Client side only; the answer to the viewer's last trust request, null until there is one.
    private TrustResultPayload trustResult;

    // Client constructor, called through the menu type when the server opens the menu.
    public ArrivalAnchorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData)
    {
        super(ModMenuTypes.ARRIVAL_ANCHOR.get(), containerId);
        this.viewer = null;
        this.level = null;
        this.dimension = null;
        this.pos = null;
        this.machineId = null;
        this.sync = null;

        addStandardInventorySlots(inventory, MachineMenuLayout.INVENTORY_X, MachineMenuLayout.INVENTORY_Y);
        // Filled by vanilla's slot sync; the synced flag only predicts the server's decision.
        addSlot(new ChargingSlot(new SimpleContainer(1), CHARGING_SLOT_X, CHARGING_SLOT_Y,
                () -> overview != null && overview.viewerMayCharge()));
    }

    // Server constructor. The menu stays bound to the clicked block and to the machine ID it had when opened.
    public ArrivalAnchorMenu(int containerId, ServerPlayer viewer, ServerLevel level, BlockPos pos, String machineId)
    {
        super(ModMenuTypes.ARRIVAL_ANCHOR.get(), containerId);
        this.viewer = viewer;
        this.level = level;
        this.dimension = level.dimension().identifier().toString();
        this.pos = pos.immutable();
        this.machineId = machineId;
        this.sync = new MenuSyncTracker();

        addStandardInventorySlots(viewer.getInventory(), MachineMenuLayout.INVENTORY_X, MachineMenuLayout.INVENTORY_Y);
        addSlot(new ChargingSlot(new ChargingSlotContainer(MachineRegistryData.get(level), machineId),
                CHARGING_SLOT_X, CHARGING_SLOT_Y, this::viewerMayCharge));
        sync.add(this::overviewSnapshot);
        sync.add(this::chargingSnapshot);
        sync.add(this::permissionsSnapshot);
    }

    public MachineOverviewPayload getOverview()
    {
        return overview;
    }

    public void setOverview(MachineOverviewPayload overview)
    {
        this.overview = overview;
    }

    public MachineChargingPayload getCharging()
    {
        return charging;
    }

    public void setCharging(MachineChargingPayload charging)
    {
        this.charging = charging;
    }

    public MachinePermissionsPayload getPermissions()
    {
        return permissions;
    }

    public void setPermissions(MachinePermissionsPayload permissions)
    {
        this.permissions = permissions;
    }

    public TrustResultPayload getTrustResult()
    {
        return trustResult;
    }

    public void setTrustResult(TrustResultPayload trustResult)
    {
        this.trustResult = trustResult;
    }

    // Server side: a viewer's request from the Permissions tab. The result reaches the client through the next
    // sync (B5-1).
    public void requestAccessMode(String requestedMachineId, MachineRight right, AccessMode mode)
    {
        if (!acceptsRequest(requestedMachineId))
        {
            return;
        }

        MachineRegistryData.get(level).setAccessMode(viewer, machineId, right, mode);
    }

    // Server side: a viewer's request from the Trusted Players tab. A lookup's answer is only applied and sent
    // while this menu is still open (M1 B6-4).
    public void requestAddTrusted(String requestedMachineId, String name)
    {
        if (!acceptsRequest(requestedMachineId))
        {
            return;
        }

        TrustRequests.add(viewer, machineId, name,
                player -> player.containerMenu == this && stillValid(player), this::sendTrustResult);
    }

    public void requestRemoveTrusted(String requestedMachineId, String storedId)
    {
        if (!acceptsRequest(requestedMachineId))
        {
            return;
        }

        TrustChange result = MachineRegistryData.get(level).removeTrusted(viewer, machineId, storedId);
        sendTrustResult(viewer, result, PlayerNames.find(level.getServer(), storedId).orElse(storedId));
    }

    // Called by ServerPlayer.tick every tick, before stillValid.
    @Override
    public void broadcastChanges()
    {
        super.broadcastChanges();

        if (sync != null)
        {
            sync.sendChanges(viewer);
        }
    }

    // Called when the menu opens, and when vanilla resynchronizes the whole menu.
    @Override
    public void sendAllDataToRemote()
    {
        super.sendAllDataToRemote();

        if (sync != null)
        {
            sync.sendAll(viewer);
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        // The client can't check the machine; the server closes the menu for it.
        if (level == null)
        {
            return true;
        }

        // isLoaded comes first so that the check never loads a chunk (E17).
        return player.level() == level
                && level.isLoaded(pos)
                && level.getBlockState(pos).is(ModBlocks.ARRIVAL_ANCHOR.get())
                && player.isWithinBlockInteractionRange(pos, RANGE_BUFFER)
                && currentRecord() != null;
    }

    // Shift-click: the charging slot empties into the inventory, hotbar first, like vanilla containers. From the
    // inventory an item goes into the charging slot if the slot takes it (Charge right and accepted item, C5);
    // otherwise it moves between the main inventory and the hotbar, like the player's own inventory.
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex)
    {
        Slot slot = slots.get(slotIndex);

        if (!slot.hasItem())
        {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;

        if (slotIndex == CHARGING_SLOT)
        {
            moved = moveItemStackTo(stack, 0, INVENTORY_END, true);
        }
        else
        {
            moved = moveItemStackTo(stack, CHARGING_SLOT, CHARGING_SLOT + 1, false)
                    || (slotIndex < HOTBAR_START
                            ? moveItemStackTo(stack, HOTBAR_START, INVENTORY_END, false)
                            : moveItemStackTo(stack, 0, HOTBAR_START, false));
        }

        if (!moved)
        {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty())
        {
            slot.setByPlayer(ItemStack.EMPTY, original);
        }
        else
        {
            slot.setChanged();
        }

        slot.onTake(player, stack);
        return original;
    }

    // Requests are ignored unless the menu is still valid and bound to the machine the client named (M1 D3).
    private boolean acceptsRequest(String requestedMachineId)
    {
        return level != null && machineId.equals(requestedMachineId) && stillValid(viewer);
    }

    private void sendTrustResult(ServerPlayer requester, TrustChange result, String name)
    {
        PacketDistributor.sendToPlayer(requester, new TrustResultPayload(containerId, result, name));
    }

    // Server side, checked at every insertion (C5).
    private boolean viewerMayCharge()
    {
        MachineRecord record = currentRecord();
        return record != null && MachineAccess.has(viewer, record, MachineRight.CHARGE);
    }

    // The machine the menu was opened for, if the clicked block still belongs to it.
    // A split or merge that gives the block a different machine ID ends the menu.
    private MachineRecord currentRecord()
    {
        MachineRecord record = MachineRegistryData.get(level).find(machineId);

        if (record == null || !record.contains(dimension, pos.asLong()))
        {
            return null;
        }

        return record;
    }

    private MachineOverviewPayload overviewSnapshot()
    {
        MachineRecord record = currentRecord();

        // broadcastChanges runs before stillValid, so the machine can already be gone here.
        if (record == null)
        {
            return null;
        }

        int members = record.blockCount();

        return new MachineOverviewPayload(
                containerId,
                record.id(),
                record.energy(),
                MachineEnergyRules.capacity(members),
                MachineEnergyRules.inputPerTick(members),
                members,
                ownerName(record.owner()),
                dimension,
                center(record),
                MachineAccess.has(viewer, record, MachineRight.CHARGE)
        );
    }

    // Null only when the machine is gone; the menu closes in that case anyway.
    private MachineChargingPayload chargingSnapshot()
    {
        MachineRecord record = currentRecord();

        if (record == null)
        {
            return null;
        }

        MachineRegistryData registry = MachineRegistryData.get(level);
        int charged = registry.chargedRecently(machineId, level.getGameTime());
        ChargingState state = ChargingItems.state(registry.getChargingItem(machineId), record.energy(), charged);

        return new MachineChargingPayload(containerId, state, state == ChargingState.CHARGING ? charged : 0);
    }

    // Null only when the machine is gone; the menu closes in that case anyway.
    private MachinePermissionsPayload permissionsSnapshot()
    {
        MachineRecord record = currentRecord();

        return record == null ? null : MachinePermissionsSnapshot.create(containerId, record, viewer);
    }

    // Display only: an unknown or unreadable owner is shown as stored, never repaired here.
    private String ownerName(String owner)
    {
        if (owner.isEmpty())
        {
            return "";
        }

        return PlayerNames.find(level.getServer(), owner).orElse(owner);
    }

    // The middle of the members' bounding box, rounded down. Used for display only.
    private static BlockPos center(MachineRecord record)
    {
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int y = 0;

        for (long packed : record.members())
        {
            BlockPos member = BlockPos.of(packed);
            minX = Math.min(minX, member.getX());
            maxX = Math.max(maxX, member.getX());
            minZ = Math.min(minZ, member.getZ());
            maxZ = Math.max(maxZ, member.getZ());
            y = member.getY();
        }

        return new BlockPos(Math.floorDiv(minX + maxX, 2), y, Math.floorDiv(minZ + maxZ, 2));
    }
}
