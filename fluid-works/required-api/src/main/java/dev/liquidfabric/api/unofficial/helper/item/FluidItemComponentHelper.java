package dev.liquidfabric.api.unofficial.helper.item;

import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.api.container.CustomFluidContainerItemRegistry;
import dev.liquidfabric.api.unofficial.api.container.FluidContainerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Central item-stack component helper for all UtilityAPI fluid-bearing items.
 *
 * This class intentionally owns the repetitive "read component, clamp amount,
 * preserve source attributes, remove when empty" behavior that was previously
 * repeated across liquid containers, universal buckets, needles, stations, and
 * drums.  Keep this helper common-safe: no client classes, no renderer calls.
 */
public final class FluidItemComponentHelper {
    private FluidItemComponentHelper() {}

    public static StoredFluidComponent get(ItemStack stack) {
        return stack.getOrDefault(ModComponents.STORED_FLUID, StoredFluidComponent.EMPTY);
    }

    public static boolean hasFluid(ItemStack stack) {
        return !get(stack).isEmpty();
    }

    public static boolean isEmpty(ItemStack stack) {
        return get(stack).isEmpty();
    }

    public static long amountDroplets(ItemStack stack) {
        return get(stack).amountDroplets();
    }

    public static long amountMb(ItemStack stack) {
        return FluidUnits.dropletsToMb(amountDroplets(stack));
    }

    public static Identifier fluidId(ItemStack stack) {
        return get(stack).liquidId();
    }

    public static SourceFluidAttributes sourceAttributes(ItemStack stack) {
        return get(stack).sourceAttributes();
    }

    public static long capacity(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof FluidContainerItem container) return container.getCapacityDroplets(stack);
        return CustomFluidContainerItemRegistry.find(item)
                .map(entry -> entry.definition().capacityDroplets())
                .orElse(0L);
    }

    public static boolean canHoldPotionLiquids(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof FluidOverlayItem overlay) return overlay.canHoldPotionLiquids();
        return CustomFluidContainerItemRegistry.find(item)
                .map(entry -> entry.definition().potionLiquidsAllowed())
                .orElse(false);
    }

    public static boolean canAcceptFluid(ItemStack stack, Identifier fluidId) {
        if (stack.getItem() instanceof FluidContainerItem container && !container.canAcceptFluid(stack, fluidId)) {
            return false;
        }
        return CustomFluidContainerItemRegistry.find(stack)
                .map(entry -> entry.definition().accepts(fluidId))
                .orElse(true);
    }

    public static StoredFluidComponent clampToStackCapacity(ItemStack stack, StoredFluidComponent fluid) {
        long capacity = capacity(stack);
        if (capacity <= 0 || fluid == null || fluid.isEmpty()) return StoredFluidComponent.EMPTY;
        return fluid.clamped(capacity);
    }

    public static ItemStack set(ItemStack stack, StoredFluidComponent fluid) {
        if (fluid != null && !fluid.isEmpty() && !canAcceptFluid(stack, fluid.liquidId())) return stack;
        StoredFluidComponent clamped = clampToStackCapacity(stack, fluid);
        stack.set(ModComponents.STORED_FLUID, clamped);
        return stack;
    }

    public static ItemStack clear(ItemStack stack) {
        stack.set(ModComponents.STORED_FLUID, StoredFluidComponent.EMPTY);
        return stack;
    }

    public static ItemStack set(ItemStack stack, Identifier fluidId, long droplets, SourceFluidAttributes attributes) {
        return set(stack, new StoredFluidComponent(fluidId, droplets, attributes == null ? SourceFluidAttributes.EMPTY : attributes));
    }

    public static ItemStack setBucket(ItemStack stack, Identifier fluidId, SourceFluidAttributes attributes) {
        return set(stack, fluidId, FluidUnits.BUCKET_DROPLETS, attributes);
    }

    public static boolean sameFluidAndSource(ItemStack a, ItemStack b) {
        StoredFluidComponent fa = get(a);
        StoredFluidComponent fb = get(b);
        if (fa.isEmpty() && fb.isEmpty()) return true;
        return fa.liquidId().equals(fb.liquidId()) && fa.sourceAttributes().equals(fb.sourceAttributes());
    }

    public static boolean canMergeFluidStacks(ItemStack target, ItemStack source) {
        if (target.isEmpty() || source.isEmpty()) return false;
        if (!sameFluidAndSource(target, source)) return false;
        long targetCapacity = capacity(target);
        return targetCapacity > 0 && amountDroplets(target) < targetCapacity;
    }

    public static long freeSpace(ItemStack stack) {
        long capacity = capacity(stack);
        if (capacity <= 0) return 0;
        return Math.max(0, capacity - amountDroplets(stack));
    }

    public static long addDroplets(ItemStack stack, long droplets) {
        if (droplets <= 0) return 0;
        StoredFluidComponent fluid = get(stack);
        if (fluid.isEmpty()) return 0;
        long inserted = Math.min(droplets, freeSpace(stack));
        if (inserted <= 0) return 0;
        set(stack, new StoredFluidComponent(fluid.liquidId(), fluid.amountDroplets() + inserted, fluid.sourceAttributes()));
        return inserted;
    }

    public static long removeDroplets(ItemStack stack, long droplets) {
        if (droplets <= 0) return 0;
        StoredFluidComponent fluid = get(stack);
        if (fluid.isEmpty()) return 0;
        long removed = Math.min(droplets, fluid.amountDroplets());
        long remaining = fluid.amountDroplets() - removed;
        if (remaining <= 0) clear(stack);
        else set(stack, new StoredFluidComponent(fluid.liquidId(), remaining, fluid.sourceAttributes()));
        return removed;
    }

    public static StoredFluidComponent copyPartial(ItemStack source, long droplets) {
        StoredFluidComponent fluid = get(source);
        if (fluid.isEmpty() || droplets <= 0) return StoredFluidComponent.EMPTY;
        return new StoredFluidComponent(fluid.liquidId(), Math.min(droplets, fluid.amountDroplets()), fluid.sourceAttributes());
    }

    public static boolean fillEmptyFromComponent(ItemStack target, StoredFluidComponent fluid) {
        if (target.isEmpty() || !get(target).isEmpty() || fluid == null || fluid.isEmpty()) return false;
        set(target, fluid);
        return !get(target).isEmpty();
    }

    public static int itemBarStep(ItemStack stack) {
        long capacity = capacity(stack);
        if (capacity <= 0) return 0;
        StoredFluidComponent fluid = get(stack);
        if (fluid.isEmpty()) return 0;
        return Math.min(13, Math.max(1, Math.round(13f * fluid.amountDroplets() / capacity)));
    }

    public static boolean itemBarVisible(ItemStack stack) {
        return !get(stack).isEmpty();
    }
}
