package io.github.chamoisest.fluxbound.machine.permission;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MachinePermissions(AccessMode use, AccessMode arrive, AccessMode manage,
                                 AccessMode charge, List<String> trusted)
{
    public static final MachinePermissions DEFAULT = new MachinePermissions(
            AccessMode.OWNER, AccessMode.OWNER, AccessMode.OWNER, AccessMode.OWNER, List.of());

    public static final Codec<MachinePermissions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    AccessMode.CODEC.optionalFieldOf("use", AccessMode.OWNER).forGetter(MachinePermissions::use),
                    AccessMode.CODEC.optionalFieldOf("arrive", AccessMode.OWNER).forGetter(MachinePermissions::arrive),
                    AccessMode.CODEC.optionalFieldOf("manage", AccessMode.OWNER).forGetter(MachinePermissions::manage),
                    AccessMode.CODEC.optionalFieldOf("charge", AccessMode.OWNER).forGetter(MachinePermissions::charge),
                    Codec.STRING.listOf().optionalFieldOf("trusted", List.of()).forGetter(MachinePermissions::trusted)
            ).apply(instance, MachinePermissions::new));

    public MachinePermissions
    {
        trusted = trusted.stream().distinct().sorted().toList();
    }

    public AccessMode mode(MachineRight right)
    {
        return switch (right) {
            case USE -> use;
            case ARRIVE -> arrive;
            case MANAGE -> manage;
            case CHARGE -> charge;
        };
    }

    public MachinePermissions withMode(MachineRight right, AccessMode mode)
    {
        return switch (right) {
            case USE -> new MachinePermissions(mode, arrive, manage, charge, trusted);
            case ARRIVE -> new MachinePermissions(use, mode, manage, charge, trusted);
            case MANAGE -> new MachinePermissions(use, arrive, mode, charge, trusted);
            case CHARGE -> new MachinePermissions(use, arrive, manage, mode, trusted);
        };
    }

    // Equal in the given rights and in the trusted list. Rights a machine doesn't use are ignored.
    public boolean sameFor(List<MachineRight> rights, MachinePermissions other)
    {
        for (MachineRight right : rights)
        {
            if (mode(right) != other.mode(right))
            {
                return false;
            }
        }

        return trusted.equals(other.trusted);
    }

    public MachinePermissions withTrusted(UUID player, boolean enabled)
    {
        Set<String> updated = new HashSet<>(trusted);

        if (enabled) {
            updated.add(player.toString());
        } else {
            updated.remove(player.toString());
        }

        return new MachinePermissions(use, arrive, manage, charge, List.copyOf(updated));
    }
}
