package com.alex.fluidworks.pump;

import com.alex.fluidworks.FluidWorks;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Moves 250 mB/t directly from the rear storage into the front storage while powered. */
public final class FluidPumpBlockEntity extends BlockEntity {
    public static final long TRANSFER_RATE = FluidUnits.mbToDroplets(250);
    private long lastTransfer;

    public FluidPumpBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.FLUID_PUMP_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state,
                                  FluidPumpBlockEntity pump) {
        if (!(world instanceof ServerWorld serverWorld)
            || !serverWorld.isReceivingRedstonePower(pos)) {
            pump.setLastTransfer(0);
            return;
        }
        Direction output = state.get(FluidPumpBlock.FACING);
        Storage<FluidVariant> source = FluidStorage.SIDED.find(serverWorld,
            pos.offset(output.getOpposite()), output);
        Storage<FluidVariant> destination = FluidStorage.SIDED.find(serverWorld,
            pos.offset(output), output.getOpposite());
        if (source == null || destination == null) {
            pump.setLastTransfer(0);
            return;
        }
        long moved;
        try (Transaction transaction = Transaction.openOuter()) {
            moved = StorageUtil.move(source, destination, variant -> true, TRANSFER_RATE, transaction);
            if (moved > 0) transaction.commit();
        }
        pump.setLastTransfer(moved);
    }

    public int comparatorOutput() {
        return lastTransfer <= 0 ? 0 : Math.max(1, Math.min(15,
            (int)Math.ceil(lastTransfer * 15.0D / TRANSFER_RATE)));
    }

    private void setLastTransfer(long moved) {
        if (lastTransfer == moved) return;
        lastTransfer = moved;
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.updateComparators(pos, getCachedState().getBlock());
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        lastTransfer = Math.max(0, view.getLong("LastTransfer", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putLong("LastTransfer", lastTransfer);
    }
}
