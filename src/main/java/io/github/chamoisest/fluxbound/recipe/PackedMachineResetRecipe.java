package io.github.chamoisest.fluxbound.recipe;

import com.mojang.serialization.MapCodec;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
import io.github.chamoisest.fluxbound.registry.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

// A packed Arrival Anchor alone in a crafting grid becomes a default one (M2-D4, E1 to E3, user decisions 2026-10-24).
// The result carries no components, so it stacks with new Anchors. Mods that do the same keep their data on the item;
// ours lives in the registry, which PackedMachineResetEvents cleans up when a player takes the result.
public class PackedMachineResetRecipe extends CustomRecipe
{
    // No settings, so one shared instance, like vanilla's RepairItemRecipe.
    public static final PackedMachineResetRecipe INSTANCE = new PackedMachineResetRecipe();
    public static final MapCodec<PackedMachineResetRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, PackedMachineResetRecipe> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<PackedMachineResetRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private PackedMachineResetRecipe() {}

    // The token of the only item in the grid if it is a packed Arrival Anchor, otherwise null. Whether the token is
    // still valid doesn't matter: items with unknown or redeemed tokens can be reset too.
    public static @Nullable String packedToken(CraftingInput input)
    {
        if (input.ingredientCount() != 1)
        {
            return null;
        }

        for (int i = 0; i < input.size(); i++)
        {
            ItemStack stack = input.getItem(i);

            if (!stack.isEmpty())
            {
                return stack.is(ModItems.ARRIVAL_ANCHOR.get()) ? stack.get(ModDataComponents.PACKED_MACHINE.get()) : null;
            }
        }

        return null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level)
    {
        return packedToken(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input)
    {
        return new ItemStack(ModItems.ARRIVAL_ANCHOR.get());
    }

    @Override
    public RecipeSerializer<PackedMachineResetRecipe> getSerializer()
    {
        return SERIALIZER;
    }
}
