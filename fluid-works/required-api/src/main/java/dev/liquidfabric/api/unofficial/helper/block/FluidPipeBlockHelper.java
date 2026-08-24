package dev.liquidfabric.api.unofficial.helper.block;

import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import dev.liquidfabric.api.unofficial.tank.pipe.fluid.BaseFluidPipeBlock;
import dev.liquidfabric.api.unofficial.tank.pipe.fluid.BaseFluidPipeBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shared pipe-network helpers for six-direction fluid pipe blocks.
 *
 * Compatibility/performance rules:
 * - no recursive network traversal
 * - no forced chunk loading
 * - neighbor changes are debounced through the pipe block entity
 * - connections only count loaded-neighbor storages/pipes
 */
public final class FluidPipeBlockHelper {
    private FluidPipeBlockHelper() {}

    private static final Map<Direction, BooleanProperty> DIRECTION_PROPERTIES = new EnumMap<>(Direction.class);

    static {
        DIRECTION_PROPERTIES.put(Direction.NORTH, BaseFluidPipeBlock.NORTH);
        DIRECTION_PROPERTIES.put(Direction.SOUTH, BaseFluidPipeBlock.SOUTH);
        DIRECTION_PROPERTIES.put(Direction.EAST, BaseFluidPipeBlock.EAST);
        DIRECTION_PROPERTIES.put(Direction.WEST, BaseFluidPipeBlock.WEST);
        DIRECTION_PROPERTIES.put(Direction.UP, BaseFluidPipeBlock.UP);
        DIRECTION_PROPERTIES.put(Direction.DOWN, BaseFluidPipeBlock.DOWN);
    }

    public static BlockState computeConnections(World world, BlockPos pos, BlockState state) {
        BlockState out = state;
        for (Direction direction : Direction.values()) {
            out = out.with(property(direction), connects(world, pos, direction));
        }
        return out;
    }

    public static BooleanProperty property(Direction direction) {
        return DIRECTION_PROPERTIES.get(direction);
    }

    public static boolean isLoaded(World world, BlockPos pos) {
        return world.isChunkLoaded(pos);
    }

    public static boolean connects(World world, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.offset(direction);
        if (!isLoaded(world, neighborPos)) return false;
        BlockState neighbor = world.getBlockState(neighborPos);
        return neighbor.isOf(ModTanksAndPipes.FLUID_PIPE) || world.getBlockEntity(neighborPos) != null;
    }

    public static void debounceInvalidateAround(World world, BlockPos pos) {
        markPipeDirty(world, pos);
        for (Direction direction : Direction.values()) {
            markPipeDirty(world, pos.offset(direction));
        }
    }

    private static void markPipeDirty(World world, BlockPos pos) {
        if (!isLoaded(world, pos)) return;
        if (world.getBlockEntity(pos) instanceof BaseFluidPipeBlockEntity pipe) {
            pipe.scheduleConnectionRefresh();
        }
    }

    public static int connectionCount(BlockState state) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            BooleanProperty property = property(direction);
            if (property != null && state.contains(property) && Boolean.TRUE.equals(state.get(property))) count++;
        }
        return count;
    }

    public static boolean isStraight(BlockState state) {
        return connectionCount(state) == 2
                && ((isConnected(state, Direction.NORTH) && isConnected(state, Direction.SOUTH))
                || (isConnected(state, Direction.EAST) && isConnected(state, Direction.WEST))
                || (isConnected(state, Direction.UP) && isConnected(state, Direction.DOWN)));
    }

    public static boolean isConnected(BlockState state, Direction direction) {
        BooleanProperty property = property(direction);
        return property != null && state.contains(property) && Boolean.TRUE.equals(state.get(property));
    }
}
