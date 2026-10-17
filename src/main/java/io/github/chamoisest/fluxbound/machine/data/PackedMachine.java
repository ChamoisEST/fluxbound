package io.github.chamoisest.fluxbound.machine.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.chamoisest.fluxbound.machine.permission.MachinePermissions;

public record PackedMachine(String token, String machineId, String owner, long energy,
                            boolean autoJoin, MachinePermissions permissions)
{
    public static final Codec<PackedMachine> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("token").forGetter(PackedMachine::token),
                    Codec.STRING.fieldOf("machine_id").forGetter(PackedMachine::machineId),
                    Codec.STRING.fieldOf("owner").forGetter(PackedMachine::owner),
                    Codec.LONG.fieldOf("energy").forGetter(PackedMachine::energy),
                    Codec.BOOL.fieldOf("auto_join").forGetter(PackedMachine::autoJoin),
                    MachinePermissions.CODEC.optionalFieldOf("permissions", MachinePermissions.DEFAULT)
                            .forGetter(PackedMachine::permissions)
            ).apply(instance, PackedMachine::new));

    public boolean hasPersistentState()
    {
        return energy > 0 || !permissions.equals(MachinePermissions.DEFAULT);
    }
}

