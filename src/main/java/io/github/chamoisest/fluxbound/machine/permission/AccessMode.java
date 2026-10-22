package io.github.chamoisest.fluxbound.machine.permission;

import com.mojang.serialization.Codec;

public enum AccessMode
{
    OWNER,
    TRUSTED,
    EVERYONE,
    TEAM;

    public static final Codec<AccessMode> CODEC =
            Codec.STRING.xmap(AccessMode::valueOf, AccessMode::name);

    // Whether players can choose this mode. Team needs the FTB Teams integration (M9); stored Team values stay as
    // they are (M1 B5-2).
    public boolean selectable()
    {
        return this != TEAM;
    }
}
