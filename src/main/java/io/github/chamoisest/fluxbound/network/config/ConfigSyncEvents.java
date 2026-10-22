package io.github.chamoisest.fluxbound.network.config;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.config.ModConfigs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = Fluxbound.MODID)
public final class ConfigSyncEvents
{
    private ConfigSyncEvents() {}

    // Fires for the joining player on login, and for all players on /reload.
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event)
    {
        ConfigSyncPayload payload = new ConfigSyncPayload(
                ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get(),
                ModConfigs.ARRIVAL_ANCHOR_INPUT.get()
        );

        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }
}
