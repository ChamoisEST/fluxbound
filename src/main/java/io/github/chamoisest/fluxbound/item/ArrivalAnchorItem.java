package io.github.chamoisest.fluxbound.item;

import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public class ArrivalAnchorItem extends BlockItem
{
    public ArrivalAnchorItem(Block block, Properties properties)
    {
        super(block, properties);
    }

    // A packed item only becomes a machine if its token can still be redeemed (M2 G2-1, spec §4). Checked before
    // vanilla places anything, so a refusal changes nothing in the world and the item stays as it is. The client
    // can't check the token and may show the block for a moment; the server sends block updates after every use
    // (ServerGamePacketListenerImpl.handleUseItemOn, 26.1.2), which remove it again.
    @Override
    public InteractionResult place(BlockPlaceContext context)
    {
        String token = context.getItemInHand().get(ModDataComponents.PACKED_MACHINE.get());

        if (token != null && context.getLevel() instanceof ServerLevel level
                && !MachineRegistryData.get(level).canRestorePacked(token))
        {
            if (context.getPlayer() != null)
            {
                context.getPlayer().sendOverlayMessage(Component.translatable("message.fluxbound.packed.invalid"));
            }

            return InteractionResult.FAIL;
        }

        return super.place(context);
    }
}
