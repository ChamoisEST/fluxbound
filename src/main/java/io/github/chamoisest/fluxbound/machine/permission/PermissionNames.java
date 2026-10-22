package io.github.chamoisest.fluxbound.machine.permission;

import net.minecraft.network.chat.Component;

import java.util.Locale;

// Display names of rights and access modes, shared by commands and machine screens. Translated by each client.
public final class PermissionNames
{
    private PermissionNames() {}

    public static Component right(MachineRight right)
    {
        return Component.translatable("gui.fluxbound.right." + right.name().toLowerCase(Locale.ROOT));
    }

    public static Component mode(AccessMode mode)
    {
        return Component.translatable("gui.fluxbound.access_mode." + mode.name().toLowerCase(Locale.ROOT));
    }
}
