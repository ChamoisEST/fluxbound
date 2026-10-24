package io.github.chamoisest.fluxbound.machine.energy;

import io.github.chamoisest.fluxbound.config.ModConfigs;

// The one place that turns a machine's size into its energy limits (E8). Capacity and input scale with the number of
// member blocks (E15: per physical member, regardless of faces). Machine types get their own values in M7.
public final class MachineEnergyRules
{
    private MachineEnergyRules() {}

    public static long capacity(int blockCount)
    {
        return (long) ModConfigs.ARRIVAL_ANCHOR_CAPACITY.get() * blockCount;
    }

    public static long inputPerTick(int blockCount)
    {
        return (long) ModConfigs.ARRIVAL_ANCHOR_INPUT.get() * blockCount;
    }
}
