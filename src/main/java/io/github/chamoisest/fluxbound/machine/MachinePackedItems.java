
package io.github.chamoisest.fluxbound.machine;

import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.machine.data.PackedMachine;
import io.github.chamoisest.fluxbound.machine.data.PackedMachineDisplay;
import io.github.chamoisest.fluxbound.machine.energy.MachineEnergyRules;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public final class MachinePackedItems
{
    private MachinePackedItems() {}


    public static void attach(ServerLevel level, ItemStack stack, PackedMachine packed)
    {
        if (packed == null || !packed.hasPersistentState()
                || stack.has(ModDataComponents.PACKED_MACHINE.get())) {
            return;
        }

        MachineRegistryData.get(level).registerPacked(packed);

        stack.set(ModDataComponents.PACKED_MACHINE.get(), packed.token());
        stack.set(ModDataComponents.PACKED_DISPLAY.get(), new PackedMachineDisplay(
                packed.energy(),
                MachineEnergyRules.capacity(1),
                MachineEnergyRules.inputPerTick(1)
        ));
    }

}
