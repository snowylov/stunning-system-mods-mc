package com.alex.fluidworks.fluid;

import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.Item;

/** Registration bundle and behavior flags for Fluid Works' non-metal liquids. */
public final class SpecialFluidFamily {
    public final String id;
    public final int tint;
    public final boolean ender;
    public final boolean cold;
    public FlowableFluid still;
    public FlowableFluid flowing;
    public FluidBlock block;
    public Item bucket;

    public SpecialFluidFamily(String id, int tint, boolean ender, boolean cold) {
        this.id = id;
        this.tint = tint;
        this.ender = ender;
        this.cold = cold;
    }

    public boolean matches(net.minecraft.fluid.Fluid fluid) {
        return fluid == still || fluid == flowing;
    }
}
