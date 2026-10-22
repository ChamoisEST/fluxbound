package io.github.chamoisest.fluxbound.network;

import io.github.chamoisest.fluxbound.network.config.ConfigSyncPayload;
import io.github.chamoisest.fluxbound.network.menu.MachineOverviewPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.SetAccessModePayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork
{
    // Bump whenever a payload is added, removed or changes its wire format.
    // NeoForge refuses connections whose payload versions differ.
    public static final String PROTOCOL_VERSION = "3";

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Handlers that reference client-only classes are attached on the client through
        // RegisterClientPayloadHandlersEvent; those using only common classes are registered here.
        registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
        registrar.playToClient(MachineOverviewPayload.TYPE, MachineOverviewPayload.STREAM_CODEC,
                MachineOverviewPayload::handleOnClient);
        registrar.playToClient(MachinePermissionsPayload.TYPE, MachinePermissionsPayload.STREAM_CODEC,
                MachinePermissionsPayload::handleOnClient);
        registrar.playToServer(SetAccessModePayload.TYPE, SetAccessModePayload.STREAM_CODEC,
                SetAccessModePayload::handleOnServer);
    }
}
