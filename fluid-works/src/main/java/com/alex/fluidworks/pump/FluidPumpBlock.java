package com.alex.fluidworks.pump;

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

/** Redstone-powered, transaction-safe pump. FACING is always the output side. */
public final class FluidPumpBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    private static final MapCodec<FluidPumpBlock> CODEC = createCodec(FluidPumpBlock::new);
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(
        createCuboidShape(3, 2, 3, 13, 14, 13),
        createCuboidShape(5, 5, 0, 11, 11, 3),
        createCuboidShape(3, 3, 0, 13, 13, 2),
        createCuboidShape(5, 5, 13, 11, 11, 16),
        createCuboidShape(3, 3, 14, 13, 13, 16),
        createCuboidShape(6, 14, 6, 10, 16, 10),
        createCuboidShape(2, 0, 4, 14, 2, 12));

    public FluidPumpBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getSide());
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
        return rotateFromNorth(NORTH_SHAPE, state.get(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPumpBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, FluidWorks.FLUID_PUMP_BLOCK_ENTITY,
            FluidPumpBlockEntity::serverTick);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof FluidPumpBlockEntity pump
            ? pump.comparatorOutput() : 0;
    }

    private static VoxelShape rotateFromNorth(VoxelShape north, Direction facing) {
        if (facing == Direction.NORTH) return north;
        VoxelShape[] result = {VoxelShapes.empty()};
        north.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double[][] corners = {{minX,minY,minZ},{minX,minY,maxZ},{minX,maxY,minZ},{minX,maxY,maxZ},
                {maxX,minY,minZ},{maxX,minY,maxZ},{maxX,maxY,minZ},{maxX,maxY,maxZ}};
            double outMinX=1,outMinY=1,outMinZ=1,outMaxX=0,outMaxY=0,outMaxZ=0;
            for (double[] corner : corners) {
                double[] transformed = transform(corner[0], corner[1], corner[2], facing);
                outMinX=Math.min(outMinX,transformed[0]); outMinY=Math.min(outMinY,transformed[1]);
                outMinZ=Math.min(outMinZ,transformed[2]); outMaxX=Math.max(outMaxX,transformed[0]);
                outMaxY=Math.max(outMaxY,transformed[1]); outMaxZ=Math.max(outMaxZ,transformed[2]);
            }
            result[0] = VoxelShapes.union(result[0], createCuboidShape(outMinX*16,outMinY*16,outMinZ*16,
                outMaxX*16,outMaxY*16,outMaxZ*16));
        });
        return result[0];
    }

    private static double[] transform(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[]{x,y,z};
            case EAST -> new double[]{1-z,y,x};
            case SOUTH -> new double[]{1-x,y,1-z};
            case WEST -> new double[]{z,y,1-x};
            case UP -> new double[]{x,z,1-y};
            case DOWN -> new double[]{x,1-z,y};
        };
    }
}
