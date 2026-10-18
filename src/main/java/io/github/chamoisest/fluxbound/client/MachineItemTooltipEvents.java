
package io.github.chamoisest.fluxbound.client;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import io.github.chamoisest.fluxbound.machine.data.PackedMachineDisplay;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
import io.github.chamoisest.fluxbound.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
                : ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get();
        long input = display != null
                ? display.inputPerTick()
                : ModConfigs.ARRIVAL_ANCHOR_INPUT.get();

        event.getToolTip().add(
                Component.translatable("tooltip.fluxbound.stored").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(
                                compact(energy) + " / " + compact(capacity) + " FE"
                        ).withStyle(ChatFormatting.WHITE))
        );

        event.getToolTip().add(
                Component.translatable("tooltip.fluxbound.max_input").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(
                                compact(input) + " FE/t"
                        ).withStyle(ChatFormatting.WHITE))
        );
    }

    private static String compact(long value)
    {
        if (value >= 1_000_000_000L) {
            return scaled(value, 1_000_000_000L, "G");
        }

        if (value >= 1_000_000L) {
            return scaled(value, 1_000_000L, "M");
        }

        if (value >= 1_000L) {
            return scaled(value, 1_000L, "k");
        }

        return Long.toString(value);
    }

    private static String scaled(long value, long divisor, String suffix)
    {
        return BigDecimal.valueOf(value)
                .divide(BigDecimal.valueOf(divisor), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + suffix;
    }
}
