package com.alex.fluidworks.fluid;

import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.Item;

/** Mutable registration bundle used to break the still/flowing/block/bucket supplier cycle. */
public final class MetalFluidFamily {
    public final String id;
    public final int tint;
    public FlowableFluid still;
    public FlowableFluid flowing;
    public FluidBlock block;
    public Item bucket;

    public MetalFluidFamily(String id, int tint) {
        this.id = id;
        this.tint = tint;
    }
}
