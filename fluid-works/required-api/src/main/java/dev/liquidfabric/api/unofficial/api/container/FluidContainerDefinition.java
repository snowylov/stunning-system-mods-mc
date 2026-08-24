package dev.liquidfabric.api.unofficial.api.container;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Describes an item that stores UtilityAPI's {@code stored_fluid} component.
 * The model's fluid pixels should be layer 1; that layer is tinted at runtime.
 */
public record FluidContainerDefinition(
        long capacityDroplets,
        boolean potionLiquidsAllowed,
        int fluidOverlayTintIndex,
        Predicate<Identifier> fluidFilter
) {
    public FluidContainerDefinition {
        if (capacityDroplets <= 0) throw new IllegalArgumentException("capacityDroplets must be positive");
        if (fluidOverlayTintIndex < 0) throw new IllegalArgumentException("fluidOverlayTintIndex cannot be negative");
        fluidFilter = Objects.requireNonNull(fluidFilter, "fluidFilter");
    }

    public static FluidContainerDefinition bucket(long capacityDroplets) {
        return new FluidContainerDefinition(capacityDroplets, false, 1, id -> true);
    }

    public static FluidContainerDefinition bottle(long capacityDroplets, boolean potionLiquidsAllowed) {
        return new FluidContainerDefinition(capacityDroplets, potionLiquidsAllowed, 1, id -> true);
    }

    public static FluidContainerDefinition custom(long capacityDroplets, boolean potionLiquidsAllowed,
                                                  int fluidOverlayTintIndex, Predicate<Identifier> fluidFilter) {
        return new FluidContainerDefinition(capacityDroplets, potionLiquidsAllowed, fluidOverlayTintIndex, fluidFilter);
    }

    public boolean accepts(Identifier fluidId) {
        return fluidId != null && fluidFilter.test(fluidId);
    }
}
