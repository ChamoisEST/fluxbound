package io.github.chamoisest.fluxbound.client.gui;

import net.minecraft.network.chat.Component;

// Short machine state shown at the top right of a machine screen, with an explanation on hover.
public record MachineStatus(Component text, int color, Component help)
{
}
