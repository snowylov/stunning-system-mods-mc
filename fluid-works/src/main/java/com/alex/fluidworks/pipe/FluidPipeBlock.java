package com.alex.fluidworks.pipe;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.block.StackableTankBlock;
import com.alex.fluidworks.block.TankBlock;
import com.alex.fluidworks.channel.FluidChannelBlock;
import com.alex.fluidworks.device.FluidDeviceBlock;
import com.alex.fluidworks.machine.ContainerDispenserBlock;
import com.alex.fluidworks.reservoir.ReservoirControllerBlock;
import com.alex.fluidworks.reservoir.ReservoirValveBlock;
import com.alex.fluidworks.pump.FluidPumpBlock;
import com.alex.fluidworks.thermal.HeaterPipeBlock;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

public final class FluidPipeBlock extends BlockWithEntity {
    public static final BooleanProperty ENABLED = BooleanProperty.of("enabled");
    public static final EnumProperty<PipeRedstoneMode> REDSTONE_MODE =
        EnumProperty.of("redstone_mode", PipeRedstoneMode.class);
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");

    private static final VoxelShape CORE = createCuboidShape(5, 5, 5, 11, 11, 11);

    private final PipeKind kind;
    private final MapCodec<FluidPipeBlock> codec = MapCodec.unit(this);

    public FluidPipeBlock(PipeKind kind, Settings settings) {
        super(settings);
        this.kind = kind;
        setDefaultState(getDefaultState().with(ENABLED, true)
            .with(REDSTONE_MODE, kind.defaultRedstoneMode()).with(FACING, Direction.NORTH)
            .with(NORTH, false).with(SOUTH, false).with(WEST, false).with(EAST, false)
            .with(UP, false).with(DOWN, false));
    }

    public long transferRate() {
        return kind.transferRate();
    }

    public PipeRedstoneMode defaultMode() {
        return kind.defaultRedstoneMode();
    }

    public PipeKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ENABLED, REDSTONE_MODE, FACING, NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState state = getDefaultState().with(FACING, context.getSide());
        for (Direction side : Direction.values()) {
            state = state.with(property(side), canConnect(context.getWorld().getBlockState(
                context.getBlockPos().offset(side))));
        }
        return state;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world,
                                                    ScheduledTickView tickView, BlockPos pos,
                                                    Direction direction, BlockPos neighborPos,
                                                    BlockState neighborState, Random random) {
        return state.with(property(direction), canConnect(neighborState));
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        VoxelShape shape = CORE;
        if (state.get(NORTH)) shape = VoxelShapes.union(shape,
            createCuboidShape(6, 6, 0, 10, 10, 5), createCuboidShape(4, 4, 0, 12, 12, 2));
        if (state.get(SOUTH)) shape = VoxelShapes.union(shape,
            createCuboidShape(6, 6, 11, 10, 10, 16), createCuboidShape(4, 4, 14, 12, 12, 16));
        if (state.get(WEST)) shape = VoxelShapes.union(shape,
            createCuboidShape(0, 6, 6, 5, 10, 10), createCuboidShape(0, 4, 4, 2, 12, 12));
        if (state.get(EAST)) shape = VoxelShapes.union(shape,
            createCuboidShape(11, 6, 6, 16, 10, 10), createCuboidShape(14, 4, 4, 16, 12, 12));
        if (state.get(UP)) shape = VoxelShapes.union(shape,
            createCuboidShape(6, 11, 6, 10, 16, 10), createCuboidShape(4, 14, 4, 12, 16, 12));
        if (state.get(DOWN)) shape = VoxelShapes.union(shape,
            createCuboidShape(6, 0, 6, 10, 5, 10), createCuboidShape(4, 0, 4, 12, 2, 12));
        return shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient() ? null
            : validateTicker(type, FluidWorks.FLUID_PIPE_BLOCK_ENTITY, FluidPipeBlockEntity::serverTick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient()) {
            if (world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe) {
                if (kind == PipeKind.METER && !player.isSneaking()) {
                    player.sendMessage(Text.translatable("message.fluidworks.pipe.meter",
                        pipe.measuredRateMb()), true);
                    return ActionResult.SUCCESS;
                }
                if (kind == PipeKind.FILTER) {
                    if (player.isSneaking()) {
                        pipe.clearFilter();
                        player.sendMessage(Text.translatable("message.fluidworks.pipe.filter_cleared"), true);
                    } else {
                        player.sendMessage(pipe.filterName(), true);
                    }
                    return ActionResult.SUCCESS;
                }
            }
            if (player.isSneaking()) {
                PipeRedstoneMode next = state.get(REDSTONE_MODE).next();
                world.setBlockState(pos, state.with(REDSTONE_MODE, next), Block.NOTIFY_ALL);
                player.sendMessage(Text.translatable("message.fluidworks.pipe.mode", next.asString()), true);
            } else {
                boolean enabled = !state.get(ENABLED);
                world.setBlockState(pos, state.with(ENABLED, enabled), Block.NOTIFY_ALL);
                player.sendMessage(Text.translatable(enabled
                    ? "message.fluidworks.pipe.enabled" : "message.fluidworks.pipe.disabled"), true);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (kind != PipeKind.FILTER
            || !(world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)) {
            return ActionResult.PASS;
        }
        if (world.isClient()) return ActionResult.SUCCESS;
        if (pipe.setFilterFrom(stack)) {
            player.sendMessage(pipe.filterName(), true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        if (!(world.getBlockEntity(pos) instanceof FluidPipeBlockEntity pipe)) return 0;
        return kind == PipeKind.METER
            ? pipe.meterComparatorOutput(transferRate())
            : pipe.storage().comparatorOutput();
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static boolean canConnect(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FluidPipeBlock
            || block instanceof FluidChannelBlock
            || block instanceof TankBlock
            || block instanceof StackableTankBlock
            || block instanceof FluidDeviceBlock
            || block instanceof ContainerDispenserBlock
            || block instanceof ReservoirControllerBlock
            || block instanceof ReservoirValveBlock
            || block instanceof FluidPumpBlock
            || block instanceof HeaterPipeBlock;
    }
}
