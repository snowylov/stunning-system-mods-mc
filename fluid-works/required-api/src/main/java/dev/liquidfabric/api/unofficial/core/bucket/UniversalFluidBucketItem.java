package dev.liquidfabric.api.unofficial.core.bucket;

import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.helper.item.FluidBucketItemHelper;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemTooltipHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Fallback bucket for fluids that do not ship their own bucket item.
 *
 * It uses layer0 as the bucket shell/base and layer1 as a tintable fluid overlay.
 * This avoids replacing vanilla BucketItem behavior and keeps the fallback opt-in.
 */
public class UniversalFluidBucketItem extends Item implements FluidOverlayItem {
    public UniversalFluidBucketItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public long capacityDroplets() {
        return FluidUnits.BUCKET_DROPLETS;
    }

    @Override
    public boolean canHoldPotionLiquids() {
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return getStoredFluid(context.getStack()).isEmpty()
                ? FluidBucketItemHelper.pickupSourceFluidIntoBucket(context)
                : FluidBucketItemHelper.placeStoredFluidFromBucket(context);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return !getStoredFluid(stack).isEmpty();
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.min(13, Math.round(13f * getStoredFluid(stack).amountDroplets() / FluidUnits.BUCKET_DROPLETS));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        FluidItemTooltipHelper.appendShiftGate(stack, tooltip, FluidItemTooltipHelper.shouldShowExpanded(type));
    }

    public static ItemStack filledWith(Identifier fluidId) {
        return filledWith(fluidId, SourceFluidAttributes.EMPTY);
    }

    public static ItemStack filledWith(Identifier fluidId, SourceFluidAttributes attributes) {
        ItemStack stack = new ItemStack(ModUniversalBuckets.UNIVERSAL_FLUID_BUCKET);
        return FluidItemComponentHelper.setBucket(stack, fluidId, attributes == null ? SourceFluidAttributes.EMPTY : attributes);
    }
}
