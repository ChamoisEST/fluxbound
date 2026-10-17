package io.github.chamoisest.fluxbound.machine;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import io.github.chamoisest.fluxbound.machine.data.PackedMachine;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
import io.github.chamoisest.fluxbound.registry.ModItems;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber(modid = Fluxbound.MODID)
public final class MachinePickupEvents
{
    private MachinePickupEvents() {}


    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event)
    {
        if (!(event.getBlockEntity() instanceof ArrivalAnchorBlockEntity anchor)) {
            return;
        }

        if (event.getBreaker() instanceof Player player && player.isCreative()) {
            return;
        }

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();

            if (!stack.is(ModItems.ARRIVAL_ANCHOR.get())) {
                continue;
            }

            if (stack.has(ModDataComponents.PACKED_MACHINE.get())) {
                return;
            }

            PackedMachine packed = anchor.takePendingPickup();

            if (packed == null) {
                return;
            }

            MachinePackedItems.attach(event.getLevel(), stack, packed);
            drop.setItem(stack);

            Fluxbound.LOGGER.info("Packed Arrival Anchor {} with {} FE",
                    packed.machineId(), packed.energy());

            return;
        }
    }

}
