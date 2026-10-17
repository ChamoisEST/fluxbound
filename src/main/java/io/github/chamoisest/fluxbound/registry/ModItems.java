
package io.github.chamoisest.fluxbound.registry;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.item.FluxboundWrenchItem;
import io.github.chamoisest.fluxbound.item.TeleportationGuideItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;

public final class ModItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fluxbound.MODID);

    public static final DeferredItem<TeleportationGuideItem> TELEPORTATION_GUIDE =
            ITEMS.registerItem("teleportation_guide", TeleportationGuideItem::new, properties -> properties.stacksTo(1));

    public static final DeferredItem<FluxboundWrenchItem> WRENCH =
            ITEMS.registerItem("wrench", FluxboundWrenchItem::new, properties -> properties.stacksTo(1));

    public static final DeferredItem<Item> BASIC_TELEPORTATION_CORE =
            ITEMS.registerSimpleItem("basic_teleportation_core");

    public static final DeferredItem<Item> TELEPORTATION_CHASSIS =
            ITEMS.registerSimpleItem("teleportation_chassis");

    public static final DeferredItem<Item> TELEPORTATION_CASING =
            ITEMS.registerSimpleItem("teleportation_casing");

    public static final DeferredItem<BlockItem> ARRIVAL_ANCHOR =
            ITEMS.registerSimpleBlockItem("arrival_anchor", ModBlocks.ARRIVAL_ANCHOR);


    private ModItems() {}

    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }
}
