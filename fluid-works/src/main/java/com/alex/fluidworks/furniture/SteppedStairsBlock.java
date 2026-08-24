package com.alex.fluidworks.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** A vanilla-behaving stair whose visible, collision, outline and raycast shape has 4-16 rises. */
public class SteppedStairsBlock extends StairsBlock {
    private static final Map<Integer, VoxelShape[][][]> SHAPE_CACHE = new ConcurrentHashMap<>();

    private final int steps;
    private final MapCodec<StairsBlock> codec = MapCodec.unit(this);

    public SteppedStairsBlock(BlockState baseBlockState, int steps, Settings settings) {
        super(baseBlockState, settings);
        if (steps < 2 || steps > 16) throw new IllegalArgumentException("Steps must be between 2 and 16");
        this.steps = steps;
    }

    public int steps() {
        return steps;
    }

    @Override
    public MapCodec<? extends StairsBlock> getCodec() {
        return codec;
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
        VoxelShape[][][] shapes = SHAPE_CACHE.computeIfAbsent(steps, SteppedStairsBlock::createShapes);
        return shapes[state.get(SHAPE).ordinal()][horizontalIndex(state.get(FACING))]
            [state.get(HALF) == BlockHalf.TOP ? 1 : 0];
    }

    private static VoxelShape[][][] createShapes(int steps) {
        VoxelShape[][][] result = new VoxelShape[StairShape.values().length][4][2];
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (StairShape stairShape : StairShape.values()) {
            for (Direction direction : directions) {
                result[stairShape.ordinal()][horizontalIndex(direction)][0] =
                    buildShape(stairShape, direction, false, steps);
                result[stairShape.ordinal()][horizontalIndex(direction)][1] =
                    buildShape(stairShape, direction, true, steps);
            }
        }
        return result;
    }

    private static VoxelShape buildShape(StairShape stairShape, Direction facing, boolean top,
                                         int steps) {
        VoxelShape result = VoxelShapes.empty();
        Direction cornerDirection = switch (stairShape) {
            case INNER_LEFT, OUTER_LEFT -> leftOf(facing);
            case INNER_RIGHT, OUTER_RIGHT -> rightOf(facing);
            case STRAIGHT -> facing;
        };
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int primary = steppedRise(facing, x, z, steps);
                int corner = steppedRise(cornerDirection, x, z, steps);
                int height = switch (stairShape) {
                    case STRAIGHT -> primary;
                    case INNER_LEFT, INNER_RIGHT -> Math.max(primary, corner);
                    case OUTER_LEFT, OUTER_RIGHT -> Math.min(primary, corner);
                };
                result = VoxelShapes.union(result, createCuboidShape(x, top ? 16 - height : 0, z,
                    x + 1, top ? 16 : height, z + 1));
            }
        }
        return result;
    }

    private static int steppedRise(Direction direction, int x, int z, int steps) {
        int distance = switch (direction) {
            case NORTH -> 15 - z;
            case SOUTH -> z;
            case WEST -> 15 - x;
            case EAST -> x;
            default -> throw new IllegalArgumentException("Stepped stairs require a horizontal facing");
        };
        int level = Math.min(steps, distance * steps / 16 + 1);
        return (int) Math.ceil(level * 16.0D / steps);
    }

    private static Direction leftOf(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            default -> direction;
        };
    }

    private static Direction rightOf(Direction direction) {
        return switch (direction) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> direction;
        };
    }

    private static int horizontalIndex(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException("Expected horizontal direction");
        };
    }
}
