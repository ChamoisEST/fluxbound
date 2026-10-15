
package io.github.chamoisest.fluxbound.item;

import guideme.GuidesCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class TeleportationGuideItem extends Item {

    private static final Identifier GUIDE_ID =
            Identifier.parse("fluxbound:guide");

    public TeleportationGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide()) {
            GuidesCommon.openGuide(player, GUIDE_ID);
        }

        return InteractionResult.SUCCESS;
    }
}
