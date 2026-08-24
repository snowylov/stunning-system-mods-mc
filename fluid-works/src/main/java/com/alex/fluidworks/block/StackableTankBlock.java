package com.alex.fluidworks.block;

import com.alex.fluidworks.fluid.FluidInteraction;
import com.mojang.serialization.MapCodec;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

/** BuildCraft-style vertical tank. Each block contributes eight buckets to one column storage. */
public final class StackableTankBlock extends BlockWithEntity {
    public static final BooleanProperty CONNECTED_UP = BooleanProperty.of("connected_up");
    public static final BooleanProperty CONNECTED_DOWN = BooleanProperty.of("connected_down");
    public static final long CAPACITY_PER_BLOCK = FluidUnits.bucketsToDroplets(8);
    private static final VoxelShape BODY_SHAPE = createCuboidShape(1, 0, 1, 15, 16, 15);
    private static final VoxelShape TOP_CAP_SHAPE = createCuboidShape(0, 14, 0, 16, 16, 16);
    private static final VoxelShape BOTTOM_CAP_SHAPE = createCuboidShape(0, 0, 0, 16, 2, 16);

    private final MapCodec<StackableTankBlock> codec = MapCodec.unit(this);

    public StackableTankBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(CONNECTED_UP, false).with(CONNECTED_DOWN, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CONNECTED_UP, CONNECTED_DOWN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return withConnections(getDefaultState(), context.getWorld(), context.getBlockPos());
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world,
                                                    ScheduledTickView tickView, BlockPos pos,
                                                    Direction direction, BlockPos neighborPos,
                                                    BlockState neighborState, Random random) {
        if (direction == Direction.UP) return state.with(CONNECTED_UP, neighborState.isOf(this));
        if (direction == Direction.DOWN) return state.with(CONNECTED_DOWN, neighborState.isOf(this));
        return state;
    }

    private BlockState withConnections(BlockState state, BlockView world, BlockPos pos) {
        return state.with(CONNECTED_UP, world.getBlockState(pos.up()).isOf(this))
            .with(CONNECTED_DOWN, world.getBlockState(pos.down()).isOf(this));
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        return shape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return shape(state);
    }

    private static VoxelShape shape(BlockState state) {
        VoxelShape result = BODY_SHAPE;
        if (!state.get(CONNECTED_UP)) result = net.minecraft.util.shape.VoxelShapes.union(result, TOP_CAP_SHAPE);
        if (!state.get(CONNECTED_DOWN)) result = net.minecraft.util.shape.VoxelShapes.union(result, BOTTOM_CAP_SHAPE);
        return result;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof TankBlockEntity tank)) return ActionResult.PASS;
        if (world.isClient()) {
            return FluidItemComponentHelper.capacity(stack) > 0 ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        return FluidInteraction.transfer((ServerWorld) world, pos, stack, tank.liquidFabricStorage());
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        if (!(world.getBlockEntity(pos) instanceof TankBlockEntity tank)) return 0;
        long amount = 0;
        long capacity = 0;
        for (var view : tank.liquidFabricStorage()) {
            amount += view.getAmount();
            capacity += view.getCapacity();
        }
        if (capacity <= 0 || amount <= 0) return 0;
        return Math.max(1, Math.min(15, (int) Math.floor((double) amount * 15.0D / capacity)));
    }
}
