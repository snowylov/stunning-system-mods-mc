package com.alex.fluidworks.item;

import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Reusable one-bucket container for any placeable fluid. */
public final class MaterialFluidBucketItem extends Item implements FluidOverlayItem, TintedFluidContainerItem {
    public MaterialFluidBucketItem(Settings settings) {
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
        ItemStack stack = context.getStack();
        StoredFluidComponent stored = FluidItemComponentHelper.get(stack);
        return stored.isEmpty() ? pickUp(context, stack) : place(context, stack, stored);
    }

    private static ActionResult pickUp(ItemUsageContext context, ItemStack stack) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        FluidState fluidState = world.getFluidState(pos);
        if (fluidState.isEmpty() || !fluidState.isStill()) return ActionResult.PASS;
        Fluid fluid = fluidState.getFluid();
        Identifier fluidId = Registries.FLUID.getId(fluid);
        if (fluid == Fluids.EMPTY || !FluidItemComponentHelper.canAcceptFluid(stack, fluidId)) {
            return ActionResult.FAIL;
        }
        if (!world.isClient()) {
            FluidItemComponentHelper.set(stack, new StoredFluidComponent(
                fluidId, FluidUnits.BUCKET_DROPLETS, SourceFluidAttributes.EMPTY));
            FluidVisuals.sync(stack);
            world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 11);
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        return ActionResult.SUCCESS;
    }

    private static ActionResult place(ItemUsageContext context, ItemStack stack, StoredFluidComponent stored) {
        World world = context.getWorld();
        Fluid fluid = Registries.FLUID.get(stored.liquidId());
        if (fluid == null || fluid == Fluids.EMPTY) return ActionResult.FAIL;
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        BlockState fluidState = fluid.getDefaultState().getBlockState();
        if (!(fluidState.getBlock() instanceof FluidBlock) || !world.getBlockState(pos).isReplaceable()) {
            return ActionResult.FAIL;
        }
        if (!world.isClient()) {
            world.setBlockState(pos, fluidState, 11);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
                FluidItemComponentHelper.clear(stack);
                FluidVisuals.sync(stack);
            }
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
        return ActionResult.SUCCESS;
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
