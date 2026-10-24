package io.github.chamoisest.fluxbound.recipe;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.machine.data.PackedMachine;
import io.github.chamoisest.fluxbound.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.CraftingContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Fluxbound.MODID)
public final class PackedMachineResetEvents
{
    private PackedMachineResetEvents() {}

    // Fires once per craft on the server, before the grid is used up (vanilla ResultSlot.checkTakeAchievements). Only
    // player crafting fires it; automated crafting leaves the packed record dormant, like a lost item's (M2-E2).
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event)
    {
        if (!(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getInventory() instanceof CraftingContainer grid)
                || !event.getCrafting().is(ModItems.ARRIVAL_ANCHOR.get()))
        {
            return;
        }

        String token = PackedMachineResetRecipe.packedToken(grid.asCraftInput());

        if (token == null)
        {
            return;
        }

        PackedMachine retired = MachineRegistryData.get(level).retirePacked(token);

        if (retired != null)
        {
            Fluxbound.LOGGER.info("{} reset packed Arrival Anchor {} by crafting; {} FE discarded",
                    event.getEntity().getName().getString(), retired.machineId(), retired.energy());
        }
    }
}
