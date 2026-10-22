package io.github.chamoisest.fluxbound.machine.permission;

import io.github.chamoisest.fluxbound.machine.data.MachineRecord;

import java.util.List;

// Which rights each machine type uses. A right a machine doesn't use can't be set on it and doesn't matter when
// machines are compared for joining (M1 B5-3).
public final class MachineRights
{
    // In display order.
    public static final List<MachineRight> ARRIVAL_ANCHOR =
            List.of(MachineRight.ARRIVE, MachineRight.MANAGE, MachineRight.CHARGE);

    private MachineRights() {}

    // Every machine record is an Arrival Anchor until records carry a machine type (M7).
    public static List<MachineRight> of(MachineRecord record)
    {
        return ARRIVAL_ANCHOR;
    }
}
