package dev.liquidfabric.api.unofficial.tank.pipe.fluid;

import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import dev.liquidfabric.api.unofficial.helper.block.FluidPipeBlockHelper;
import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import dev.liquidfabric.api.unofficial.tank.common.UtilityFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Fluid pipe block entity.
 *
 * Compatibility/performance pass:
 * - no large network scan every tick
 * - no recursive traversal
 * - debounced connection refresh
 * - skips unloaded neighbor chunks instead of forcing them loaded
 * - uses one transaction per neighbor operation and commits only exact transfers
 */
public class BaseFluidPipeBlockEntity extends BlockEntity implements dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity {
    public final UtilityFluidStorage buffer = new UtilityFluidStorage(this, LiquidFabricConfig.fluidPipeTransferRateDropletsPerTick * 4);

    @Override public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> liquidFabricStorage() { return buffer; }

    private long networkVersion;
    private boolean connectionRefreshQueued = true;
    private long nextTransferTick;

    public BaseFluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModTanksAndPipes.FLUID_PIPE_BE, pos, state);
    }

    public void invalidateNetworkCache() {
        scheduleConnectionRefresh();
    }

    public void scheduleConnectionRefresh() {
        networkVersion++;
        connectionRefreshQueued = true;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, BaseFluidPipeBlockEntity pipe) {
        long time = world.getTime();

        if (pipe.connectionRefreshQueued) {
            pipe.connectionRefreshQueued = false;
            BlockState updated = BaseFluidPipeBlock.compute(world, pos, state);
            if (updated != state) {
                world.setBlockState(pos, updated, Block.NOTIFY_LISTENERS);
            }
        }

        // Pipes operate in small pulses. This avoids every pipe in a large build
        // scanning all sides every single tick while still keeping transfer smooth.
        if (time < pipe.nextTransferTick) return;
        pipe.nextTransferTick = time + Math.max(1, LiquidFabricConfig.fluidPipeTransferIntervalTicks);

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            if (!FluidPipeBlockHelper.isLoaded(world, neighborPos)) continue;

            var storage = FluidStorage.SIDED.find(world, neighborPos, direction.getOpposite());
            if (storage == null) continue;

            if (tryPush(pipe, storage)) continue;
            tryPull(pipe, storage);
        }
    }

    private static boolean tryPush(BaseFluidPipeBlockEntity pipe, net.fabricmc.fabric.api.transfer.v1.storage.Storage<FluidVariant> target) {
        if (pipe.buffer.variant == null || pipe.buffer.variant.isBlank()) return false;

        try (Transaction tx = Transaction.openOuter()) {
            long extracted = pipe.buffer.extract(pipe.buffer.variant, LiquidFabricConfig.fluidPipeTransferRateDropletsPerTick, tx);
            if (extracted <= 0) return false;

            long inserted = target.insert(pipe.buffer.variant, extracted, tx);
            if (inserted == extracted) {
                tx.commit();
                return true;
            }
            // No commit: extraction and partial insertion both roll back.
            return false;
        }
    }

    private static boolean tryPull(BaseFluidPipeBlockEntity pipe, net.fabricmc.fabric.api.transfer.v1.storage.Storage<FluidVariant> source) {
        try (Transaction tx = Transaction.openOuter()) {
            for (var view : source) {
                FluidVariant variant = view.getResource();
                if (variant.isBlank() || view.getAmount() <= 0) continue;

                long extracted = source.extract(variant, LiquidFabricConfig.fluidPipeTransferRateDropletsPerTick, tx);
                if (extracted <= 0) return false;

                long accepted = pipe.buffer.insert(variant, extracted, tx);
                if (accepted == extracted) {
                    tx.commit();
                    return true;
                }
                // No commit: source extraction and buffer insert roll back.
                return false;
            }
        }
        return false;
    }
}
