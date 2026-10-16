package io.github.chamoisest.fluxbound.registry;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities
{
    private ModCapabilities() {}

    public static void register(RegisterCapabilitiesEvent event)
    {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                ModBlockEntities.ARRIVAL_ANCHOR.get(),
                (anchor, side) -> side != null && side != Direction.UP
                        ? anchor.getEnergyHandler()
                        : null
        );
    }
}
