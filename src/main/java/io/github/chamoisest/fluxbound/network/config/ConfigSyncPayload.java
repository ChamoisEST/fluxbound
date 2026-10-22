package io.github.chamoisest.fluxbound.network.config;

import io.github.chamoisest.fluxbound.Fluxbound;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

// Server config values that client-side displays need.
public record ConfigSyncPayload(int anchorCapacityPerBlock, int anchorInputPerTickPerBlock)
        implements CustomPacketPayload
{
    public static final Type<ConfigSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "config_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ConfigSyncPayload::anchorCapacityPerBlock,
                    ByteBufCodecs.VAR_INT, ConfigSyncPayload::anchorInputPerTickPerBlock,
                    ConfigSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
