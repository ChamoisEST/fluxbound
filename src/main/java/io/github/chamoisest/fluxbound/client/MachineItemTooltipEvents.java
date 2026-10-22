
package io.github.chamoisest.fluxbound.client;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.data.PackedMachineDisplay;
import io.github.chamoisest.fluxbound.machine.energy.EnergyFormat;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
import io.github.chamoisest.fluxbound.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = Fluxbound.MODID, value = Dist.CLIENT)
public final class MachineItemTooltipEvents
{
    private MachineItemTooltipEvents() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();

        if (!stack.is(ModItems.ARRIVAL_ANCHOR.get())) {
            return;
        }

        PackedMachineDisplay display = stack.get(ModDataComponents.PACKED_DISPLAY.get());

        long energy = display != null ? display.energy() : 0;
        long capacity = display != null
                ? display.capacity()
                : ClientConfigValues.arrivalAnchorCapacityPerBlock();
        long input = display != null
                ? display.inputPerTick()
                : ClientConfigValues.arrivalAnchorInputPerTickPerBlock();

        event.getToolTip().add(
                Component.translatable("tooltip.fluxbound.stored").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(EnergyFormat.stored(energy, capacity))
                                .withStyle(ChatFormatting.WHITE))
        );

        event.getToolTip().add(
                Component.translatable("tooltip.fluxbound.max_input").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(EnergyFormat.perTick(input))
                                .withStyle(ChatFormatting.WHITE))
        );
    }
}
