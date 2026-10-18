
package io.github.chamoisest.fluxbound;

import com.mojang.logging.LogUtils;
import guideme.Guide;
import io.github.chamoisest.fluxbound.registry.*;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;


@Mod(Fluxbound.MODID)
public class Fluxbound {

    public static final String MODID = "fluxbound";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Fluxbound(IEventBus modEventBus, ModContainer container){
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);

        container.registerConfig(ModConfig.Type.SERVER, ModConfigs.SPEC);

        Guide.builder(Identifier.parse("fluxbound:guide")).build();

        LOGGER.info("Fluxbound initialized successfully!");
    }
}
