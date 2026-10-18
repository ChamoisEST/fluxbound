package io.github.chamoisest.fluxbound.registry;

import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public final class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Fluxbound.MODID);

    // Items deliberately kept out of the creative tab. Empty for now; add an item here to hide it.
    private static final Set<DeferredHolder<Item, ? extends Item>> HIDDEN_ITEMS = Set.of();

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLUXBOUND_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fluxbound"))
                    .icon(() -> ModItems.TELEPORTATION_GUIDE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        for (DeferredHolder<Item, ? extends Item> entry : ModItems.ITEMS.getEntries()) {
                            if (!HIDDEN_ITEMS.contains(entry)) {
                                output.accept(entry.get());
                            }
                        }
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus)
    {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
