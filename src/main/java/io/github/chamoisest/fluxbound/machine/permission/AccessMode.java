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
}
