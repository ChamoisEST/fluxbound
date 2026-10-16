
package io.github.chamoisest.fluxbound.machine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MachineRecord(String id, String dimension, long position, String owner, long energy)
{
    public static final Codec<MachineRecord> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(MachineRecord::id),
                    Codec.STRING.fieldOf("dimension").forGetter(MachineRecord::dimension),
                    Codec.LONG.fieldOf("position").forGetter(MachineRecord::position),
                    Codec.STRING.fieldOf("owner").forGetter(MachineRecord::owner),
                    Codec.LONG.fieldOf("energy").forGetter(MachineRecord::energy)
            ).apply(instance, MachineRecord::new));

    public MachineRecord withOwner(String owner)
    {
        return new MachineRecord(id, dimension, position, owner, energy);
    }

    public MachineRecord withEnergy(long energy)
    {
        return new MachineRecord(id, dimension, position, owner, energy);
    }
}

