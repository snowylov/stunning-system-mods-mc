package dev.liquidfabric.api.unofficial.tank.drum;

import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import dev.liquidfabric.api.unofficial.tank.common.UtilityFluidStorage;
import dev.liquidfabric.api.unofficial.helper.block.FluidStorageBlockHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class FluidDrumBlockEntity extends BlockEntity implements dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity {
    public final UtilityFluidStorage fluidStorage;
    private final DrumMode mode;
    private boolean locked;

    public FluidDrumBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, state.getBlock() instanceof FluidDrumBlock drum ? drum.capacity() : 81000L, state.getBlock() instanceof FluidDrumBlock drum ? drum.mode() : DrumMode.NORMAL);
    }

    public FluidDrumBlockEntity(BlockPos pos, BlockState state, long capacity, DrumMode mode) {
        super(ModTanksAndPipes.FLUID_DRUM_BE, pos, state);
        this.mode = mode;
        this.fluidStorage = new UtilityFluidStorage(this, capacity) {
            @Override
            protected long getCapacity(FluidVariant variant) {
                return mode == DrumMode.CREATIVE ? Long.MAX_VALUE / 4 : super.getCapacity(variant);
            }

            @Override
            public long insert(FluidVariant insertedVariant, long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
                if (mode == DrumMode.VOID) return maxAmount;
                if (locked && !variant.isBlank() && !variant.equals(insertedVariant)) return 0;
                if (mode == DrumMode.CREATIVE) {
                    if (variant.isBlank()) variant = insertedVariant;
                    amount = getCapacity(insertedVariant);
                    return maxAmount;
                }
                return super.insert(insertedVariant, maxAmount, transaction);
            }

            @Override
            public long extract(FluidVariant extractedVariant, long maxAmount, net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
                if (mode == DrumMode.CREATIVE && !variant.isBlank() && variant.equals(extractedVariant)) return maxAmount;
                return super.extract(extractedVariant, maxAmount, transaction);
            }
        };
    }

    public DrumMode mode() { return mode; }

    public boolean locked() { return locked; }

    public void toggleLocked() {
        locked = !locked;
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    public int comparatorOutput() {
        return FluidStorageBlockHelper.comparatorOutput(fluidStorage);
    }

    @Override public net.fabricmc.fabric.api.transfer.v1.storage.Storage<FluidVariant> liquidFabricStorage() { return fluidStorage; }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        FluidStorageBlockHelper.readStorage(fluidStorage, nbt, lookup);
        locked = nbt.getBoolean("Locked");
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        FluidStorageBlockHelper.writeStorage(fluidStorage, nbt, lookup);
        nbt.putString("Mode", mode.name());
        nbt.putBoolean("Locked", locked);
    }
}
