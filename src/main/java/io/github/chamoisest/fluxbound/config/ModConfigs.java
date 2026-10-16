package io.github.chamoisest.fluxbound.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfigs
{
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue ARRIVAL_ANCHOR_CAPACITY;
    public static final ModConfigSpec.IntValue ARRIVAL_ANCHOR_INPUT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("arrivalAnchor");

        ARRIVAL_ANCHOR_CAPACITY = builder
                .comment("FE capacity per Arrival Anchor block.")
                .worldRestart()
                .defineInRange("capacityPerBlock", 100_000, 1, 20_000_000);

        ARRIVAL_ANCHOR_INPUT = builder
                .comment("Maximum FE input per tick per Arrival Anchor block.")
                .worldRestart()
                .defineInRange("inputPerTickPerBlock", 200, 0, 1_000_000);

        builder.pop();

        SPEC = builder.build();
    }

    private ModConfigs() {}
}
