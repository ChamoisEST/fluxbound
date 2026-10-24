package io.github.chamoisest.fluxbound.client.gui.arrivalanchor;

import io.github.chamoisest.fluxbound.client.gui.MachineScreen;
import io.github.chamoisest.fluxbound.client.gui.MachineStatus;
import io.github.chamoisest.fluxbound.client.gui.tab.PermissionsTab;
import io.github.chamoisest.fluxbound.client.gui.tab.TrustedPlayersTab;
import io.github.chamoisest.fluxbound.client.gui.widget.EnergyBar;
import io.github.chamoisest.fluxbound.machine.permission.MachineRights;
import io.github.chamoisest.fluxbound.menu.arrivalanchor.ArrivalAnchorMenu;
import io.github.chamoisest.fluxbound.network.menu.AddTrustedPlayerPayload;
import io.github.chamoisest.fluxbound.network.menu.MachinePermissionsPayload;
import io.github.chamoisest.fluxbound.network.menu.RemoveTrustedPlayerPayload;
import io.github.chamoisest.fluxbound.network.menu.SetAccessModePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class ArrivalAnchorScreen extends MachineScreen<ArrivalAnchorMenu>
{
    // Energy column left of the content area (M1 decision D1); the space below the bar is reserved for M3.
    private static final int ENERGY_BAR_X = 8;
    private static final int ENERGY_BAR_Y = 17;

    private static final int ONLINE_COLOR = 0xFF2E7D32;

    // Nothing can take an Anchor offline yet. Once redstone control (M5) or networks (M10) can, this
    // becomes server-computed state from the Overview payload.
    private static final MachineStatus ONLINE = new MachineStatus(
            Component.translatable("gui.fluxbound.status.online"), ONLINE_COLOR,
            Component.translatable("gui.fluxbound.status.online.help"));

    public ArrivalAnchorScreen(ArrivalAnchorMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);

        addTab(new ArrivalAnchorOverviewTab(menu));
        // Built from the same list the server uses, so the screen never offers a right the server rejects.
        List<PermissionsTab.Row> rights = MachineRights.ARRIVAL_ANCHOR.stream()
                .map(right -> new PermissionsTab.Row(right,
                        text("arrival_anchor.right." + right.name().toLowerCase(Locale.ROOT))))
                .toList();

        addTab(new PermissionsTab(font, menu::getPermissions, rights, this,
                (right, mode) -> send(id -> new SetAccessModePayload(menu.containerId, id, right, mode))));
        addTab(new TrustedPlayersTab(font, menu::getPermissions, menu::getTrustResult,
                () -> menu.getOverview() == null ? null : menu.getOverview().ownerName(), this,
                name -> send(id -> new AddTrustedPlayerPayload(menu.containerId, id, name)),
                storedId -> send(id -> new RemoveTrustedPlayerPayload(menu.containerId, id, storedId))));
    }

    @Override
    protected MachineStatus status()
    {
        return ONLINE;
    }

    // A widget rather than background drawing, so keyboard focus can reach its values (M1 B9); added last, so it
    // comes after the status in the Tab order.
    @Override
    protected void init()
    {
        super.init();
        addRenderableWidget(new EnergyBar(leftPos + ENERGY_BAR_X, topPos + ENERGY_BAR_Y, menu::getOverview));
    }

    // Names the machine from the latest Permissions sync, so the server can refuse the request if the menu is now
    // bound to a different machine (M1 D3).
    private void send(Function<String, CustomPacketPayload> request)
    {
        MachinePermissionsPayload permissions = menu.getPermissions();

        if (permissions != null)
        {
            ClientPacketDistributor.sendToServer(request.apply(permissions.machineId()));
        }
    }

    private static Component text(String key)
    {
        return Component.translatable("gui.fluxbound." + key);
    }
}
