
package io.github.chamoisest.fluxbound.integration.jade;

import io.github.chamoisest.fluxbound.machine.block.ArrivalAnchorBlock;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class FluxboundJadePlugin implements IWailaPlugin
{
    @Override
    public void register(IWailaCommonRegistration registration)
    {
        registration.registerBlockDataProvider(MachineEnergyJadeProvider.INSTANCE, ArrivalAnchorBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        registration.registerBlockComponent(MachineEnergyJadeComponent.INSTANCE, ArrivalAnchorBlock.class);
    }
}
