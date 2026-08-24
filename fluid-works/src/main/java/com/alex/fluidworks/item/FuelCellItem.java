package com.alex.fluidworks.item;

import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Optional;

/** A non-drinkable, reusable 500 mB component-backed fluid container. */
public final class FuelCellItem extends Item implements FluidOverlayItem, TintedFluidContainerItem {
    private final long capacityDroplets;

    public FuelCellItem(Settings settings, long capacityDroplets) {
        super(settings.maxCount(1));
        this.capacityDroplets = capacityDroplets;
    }

    @Override
    public long capacityDroplets() {
        return capacityDroplets;
    }

    @Override
    public boolean canHoldPotionLiquids() {
        return false;
    }

    @Override
    public Optional<StoredFluidComponent> getStoredFluidComponent(ItemStack stack) {
        StoredFluidComponent stored = stack.getOrDefault(ModComponents.STORED_FLUID,
            StoredFluidComponent.EMPTY);
        return stored.isEmpty() ? Optional.empty() : Optional.of(stored);
    }

    @Override
    public ItemStack withStoredFluidComponent(ItemStack stack, StoredFluidComponent fluid) {
        FluidItemComponentHelper.set(stack, fluid);
        return stack;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return FluidItemComponentHelper.itemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return FluidItemComponentHelper.itemBarStep(stack);
    }
}
