package dev.liquidfabric.api.unofficial.helper.block;

import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import dev.liquidfabric.api.unofficial.tank.buildcraft.BaseStackableTankBlock;
import dev.liquidfabric.api.unofficial.tank.buildcraft.BaseStackableTankBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * BuildCraft-style vertical tank helpers.
 */
public final class TankBlockHelper {
    private TankBlockHelper() {}

    public static BlockState withVerticalConnections(World world, BlockPos pos, BlockState state) {
        return state.with(BaseStackableTankBlock.CONNECTED_UP, isTank(world, pos.up()))
                .with(BaseStackableTankBlock.CONNECTED_DOWN, isTank(world, pos.down()));
    }

    public static boolean isTank(World world, BlockPos pos) {
        return world.getBlockState(pos).isOf(ModTanksAndPipes.STACKABLE_TANK);
    }

    public static List<BlockPos> scanVerticalTankColumn(World world, BlockPos origin, int maxBlocks) {
        int limit = Math.max(1, Math.min(maxBlocks, BaseStackableTankBlockEntity.SAFE_VERTICAL_SCAN_LIMIT));
        List<BlockPos> positions = new ArrayList<>();
        positions.add(origin.toImmutable());

        for (int i = 1; i <= limit; i++) {
            BlockPos pos = origin.up(i);
            if (!isTank(world, pos)) break;
            positions.add(pos.toImmutable());
        }

        for (int i = 1; i <= limit; i++) {
            BlockPos pos = origin.down(i);
            if (!isTank(world, pos)) break;
            positions.add(pos.toImmutable());
        }

        return positions;
    }

    public static long totalCapacity(World world, BlockPos origin, long perBlockCapacity) {
        return scanVerticalTankColumn(world, origin, BaseStackableTankBlockEntity.SAFE_VERTICAL_SCAN_LIMIT).size() * perBlockCapacity;
    }

    public static int combinedComparator(World world, BlockPos origin) {
        long amount = 0;
        long capacity = 0;
        for (BlockPos pos : scanVerticalTankColumn(world, origin, BaseStackableTankBlockEntity.SAFE_VERTICAL_SCAN_LIMIT)) {
            if (world.getBlockEntity(pos) instanceof BaseStackableTankBlockEntity tank) {
                amount += tank.fluidStorage.amountView();
                capacity += tank.fluidStorage.getCapacityView();
            }
        }
        if (capacity <= 0 || amount <= 0) return 0;
        return Math.max(1, Math.min(15, (int) Math.floor((double) amount * 15.0D / (double) capacity)));
    }

    public static void refreshColumnConnections(World world, BlockPos origin) {
        for (BlockPos pos : scanVerticalTankColumn(world, origin, BaseStackableTankBlockEntity.SAFE_VERTICAL_SCAN_LIMIT)) {
            BlockState state = world.getBlockState(pos);
            if (state.isOf(ModTanksAndPipes.STACKABLE_TANK)) {
                world.setBlockState(pos, withVerticalConnections(world, pos, state), 3);
            }
        }
    }
}
