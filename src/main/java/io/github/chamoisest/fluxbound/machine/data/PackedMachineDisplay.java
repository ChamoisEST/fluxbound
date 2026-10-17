package io.github.chamoisest.fluxbound.machine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PackedMachineDisplay(long energy, long capacity, long inputPerTick)
{
    public static final Codec<PackedMachineDisplay> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("energy").forGetter(PackedMachineDisplay::energy),
                    Codec.LONG.fieldOf("capacity").forGetter(PackedMachineDisplay::capacity),
                    Codec.LONG.fieldOf("input_per_tick").forGetter(PackedMachineDisplay::inputPerTick)
            ).apply(instance, PackedMachineDisplay::new));
}
