package dev.liquidfabric.api.unofficial.api.container;

import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Public item-side fluid container contract.
 *
 * Addon items implement this instead of depending on UtilityAPI internals.
 * Amounts are stored in droplets; UI may format as mB/buckets.
 *
 * Method names intentionally avoid colliding with older internal helper
 * interfaces that used getStoredFluid()/withStoredFluid().
 */
public interface FluidContainerItem {
    long getCapacityDroplets(ItemStack stack);

    Optional<StoredFluidComponent> getStoredFluidComponent(ItemStack stack);

    ItemStack withStoredFluidComponent(ItemStack stack, StoredFluidComponent fluid);

    default boolean isEmpty(ItemStack stack) {
        return getStoredFluidComponent(stack).map(StoredFluidComponent::isEmpty).orElse(true);
    }

    default boolean canAcceptFluid(ItemStack stack, Identifier liquidOrFluidId) {
        return true;
    }

    default long getStoredDroplets(ItemStack stack) {
        return getStoredFluidComponent(stack).map(StoredFluidComponent::amountDroplets).orElse(0L);
    }

    default boolean canFit(ItemStack stack, StoredFluidComponent fluid) {
        return fluid.amountDroplets() <= getCapacityDroplets(stack) && canAcceptFluid(stack, fluid.liquidId());
    }
}
