package io.github.chamoisest.fluxbound.registry;

import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs
{
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Fluxbound.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLUXBOUND_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fluxbound"))
                    .icon(() -> ModItems.TELEPORTATION_GUIDE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.TELEPORTATION_GUIDE.get());
                        output.accept(ModItems.BASIC_TELEPORTATION_CORE.get());
                        output.accept(ModItems.TELEPORTATION_CHASSIS.get());
                        output.accept(ModItems.TELEPORTATION_CASING.get());
                        output.accept(ModItems.ARRIVAL_ANCHOR.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(IEventBus modEventBus)
    {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
