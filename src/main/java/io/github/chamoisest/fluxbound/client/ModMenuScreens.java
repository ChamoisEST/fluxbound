package io.github.chamoisest.fluxbound.client;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.client.gui.arrivalanchor.ArrivalAnchorScreen;
import io.github.chamoisest.fluxbound.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

// Connects menu types to their screens. Client only, so screen classes never load on a dedicated server.
@EventBusSubscriber(modid = Fluxbound.MODID, value = Dist.CLIENT)
public final class ModMenuScreens
{
    private ModMenuScreens() {}

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event)
    {
        event.register(ModMenuTypes.ARRIVAL_ANCHOR.get(), ArrivalAnchorScreen::new);
    }
}
