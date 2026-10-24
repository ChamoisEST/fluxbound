package io.github.chamoisest.fluxbound.item;

import io.github.chamoisest.fluxbound.machine.MachinePackedItems;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import io.github.chamoisest.fluxbound.machine.data.MachineRecord;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.machine.data.PackedMachine;
import io.github.chamoisest.fluxbound.registry.ModBlocks;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
import io.github.chamoisest.fluxbound.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import io.github.chamoisest.fluxbound.machine.permission.MachineAccess;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;

public class FluxboundWrenchItem extends Item
{
    public FluxboundWrenchItem(Properties properties)
    {
        super(properties);
    }


    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand)
    {
        ItemStack wrench = player.getItemInHand(hand);

        if (!wrench.has(ModDataComponents.WRENCH_SELECTION.get())) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            wrench.remove(ModDataComponents.WRENCH_SELECTION.get());
            message(player, "message.fluxbound.wrench.cleared");
        }

        return InteractionResult.SUCCESS;
    }


    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        BlockPos pos = context.getClickedPos();

        if (!context.getLevel().getBlockState(pos).is(ModBlocks.ARRIVAL_ANCHOR.get())) {
            return InteractionResult.PASS;
        }

        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) {
            message(player, "message.fluxbound.wrench.denied");
            return InteractionResult.SUCCESS;
        }

        MachineRegistryData registry = MachineRegistryData.get(level);
        MachineRecord target = registry.findAt(level.dimension().identifier().toString(), pos.asLong());

        if (target == null) {
            // No machine contains this block: its data was lost or it's a copy (M2 G2-2). It can only come back as a
            // default item, which carries no state, so no Manage check applies (anyone can mine it as well).
            if (player.isShiftKeyDown()) {
                pickup(level, player, pos, stack);
            } else {
                message(player, "message.fluxbound.anchor.orphaned");
            }

            return InteractionResult.SUCCESS;
        }

        if (!MachineAccess.has(player, target, MachineRight.MANAGE)) {
            message(player, "message.fluxbound.wrench.denied");
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            pickup(level, player, pos, stack);
        } else {
            selectOrJoin(level, player, target, stack);
        }

        return InteractionResult.SUCCESS;
    }

    private void selectOrJoin(ServerLevel level, ServerPlayer player, MachineRecord target, ItemStack wrench)
    {
        String selectedId = wrench.get(ModDataComponents.WRENCH_SELECTION.get());

        if (selectedId == null) {
            wrench.set(ModDataComponents.WRENCH_SELECTION.get(), target.id());
            message(player, "message.fluxbound.wrench.selected", target.blockCount());
            return;
        }

        if (selectedId.equals(target.id())) {
            wrench.remove(ModDataComponents.WRENCH_SELECTION.get());
            message(player, "message.fluxbound.wrench.cleared");
            return;
        }

        MachineRegistryData registry = MachineRegistryData.get(level);
        MachineRecord selected = registry.find(selectedId);

        // The selected machine no longer exists, for example after it was split, merged or removed.
        if (selected == null) {
            wrench.remove(ModDataComponents.WRENCH_SELECTION.get());
            message(player, "message.fluxbound.wrench.invalid");
            return;
        }

        if (!MachineAccess.has(player, selected, MachineRight.MANAGE)) {
            wrench.remove(ModDataComponents.WRENCH_SELECTION.get());
            message(player, "message.fluxbound.wrench.denied");
            return;
        }

        if (registry.tryManualJoin(level, selectedId, target.id())) {
            wrench.remove(ModDataComponents.WRENCH_SELECTION.get());
            message(player, "message.fluxbound.wrench.joined");
        } else {
            // An ordinary failed join keeps the selection, so the player can try another machine.
            message(player, "message.fluxbound.wrench.incompatible");
        }
    }

    private void pickup(ServerLevel level, ServerPlayer player, BlockPos pos, ItemStack wrench)
    {
        if (!(level.getBlockEntity(pos) instanceof ArrivalAnchorBlockEntity anchor)) {
            message(player, "message.fluxbound.wrench.invalid");
            return;
        }

        // No vanilla loot drop is generated by removeBlock().
        // The block entity's normal removal callback prepares the packed data.
        if (!level.removeBlock(pos, false)) {
            message(player, "message.fluxbound.wrench.failed");
            return;
        }

        // Null for an orphaned block, which comes back as a default item (attach ignores null).
        PackedMachine packed = anchor.takePendingPickup();

        ItemStack result = new ItemStack(ModItems.ARRIVAL_ANCHOR.get());
        MachinePackedItems.attach(level, result, packed);

        // Give the result to the player, including in Creative Mode.
        // If inventory space is unavailable, drop it at the player's feet.
        player.getInventory().add(result);

        if (result.isEmpty()) {
            // No item entity exists, so vanilla's pickup sound doesn't play; same sound and values as vanilla's.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP,
                    SoundSource.PLAYERS, 0.2F,
                    (player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 1.4F + 2.0F);
        } else {
            player.drop(result, false);
        }

        wrench.remove(ModDataComponents.WRENCH_SELECTION.get());
        message(player, "message.fluxbound.wrench.picked_up");
    }

    private static void message(Player player, String key, Object... arguments)
    {
        player.sendOverlayMessage(Component.translatable(key, arguments));
    }
}
