package io.github.chamoisest.fluxbound.registry;

import com.mojang.serialization.Codec;
import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.chamoisest.fluxbound.machine.data.PackedMachineDisplay;

import java.util.function.Supplier;

public final class ModDataComponents
{
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Fluxbound.MODID);

    public static final Supplier<DataComponentType<String>> PACKED_MACHINE =
            COMPONENTS.registerComponentType("packed_machine", builder ->
                    builder.persistent(Codec.STRING));


    public static final Supplier<DataComponentType<PackedMachineDisplay>> PACKED_DISPLAY =
            COMPONENTS.registerComponentType("packed_display", builder ->
                    builder.persistent(PackedMachineDisplay.CODEC));

    public static final Supplier<DataComponentType<String>> WRENCH_SELECTION =
            COMPONENTS.registerComponentType("wrench_selection",
                    builder -> builder.persistent(Codec.STRING));


    private ModDataComponents() {}

    public static void register(IEventBus modEventBus)
    {
        COMPONENTS.register(modEventBus);
    }
}
