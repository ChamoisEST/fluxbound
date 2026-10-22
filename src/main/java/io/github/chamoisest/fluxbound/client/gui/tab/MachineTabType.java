package io.github.chamoisest.fluxbound.client.gui.tab;

import net.minecraft.network.chat.Component;

// Tab functions with fixed colours, shared by every machine screen, so a colour always means the same
// kind of section (M1 decision E1). New tab types get their colour when they are added.
public enum MachineTabType
{
    OVERVIEW("overview", 0x3A6EA5),
    PERMISSIONS("permissions", 0xB8733A),
    TRUSTED_PLAYERS("trusted_players", 0x7A4FA8);

    private final Component label;
    private final int color;

    MachineTabType(String key, int rgb)
    {
        this.label = Component.translatable("gui.fluxbound.tab." + key);
        this.color = 0xFF000000 | rgb;
    }

    public Component label()
    {
        return label;
    }

    // Opaque ARGB colour, used to tint the tab sprites.
    public int color()
    {
        return color;
    }
}
