package dev.liquidfabric.api.unofficial.tank.common;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.block.entity.BlockEntity;

public class UtilityFluidStorage extends SingleVariantStorage<FluidVariant> {
    private final BlockEntity owner;
    private final long capacity;

    public UtilityFluidStorage(BlockEntity owner, long capacity) {
        this.owner = owner;
        this.capacity = capacity;
    }

    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return capacity;
    }

    @Override
    protected void onFinalCommit() {
        owner.markDirty();
        if (owner.getWorld() != null) owner.getWorld().updateListeners(owner.getPos(), owner.getCachedState(), owner.getCachedState(), 3);
    }

    public long amountView() { return amount; }

    public long getCapacityView() { return getCapacity(variant); }

    public FluidVariant variantView() { return variant; }

}
