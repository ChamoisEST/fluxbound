package io.github.chamoisest.fluxbound.network.menu;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.permission.TrustChange;
import io.github.chamoisest.fluxbound.menu.arrivalanchor.ArrivalAnchorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// The answer to the viewer's last trust request, shown in the Trusted Players tab (M1 B6-5). The list itself
// still arrives through the Permissions sync.
public record TrustResultPayload(int containerId, TrustChange result, String name) implements CustomPacketPayload
{
    public static final Type<TrustResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "trust_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrustResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TrustResultPayload::containerId,
                    NeoForgeStreamCodecs.enumCodec(TrustChange.class), TrustResultPayload::result,
                    ByteBufCodecs.STRING_UTF8, TrustResultPayload::name,
                    TrustResultPayload::new
            );

    // Uses only common classes, so it can be registered in common code.
    public static void handleOnClient(TrustResultPayload payload, IPayloadContext context)
    {
        if (context.player().containerMenu instanceof ArrivalAnchorMenu menu
                && menu.containerId == payload.containerId())
        {
            menu.setTrustResult(payload);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
