package com.alex.fluidworks.reservoir;

import com.alex.fluidworks.FluidWorks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class ReservoirStructure {
    public static final int MIN_SIZE = 3;
    public static final int MAX_SIZE = 11;
    private static final int MAX_STRUCTURE_BLOCKS = 800;

    private ReservoirStructure() {
    }

    public static boolean validate(ReservoirControllerBlockEntity controller) {
        World world = controller.getWorld();
        if (world == null || world.isClient()) return controller.formed();

        Set<BlockPos> connected = collectConnected(world, controller.getPos());
        if (connected.isEmpty() || connected.size() > MAX_STRUCTURE_BLOCKS) {
            controller.applyStructure(false, null, null, 0, controller.storage().amountView());
            return false;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : connected) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        if (!validDimension(width) || !validDimension(height) || !validDimension(depth)) {
            controller.applyStructure(false, null, null, 0, controller.storage().amountView());
            return false;
        }

        ReservoirTier tier = controller.tier();
        int controllerCount = 0;
        BlockPos minimum = new BlockPos(minX, minY, minZ);
        BlockPos maximum = new BlockPos(maxX, maxY, maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos current = new BlockPos(x, y, z);
                    boolean boundary = x == minX || x == maxX || y == minY || y == maxY
                        || z == minZ || z == maxZ;
                    BlockState state = world.getBlockState(current);
                    if (boundary) {
                        if (!allowedForTier(state.getBlock(), tier)) {
                            controller.applyStructure(false, null, null, 0, controller.storage().amountView());
                            return false;
                        }
                        if (state.getBlock() instanceof ReservoirControllerBlock) controllerCount++;
                    } else if (!state.isAir()) {
                        controller.applyStructure(false, null, null, 0, controller.storage().amountView());
                        return false;
                    }
                }
            }
        }

        if (controllerCount != 1) {
            controller.applyStructure(false, null, null, 0, controller.storage().amountView());
            return false;
        }

        int interiorBlocks = (width - 2) * (height - 2) * (depth - 2);
        long capacity = interiorBlocks * tier.dropletsPerInteriorBlock();
        if (controller.storage().amountView() > capacity) {
            controller.applyStructure(false, null, null, 0, controller.storage().amountView());
            return false;
        }

        controller.applyStructure(true, minimum, maximum, interiorBlocks, capacity);
        linkValves(world, minimum, maximum, tier, controller.getPos());
        return true;
    }

    public static ReservoirControllerBlockEntity findController(World world, BlockPos start,
                                                                 ReservoirTier tier) {
        for (BlockPos pos : collectConnected(world, start)) {
            if (world.getBlockEntity(pos) instanceof ReservoirControllerBlockEntity controller
                && controller.tier() == tier && controller.rebuild()) {
                return controller;
            }
        }
        return null;
    }

    public static void revalidateNearby(ServerWorld world, BlockPos changedPos) {
        Set<ReservoirControllerBlockEntity> controllers = new HashSet<>();
        BlockPos.iterate(changedPos.add(-MAX_SIZE, -MAX_SIZE, -MAX_SIZE),
                changedPos.add(MAX_SIZE, MAX_SIZE, MAX_SIZE))
            .forEach(pos -> {
                BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity instanceof ReservoirControllerBlockEntity controller) {
                    controllers.add(controller);
                }
            });
        controllers.forEach(ReservoirControllerBlockEntity::rebuild);
    }

    private static Set<BlockPos> collectConnected(World world, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        if (!isStructural(world.getBlockState(start).getBlock())) return visited;
        queue.add(start.toImmutable());

        while (!queue.isEmpty() && visited.size() <= MAX_STRUCTURE_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) continue;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.offset(direction);
                if (!visited.contains(neighbor) && isStructural(world.getBlockState(neighbor).getBlock())) {
                    queue.addLast(neighbor.toImmutable());
                }
            }
        }
        return visited;
    }

    private static void linkValves(World world, BlockPos min, BlockPos max, ReservoirTier tier,
                                   BlockPos controllerPos) {
        BlockPos.iterate(min, max).forEach(pos -> {
            if (world.getBlockEntity(pos) instanceof ReservoirValveBlockEntity valve
                && valve.tier() == tier) {
                valve.link(controllerPos);
            }
        });
    }

    private static boolean validDimension(int size) {
        return size >= MIN_SIZE && size <= MAX_SIZE;
    }

    private static boolean isStructural(Block block) {
        return block == FluidWorks.RESERVOIR_WINDOW || block instanceof TieredReservoirPart;
    }

    private static boolean allowedForTier(Block block, ReservoirTier tier) {
        return block == FluidWorks.RESERVOIR_WINDOW
            || block instanceof TieredReservoirPart part && part.tier() == tier;
    }
}
