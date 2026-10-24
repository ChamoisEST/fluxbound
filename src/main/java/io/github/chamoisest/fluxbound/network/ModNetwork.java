package io.github.chamoisest.fluxbound.network;

import io.github.chamoisest.fluxbound.network.config.ConfigSyncPayload;
import io.github.chamoisest.fluxbound.network.menu.AddTrustedPlayerPayload;
import io.github.chamoisest.fluxbound.network.menu.MachineChargingPayload;
import io.github.chamoisest.fluxbound.network.menu.MachineOverviewPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.RemoveTrustedPlayerPayload;
import io.github.chamoisest.fluxbound.network.menu.SetAccessModePayload;
import io.github.chamoisest.fluxbound.network.menu.TrustResultPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork
{
    // Bump whenever a payload is added, removed or changes its wire format.
    // NeoForge refuses connections whose payload versions differ.
    public static final String PROTOCOL_VERSION = "6";

    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Handlers that reference client-only classes are attached on the client through
        // RegisterClientPayloadHandlersEvent; those using only common classes are registered here.
        registrar.playToClient(ConfigSyncPayload.TYPE, ConfigSyncPayload.STREAM_CODEC);
        registrar.playToClient(MachineOverviewPayload.TYPE, MachineOverviewPayload.STREAM_CODEC,
                MachineOverviewPayload::handleOnClient);
        registrar.playToClient(MachineChargingPayload.TYPE, MachineChargingPayload.STREAM_CODEC,
                MachineChargingPayload::handleOnClient);
        registrar.playToClient(MachinePermissionsPayload.TYPE, MachinePermissionsPayload.STREAM_CODEC,
                MachinePermissionsPayload::handleOnClient);
        registrar.playToServer(SetAccessModePayload.TYPE, SetAccessModePayload.STREAM_CODEC,
                SetAccessModePayload::handleOnServer);
        registrar.playToServer(AddTrustedPlayerPayload.TYPE, AddTrustedPlayerPayload.STREAM_CODEC,
                AddTrustedPlayerPayload::handleOnServer);
        registrar.playToServer(RemoveTrustedPlayerPayload.TYPE, RemoveTrustedPlayerPayload.STREAM_CODEC,
                RemoveTrustedPlayerPayload::handleOnServer);
        registrar.playToClient(TrustResultPayload.TYPE, TrustResultPayload.STREAM_CODEC,
                TrustResultPayload::handleOnClient);
    }
}
