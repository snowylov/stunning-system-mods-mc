package com.alex.fluidworks.channel;

import com.alex.fluidworks.FluidWorks;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** A horizontal U-shaped channel that always extracts from its back and outputs at its front. */
public final class FluidChannelBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty FILLED = BooleanProperty.of("filled");
    public static final long CAPACITY = dev.liquidfabric.api.unofficial.core.FluidUnits.BUCKET_DROPLETS;
    public static final long TRANSFER_RATE = dev.liquidfabric.api.unofficial.core.FluidUnits.mbToDroplets(250);

    private static final VoxelShape NORTH_SOUTH = VoxelShapes.union(
        createCuboidShape(2, 2, 0, 14, 5, 16),
        createCuboidShape(2, 5, 0, 5, 12, 16),
        createCuboidShape(11, 5, 0, 14, 12, 16));
    private static final VoxelShape EAST_WEST = VoxelShapes.union(
        createCuboidShape(0, 2, 2, 16, 5, 14),
        createCuboidShape(0, 5, 2, 16, 12, 5),
        createCuboidShape(0, 5, 11, 16, 12, 14));

    private final ChannelMaterial material;
    private final MapCodec<FluidChannelBlock> codec = MapCodec.unit(this);

    public FluidChannelBlock(ChannelMaterial material, Settings settings) {
        super(settings);
        this.material = material;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(FILLED, false));
    }

    public ChannelMaterial material() {
        return material;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, FILLED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return rotate(state, mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        return state.get(FACING).getAxis() == Direction.Axis.Z ? NORTH_SOUTH : EAST_WEST;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidChannelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, FluidWorks.FLUID_CHANNEL_BLOCK_ENTITY,
            FluidChannelBlockEntity::serverTick);
    }
}
