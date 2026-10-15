
package io.github.chamoisest.fluxbound;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.chamoisest.fluxbound.item.TeleportationGuideItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.slf4j.Logger;

import guideme.Guide;
import net.minecraft.resources.Identifier;


@Mod(Fluxbound.MODID)
public class Fluxbound {

    public static final String MODID = "fluxbound";

    public static final Logger LOGGER = LogUtils.getLogger();


    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MODID);


    public static final DeferredItem<TeleportationGuideItem> TELEPORTATION_GUIDE =
            ITEMS.registerItem(
                    "teleportation_guide",
                    TeleportationGuideItem::new,
                    properties -> properties.stacksTo(1)
            );


    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLUXBOUND_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fluxbound"))
                    .icon(() -> TELEPORTATION_GUIDE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(TELEPORTATION_GUIDE.get());
                    })
                    .build());



    public Fluxbound(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);

        CREATIVE_MODE_TABS.register(modEventBus);

        Guide.builder(Identifier.parse("fluxbound:guide")).build();

        LOGGER.info("Fluxbound initialized successfully!");
    }
}
