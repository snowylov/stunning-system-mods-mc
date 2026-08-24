package com.alex.fluidworks.item;

import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** A reusable 250 mB glass bottle for fluids that are not vanilla water. */
public final class CustomFluidBottleItem extends Item implements FluidOverlayItem, TintedFluidContainerItem {
    public static final long CAPACITY = FluidUnits.mbToDroplets(250);

    public CustomFluidBottleItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public long capacityDroplets() {
        return CAPACITY;
    }

    @Override
    public boolean canHoldPotionLiquids() {
        return false;
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
