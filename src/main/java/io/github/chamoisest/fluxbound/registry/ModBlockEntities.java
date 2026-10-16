
package io.github.chamoisest.fluxbound.registry;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Fluxbound.MODID);

    public static final Supplier<BlockEntityType<ArrivalAnchorBlockEntity>> ARRIVAL_ANCHOR =
            BLOCK_ENTITIES.register("arrival_anchor", () ->
                    new BlockEntityType<>(ArrivalAnchorBlockEntity::new, false, ModBlocks.ARRIVAL_ANCHOR.get()));

    private ModBlockEntities() {}

    public static void register(IEventBus modEventBus)
    {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
