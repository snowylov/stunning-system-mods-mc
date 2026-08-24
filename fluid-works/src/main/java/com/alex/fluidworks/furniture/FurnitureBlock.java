package com.alex.fluidworks.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.util.math.random.Random;
import net.minecraft.block.ShapeContext;

/** Directional JSON-model furniture with collision matching its visible cuboids. */
public final class FurnitureBlock extends Block {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LEFT = BooleanProperty.of("left");
    public static final BooleanProperty RIGHT = BooleanProperty.of("right");

    private final FurnitureKind kind;
    private final MapCodec<FurnitureBlock> codec = MapCodec.unit(this);

    public FurnitureBlock(FurnitureKind kind, Settings settings) {
        super(settings);
        this.kind = kind;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH)
            .with(LEFT, false).with(RIGHT, false));
    }

    public FurnitureKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEFT, RIGHT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction facing = context.getHorizontalPlayerFacing().getOpposite();
        BlockState state = getDefaultState().with(FACING, facing);
        if (kind != FurnitureKind.CHAIR) return state;
        BlockPos pos = context.getBlockPos();
        if (runLength(context.getWorld(), pos, facing) > 4) return null;
        return withChairConnections(state, context.getWorld(), pos);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world,
                                                    ScheduledTickView tickView, BlockPos pos,
                                                    Direction direction, BlockPos neighborPos,
                                                    BlockState neighborState, Random random) {
        return kind == FurnitureKind.CHAIR ? withChairConnections(state, world, pos) : state;
    }

    private BlockState withChairConnections(BlockState state, BlockView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        return state.with(LEFT, matches(world.getBlockState(pos.offset(left)), facing))
            .with(RIGHT, matches(world.getBlockState(pos.offset(right)), facing));
    }

    private boolean matches(BlockState state, Direction facing) {
        return state.isOf(this) && state.get(FACING) == facing;
    }

    private int runLength(BlockView world, BlockPos origin, Direction facing) {
        int total = 1;
        for (Direction side : new Direction[]{facing.rotateYCounterclockwise(), facing.rotateYClockwise()}) {
            BlockPos cursor = origin.offset(side);
            while (total <= 4 && matches(world.getBlockState(cursor), facing)) {
                total++;
                cursor = cursor.offset(side);
            }
        }
        return total;
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
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        return shape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return shape(state);
    }

    private VoxelShape shape(BlockState state) {
        VoxelShape north = switch (kind) {
            case CHAIR -> VoxelShapes.union(
                createCuboidShape(2, 7, 2, 14, 10, 14),
                createCuboidShape(2, 0, 2, 5, 7, 5),
                createCuboidShape(11, 0, 2, 14, 7, 5),
                createCuboidShape(2, 0, 11, 5, 7, 14),
                createCuboidShape(11, 0, 11, 14, 7, 14),
                createCuboidShape(2, 10, 12, 14, 16, 15));
            case FOUR_LEGGED_TABLE -> tableTopAndLegs(true);
            case ONE_LEGGED_TABLE -> tableTopAndLegs(false);
        };
        return rotateHorizontal(north, state.get(FACING));
    }

    private static VoxelShape tableTopAndLegs(boolean fourLegged) {
        VoxelShape top = createCuboidShape(0, 12, 0, 16, 16, 16);
        if (!fourLegged) {
            return VoxelShapes.union(top,
                createCuboidShape(6, 1, 6, 10, 12, 10),
                createCuboidShape(3, 0, 3, 13, 2, 13));
        }
        return VoxelShapes.union(top,
            createCuboidShape(1, 0, 1, 4, 12, 4),
            createCuboidShape(12, 0, 1, 15, 12, 4),
            createCuboidShape(1, 0, 12, 4, 12, 15),
            createCuboidShape(12, 0, 12, 15, 12, 15));
    }

    private static VoxelShape rotateHorizontal(VoxelShape shape, Direction facing) {
        if (facing == Direction.NORTH) return shape;
        VoxelShape[] buffer = {shape, VoxelShapes.empty()};
        int turns = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; i++) {
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) ->
                buffer[1] = VoxelShapes.union(buffer[1],
                    createCuboidShape(16 - maxZ * 16, minY * 16, minX * 16,
                        16 - minZ * 16, maxY * 16, maxX * 16)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }
        return buffer[0];
    }
}
