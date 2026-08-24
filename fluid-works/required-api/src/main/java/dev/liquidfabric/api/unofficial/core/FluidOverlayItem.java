package dev.liquidfabric.api.unofficial.core;

import dev.liquidfabric.api.unofficial.api.container.FluidContainerItem;
import java.util.Optional;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public interface FluidOverlayItem extends FluidContainerItem {
    long capacityDroplets();

    @Override
    default long getCapacityDroplets(ItemStack stack) { return capacityDroplets(); }
    boolean canHoldPotionLiquids();

    default StoredFluidComponent getStoredFluid(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_FLUID, StoredFluidComponent.EMPTY);
    }

    default ItemStack withStoredFluid(ItemStack stack, StoredFluidComponent fluid) {
        stack.set(ModComponents.STORED_FLUID, fluid.clamped(capacityDroplets()));
        return stack;
    }

    @Override
    default Optional<StoredFluidComponent> getStoredFluidComponent(ItemStack stack) {
        return Optional.of(getStoredFluid(stack));
    }

    @Override
    default ItemStack withStoredFluidComponent(ItemStack stack, StoredFluidComponent fluid) {
        return withStoredFluid(stack, fluid);
    }

    default Text fluidTooltip(ItemStack stack) {
        StoredFluidComponent fluid = getStoredFluid(stack);
        if (fluid.isEmpty()) return Text.translatable("tooltip.utilityapi.fluid_empty");
        return Text.literal(fluid.liquidId() + " - " + FluidUnits.dropletsToMb(fluid.amountDroplets()) + " mB");
    }

    static long getCapacity(Item item) {
        return item instanceof FluidOverlayItem overlay ? overlay.capacityDroplets() : 0;
    }
}
