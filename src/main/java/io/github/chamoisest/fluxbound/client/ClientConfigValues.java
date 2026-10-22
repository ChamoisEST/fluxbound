package io.github.chamoisest.fluxbound.client;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import io.github.chamoisest.fluxbound.network.config.ConfigSyncPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

// The server's config values as seen by this client. Falls back to the local config
// while not connected (for example, tooltips in the main menu or before the first sync).
@EventBusSubscriber(modid = Fluxbound.MODID, value = Dist.CLIENT)
public final class ClientConfigValues
{
    // Null until the server sends its values; only accessed on the client main thread.
    private static ConfigSyncPayload synced;

    private ClientConfigValues() {}

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterClientPayloadHandlersEvent event)
    {
        event.register(ConfigSyncPayload.TYPE, (payload, context) -> synced = payload);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        synced = null;
    }

    public static int arrivalAnchorCapacityPerBlock()
    {
        return synced != null ? synced.anchorCapacityPerBlock() : ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get();
    }

    public static int arrivalAnchorInputPerTickPerBlock()
    {
        return synced != null ? synced.anchorInputPerTickPerBlock() : ModConfigs.ARRIVAL_ANCHOR_INPUT.get();
    }
}
