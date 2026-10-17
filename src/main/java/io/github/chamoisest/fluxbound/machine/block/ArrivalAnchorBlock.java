
package io.github.chamoisest.fluxbound.machine.block;

import io.github.chamoisest.fluxbound.Fluxbound;
import io.github.chamoisest.fluxbound.machine.blockentity.ArrivalAnchorBlockEntity;
import io.github.chamoisest.fluxbound.machine.data.MachineRegistryData;
import io.github.chamoisest.fluxbound.registry.ModDataComponents;
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
import io.github.chamoisest.fluxbound.machine.MachinePackedItems;
import io.github.chamoisest.fluxbound.machine.data.PackedMachine;
import io.github.chamoisest.fluxbound.registry.ModItems;
import net.minecraft.world.level.Explosion;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class ArrivalAnchorBlock extends Block implements EntityBlock
{

    public static final BooleanProperty NORTH_CONNECTED = BooleanProperty.create("north_connected");
    public static final BooleanProperty EAST_CONNECTED = BooleanProperty.create("east_connected");
    public static final BooleanProperty SOUTH_CONNECTED = BooleanProperty.create("south_connected");
    public static final BooleanProperty WEST_CONNECTED = BooleanProperty.create("west_connected");


    public ArrivalAnchorBlock(Properties properties)
    {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(NORTH_CONNECTED, false)
                .setValue(EAST_CONNECTED, false)
                .setValue(SOUTH_CONNECTED, false)
                .setValue(WEST_CONNECTED, false));
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

        if (!(level instanceof ServerLevel serverLevel)
                || !(placer instanceof Player player)
                || !(level.getBlockEntity(pos) instanceof ArrivalAnchorBlockEntity anchor)) {
            return;
        }

        MachineRegistryData registry = MachineRegistryData.get(serverLevel);
        anchor.initializeMachine();

        String token = stack.get(ModDataComponents.PACKED_MACHINE.get());

        if (token != null && !registry.restorePacked(token, serverLevel, pos, anchor)) {
            Fluxbound.LOGGER.warn("Invalid or already redeemed packed machine token: {}", token);
        }

        if (anchor.getMachineId() == null) {
            return;
        }

        registry.assignOwner(anchor.getMachineId(), player.getUUID().toString());

        if (player.isShiftKeyDown()) {
            registry.setAutoJoin(anchor.getMachineId(), false);
        } else {
            registry.tryMergeAt(serverLevel, pos);
        }
    }


    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos,
                                  Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit)
    {
        PackedMachine packed = MachineRegistryData.get(level).previewPackedForRemoval(level, pos);
        boolean[] attached = {false};

        super.onExplosionHit(state, level, pos, explosion, (stack, dropPos) -> {
            if (!attached[0] && packed != null && stack.is(ModItems.ARRIVAL_ANCHOR.get())
                    && !stack.has(ModDataComponents.PACKED_MACHINE.get())) {
                MachinePackedItems.attach(level, stack, packed);
                attached[0] = true;
            }

            onHit.accept(stack, dropPos);
        });
    }


    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(NORTH_CONNECTED, EAST_CONNECTED, SOUTH_CONNECTED, WEST_CONNECTED);
    }
}
