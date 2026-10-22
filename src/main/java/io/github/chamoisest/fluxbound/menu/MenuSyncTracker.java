package io.github.chamoisest.fluxbound.menu;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Sends a menu's sync sections to its viewer, each one only when its content has changed.
// Sections are value records, so equals() detects changes without per-field bookkeeping.
public final class MenuSyncTracker
{
    private final List<Section> sections = new ArrayList<>();

    // A snapshot returns null when the section currently has nothing valid to send.
    public void add(Supplier<? extends CustomPacketPayload> snapshot)
    {
        sections.add(new Section(snapshot));
    }

    public void sendChanges(ServerPlayer viewer)
    {
        for (Section section : sections)
        {
            section.send(viewer, false);
        }
    }

    public void sendAll(ServerPlayer viewer)
    {
        for (Section section : sections)
        {
            section.send(viewer, true);
        }
    }

    private static final class Section
    {
        private final Supplier<? extends CustomPacketPayload> snapshot;
        private CustomPacketPayload lastSent;

        private Section(Supplier<? extends CustomPacketPayload> snapshot)
        {
            this.snapshot = snapshot;
        }

        private void send(ServerPlayer viewer, boolean force)
        {
            CustomPacketPayload current = snapshot.get();

            if (current == null || (!force && current.equals(lastSent)))
            {
                return;
            }

            PacketDistributor.sendToPlayer(viewer, current);
            lastSent = current;
        }
    }
}
