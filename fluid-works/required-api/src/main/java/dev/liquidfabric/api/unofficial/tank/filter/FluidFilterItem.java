package dev.liquidfabric.api.unofficial.tank.filter;

import dev.liquidfabric.api.unofficial.api.filter.FluidFilter;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Basic portable fluid filter. Later UI passes can bind this to screens; this
 * first pass exposes the public shape and tooltip behavior.
 */
public class FluidFilterItem extends Item {
    public FluidFilterItem(Settings settings) {
        super(settings);
    }

    public static Optional<Identifier> getLockedFluid(ItemStack stack) {
        // Component-backed storage should replace this once final 1.21.11
        // component wiring is verified.
        return Optional.empty();
    }

    public static FluidFilter asFilter(ItemStack stack) {
        return getLockedFluid(stack)
            .<FluidFilter>map(id -> variant -> !variant.isBlank() && id.equals(net.minecraft.registry.Registries.FLUID.getId(variant.getFluid())))
            .orElse(FluidFilter.ANY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal("Fluid Filter"));
        tooltip.add(Text.literal("Use in future pipes/drums to restrict accepted fluid."));
        getLockedFluid(stack).ifPresentOrElse(
            id -> tooltip.add(Text.literal("Locked to: " + id)),
            () -> tooltip.add(Text.literal("Mode: Any fluid"))
        );
    }
}
