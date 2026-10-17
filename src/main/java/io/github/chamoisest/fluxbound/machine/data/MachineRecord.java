
package io.github.chamoisest.fluxbound.machine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import io.github.chamoisest.fluxbound.machine.permission.MachinePermissions;

public record MachineRecord(String id, String dimension, List<Long> members, String owner,
                            long energy, boolean autoJoin, MachinePermissions permissions)
{
    public static final Codec<MachineRecord> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(MachineRecord::id),
                    Codec.STRING.fieldOf("dimension").forGetter(MachineRecord::dimension),
                    Codec.LONG.optionalFieldOf("position").forGetter(record -> Optional.empty()),
                    Codec.LONG.listOf().optionalFieldOf("members", List.of()).forGetter(MachineRecord::members),
                    Codec.STRING.fieldOf("owner").forGetter(MachineRecord::owner),
                    Codec.LONG.fieldOf("energy").forGetter(MachineRecord::energy),
                    Codec.BOOL.optionalFieldOf("auto_join", true).forGetter(MachineRecord::autoJoin),
                    MachinePermissions.CODEC.optionalFieldOf("permissions", MachinePermissions.DEFAULT)
                            .forGetter(MachineRecord::permissions)
            ).apply(instance, MachineRecord::fromSavedData));

    public MachineRecord(String id, String dimension, long position, String owner, long energy)
    {
        this(id, dimension, List.of(position), owner, energy, true, MachinePermissions.DEFAULT);
    }

    public MachineRecord(String id, String dimension, List<Long> members, String owner, long energy, boolean autoJoin)
    {
        this(id, dimension, members, owner, energy, autoJoin, MachinePermissions.DEFAULT);
    }

    private static MachineRecord fromSavedData(String id, String dimension, Optional<Long> legacyPosition,
                                               List<Long> members, String owner, long energy, boolean autoJoin, MachinePermissions permissions)
    {
        if (members.isEmpty() && legacyPosition.isPresent()) {
            members = List.of(legacyPosition.get());
        }

        return new MachineRecord(id, dimension, members, owner, energy, autoJoin, permissions);
    }

    public boolean contains(String dimension, long position)
    {
        return this.dimension.equals(dimension) && members.contains(position);
    }

    public int blockCount()
    {
        return members.size();
    }

    public MachineRecord withMembers(List<Long> members)
    {
        return new MachineRecord(id, dimension, members, owner, energy, autoJoin, permissions);
    }

    public MachineRecord withOwner(String owner)
    {
        return new MachineRecord(id, dimension, members, owner, energy, autoJoin, permissions);
    }

    public MachineRecord withEnergy(long energy)
    {
        return new MachineRecord(id, dimension, members, owner, energy, autoJoin, permissions);
    }

    public MachineRecord withAutoJoin(boolean autoJoin)
    {
        return new MachineRecord(id, dimension, members, owner, energy, autoJoin, permissions);
    }

    public MachineRecord withPermissions(MachinePermissions permissions)
    {
        return new MachineRecord(id, dimension, members, owner, energy, autoJoin, permissions);
    }
}
