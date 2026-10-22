
package io.github.chamoisest.fluxbound;

import com.mojang.logging.LogUtils;
import io.github.chamoisest.fluxbound.registry.*;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import io.github.chamoisest.fluxbound.network.ModNetwork;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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
        modEventBus.addListener(ModNetwork::register);

        container.registerConfig(ModConfig.Type.COMMON, ModConfigs.SPEC);

        LOGGER.info("Fluxbound initialized successfully!");
    }
}
