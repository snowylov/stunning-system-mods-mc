package dev.liquidfabric.api.unofficial.tank.buildcraft;

import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import dev.liquidfabric.api.unofficial.tank.common.UtilityFluidStorage;
import dev.liquidfabric.api.unofficial.helper.block.FluidStorageBlockHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class BaseStackableTankBlockEntity extends BlockEntity implements dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity {
    public static final int SAFE_VERTICAL_SCAN_LIMIT = 64;
    public final UtilityFluidStorage fluidStorage = new UtilityFluidStorage(this, FluidContainerSizes.STACKABLE_TANK_DROPLETS);

    public BaseStackableTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModTanksAndPipes.STACKABLE_TANK_BE, pos, state);
    }

    public int getComparatorOutput() {
        return FluidStorageBlockHelper.comparatorOutput(fluidStorage);
    }

    @Override public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> liquidFabricStorage() { return fluidStorage; }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        FluidStorageBlockHelper.readStorage(fluidStorage, nbt, lookup);
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        FluidStorageBlockHelper.writeStorage(fluidStorage, nbt, lookup);
    }
}
