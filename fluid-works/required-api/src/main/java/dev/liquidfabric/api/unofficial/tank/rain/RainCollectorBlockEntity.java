package dev.liquidfabric.api.unofficial.tank.rain;

import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import dev.liquidfabric.api.unofficial.tank.common.UtilityFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class RainCollectorBlockEntity extends BlockEntity implements dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity {
    public final UtilityFluidStorage fluidStorage = new UtilityFluidStorage(this, FluidContainerSizes.BUCKET_DROPLETS * 4L);

    @Override public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> liquidFabricStorage() { return fluidStorage; }

    public RainCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModTanksAndPipes.RAIN_COLLECTOR_BE, pos, state);
    }

    public void collectRainTick() {
        if (world == null || world.isClient || !world.isRaining() || !world.isSkyVisible(pos.up())) return;
        try (var tx = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
            fluidStorage.insert(FluidVariant.of(Fluids.WATER), 81L, tx);
            tx.commit();
        }
    }

    @Override protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        fluidStorage.readNbt(nbt, lookup);
    }

    @Override protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        fluidStorage.writeNbt(nbt, lookup);
    }
}
