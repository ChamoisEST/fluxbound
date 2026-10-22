package io.github.chamoisest.fluxbound.network.menu;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.permission.AccessMode;
import io.github.chamoisest.fluxbound.machine.permission.MachineRight;
import io.github.chamoisest.fluxbound.menu.arrivalanchor.ArrivalAnchorMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.Optional;

// Permission settings of a machine, filled only for viewers with Manage (spec §7: private data never reaches
// other clients). Without Manage the content is empty rather than the payload missing: an empty snapshot
// differs from the last one sent, so a viewer who loses Manage also loses the data they saw before.
public record MachinePermissionsPayload(int containerId, String machineId, Optional<Content> content)
        implements CustomPacketPayload
{
    public static final Type<MachinePermissionsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Fluxbound.MODID, "machine_permissions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachinePermissionsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MachinePermissionsPayload::containerId,
                    ByteBufCodecs.STRING_UTF8, MachinePermissionsPayload::machineId,
                    ByteBufCodecs.optional(Content.STREAM_CODEC), MachinePermissionsPayload::content,
                    MachinePermissionsPayload::new
            );

    // Uses only common classes, so it can be registered in common code.
    public static void handleOnClient(MachinePermissionsPayload payload, IPayloadContext context)
    {
        if (context.player().containerMenu instanceof ArrivalAnchorMenu menu
                && menu.containerId == payload.containerId())
        {
            menu.setPermissions(payload);
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    // All four modes regardless of machine type; each screen shows only the rights its machine uses.
    // viewerIsOwner lets the client warn before a change would remove the viewer's own Manage right.
    public record Content(boolean viewerIsOwner, AccessMode use, AccessMode arrive, AccessMode manage,
                          AccessMode charge, List<TrustedPlayer> trusted)
    {
        private static final StreamCodec<FriendlyByteBuf, AccessMode> MODE_CODEC =
                NeoForgeStreamCodecs.enumCodec(AccessMode.class);

        public static final StreamCodec<FriendlyByteBuf, Content> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, Content::viewerIsOwner,
                MODE_CODEC, Content::use,
                MODE_CODEC, Content::arrive,
                MODE_CODEC, Content::manage,
                MODE_CODEC, Content::charge,
                TrustedPlayer.STREAM_CODEC.apply(ByteBufCodecs.list()), Content::trusted,
                Content::new
        );

        public AccessMode mode(MachineRight right)
        {
            return switch (right)
            {
                case USE -> use;
                case ARRIVE -> arrive;
                case MANAGE -> manage;
                case CHARGE -> charge;
            };
        }
    }

    // A trusted entry as stored (normally a UUID) and the server's cached name for it, if it has one.
    public record TrustedPlayer(String id, Optional<String> name)
    {
        public static final StreamCodec<ByteBuf, TrustedPlayer> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, TrustedPlayer::id,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), TrustedPlayer::name,
                TrustedPlayer::new
        );
    }
}
