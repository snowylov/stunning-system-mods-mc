package dev.liquidfabric.api.unofficial.tank.buildcraft;

import dev.liquidfabric.api.unofficial.helper.block.TankBlockHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BaseStackableTankBlock extends BlockWithEntity {
    public static final MapCodec<BaseStackableTankBlock> CODEC = createCodec(BaseStackableTankBlock::new);
    public static final BooleanProperty CONNECTED_UP = BooleanProperty.of("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.of("connected_down");

    public BaseStackableTankBlock(Settings settings) {
        super(settings.nonOpaque());
        setDefaultState(getDefaultState().with(CONNECTED_UP, false).with(CONNECTED_DOWN, false));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_UP, CONNECTED_DOWN);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BaseStackableTankBlockEntity(pos, state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return TankBlockHelper.withVerticalConnections(ctx.getWorld(), ctx.getBlockPos(), getDefaultState());
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        world.setBlockState(pos, TankBlockHelper.withVerticalConnections(world, pos, state), Block.NOTIFY_ALL);
    }


    @Override protected boolean hasComparatorOutput(BlockState state) { return true; }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof BaseStackableTankBlockEntity be) {
            return be.getComparatorOutput();
        }
        return 0;
    }
}
