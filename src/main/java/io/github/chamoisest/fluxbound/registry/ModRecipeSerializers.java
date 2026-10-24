package io.github.chamoisest.fluxbound.registry;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.recipe.PackedMachineResetRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Fluxbound.MODID);

    public static final Supplier<RecipeSerializer<PackedMachineResetRecipe>> PACKED_MACHINE_RESET =
            RECIPE_SERIALIZERS.register("packed_machine_reset", () -> PackedMachineResetRecipe.SERIALIZER);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
