package dev.liquidfabric.api.unofficial.api.filter;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

@FunctionalInterface
public interface FluidFilter {
    FluidFilter ANY = variant -> true;
    FluidFilter NONE = variant -> false;

    boolean allows(FluidVariant variant);

    default FluidFilter and(FluidFilter other) {
        return variant -> this.allows(variant) && other.allows(variant);
    }

    default FluidFilter or(FluidFilter other) {
        return variant -> this.allows(variant) || other.allows(variant);
    }

    static FluidFilter exact(FluidVariant expected) {
        return variant -> expected != null && expected.equals(variant);
    }
}
