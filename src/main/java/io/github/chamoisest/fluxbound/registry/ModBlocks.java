package io.github.chamoisest.fluxbound.registry;

import io.github.chamoisest.fluxbound.Fluxbound;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.chamoisest.fluxbound.machine.block.ArrivalAnchorBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class ModBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Fluxbound.MODID);

    public static final DeferredBlock<ArrivalAnchorBlock> ARRIVAL_ANCHOR =
            BLOCKS.registerBlock("arrival_anchor", ArrivalAnchorBlock::new,
                    properties -> properties.mapColor(MapColor.METAL)
                            .strength(3.5F)
                            .requiresCorrectToolForDrops());

    private ModBlocks() {}

    public static void register(IEventBus modEventBus)
    {
        BLOCKS.register(modEventBus);
    }
}
