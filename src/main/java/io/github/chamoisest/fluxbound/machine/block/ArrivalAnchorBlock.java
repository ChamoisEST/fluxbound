
package io.github.chamoisest.fluxbound.machine.block;

import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ArrivalAnchorBlock extends Block implements EntityBlock
{
    public ArrivalAnchorBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ArrivalAnchorBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level instanceof ServerLevel serverLevel
                && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof ArrivalAnchorBlockEntity anchor) {
            anchor.initializeMachine();

            if (anchor.getMachineId() != null) {
                io.github.chamoisest.fluxbound.machine.data.MachineRegistryData.get(serverLevel)
                        .assignOwner(anchor.getMachineId(), player.getUUID().toString());
            }
        }
    }
}
