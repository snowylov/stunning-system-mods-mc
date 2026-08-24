package com.alex.fluidworks.fluid;

import dev.liquidfabric.api.unofficial.tank.common.UtilityFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.entity.BlockEntity;

/** Storage with a capacity that can change when a multiblock forms. */
public final class FluidWorksStorage extends UtilityFluidStorage {
    private long capacity;

    public FluidWorksStorage(BlockEntity owner, long capacity) {
        super(owner, Math.max(0, capacity));
        this.capacity = Math.max(0, capacity);
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = Math.max(amount, Math.max(0, capacity));
    }

    public void load(FluidVariant variant, long amount) {
        if (variant == null || variant.isBlank() || amount <= 0) {
            this.variant = FluidVariant.blank();
            this.amount = 0;
            return;
        }
        this.variant = variant;
        this.amount = Math.min(Math.max(0, amount), Math.max(capacity, amount));
        this.capacity = Math.max(capacity, this.amount);
    }

    public int comparatorOutput() {
        if (capacity <= 0 || amount <= 0) return 0;
        return Math.max(1, Math.min(15, (int) Math.floor((double) amount * 15.0D / capacity)));
    }
}
