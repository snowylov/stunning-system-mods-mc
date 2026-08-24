package dev.liquidfabric.api.unofficial.helper.item;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.core.source.FluidSourceAttributeUtil;
import dev.liquidfabric.api.unofficial.core.source.PlacedFluidAttributeTracker;
import net.minecraft.block.Blocks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Shared bucket placement/pickup helpers.
 *
 * Use this for fallback buckets and future custom buckets so vanilla behavior is
 * not globally replaced.  The helpers only act on the passed stack/context.
 */
public final class FluidBucketItemHelper {
    private FluidBucketItemHelper() {}

    public static ActionResult pickupSourceFluidIntoBucket(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        if (!FluidItemComponentHelper.get(stack).isEmpty()) return ActionResult.PASS;

        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        FluidState state = world.getFluidState(pos);
        if (state.isEmpty() || !state.isStill()) return ActionResult.PASS;

        Fluid fluid = state.getFluid();
        Identifier fluidId = Registries.FLUID.getId(fluid);
        if (fluidId == null || fluid == net.minecraft.fluid.Fluids.EMPTY) return ActionResult.FAIL;
        if (!FluidItemComponentHelper.canAcceptFluid(stack, fluidId)) return ActionResult.FAIL;

        if (!world.isClient) {
            long capacity = FluidItemComponentHelper.capacity(stack);
            SourceFluidAttributes attributes = FluidSourceAttributeUtil.resolve(world, pos);
            FluidItemComponentHelper.set(stack, fluidId, capacity, attributes);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
            PlacedFluidAttributeTracker.remove(world, pos);
        }
        return ActionResult.SUCCESS;
    }

    public static ActionResult placeStoredFluidFromBucket(ItemUsageContext context) {
        ItemStack stack = context.getStack();
        StoredFluidComponent stored = FluidItemComponentHelper.get(stack);
        if (stored.isEmpty()) return ActionResult.PASS;

        World world = context.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        Fluid fluid = Registries.FLUID.get(stored.liquidId());
        if (fluid == null || fluid == net.minecraft.fluid.Fluids.EMPTY) return ActionResult.FAIL;

        BlockPos placePos = context.getBlockPos().offset(context.getSide());
        if (!canPlaceFluidAt(world, placePos)) return ActionResult.FAIL;

        BlockState fluidBlockState = defaultFluidBlockState(fluid);
        if (fluidBlockState == null) return ActionResult.FAIL;

        world.setBlockState(placePos, fluidBlockState, 11);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            FluidItemComponentHelper.clear(stack);
        }
        return ActionResult.SUCCESS;
    }

    public static boolean canPlaceFluidAt(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.getFluidState().isEmpty();
    }

    public static BlockState defaultFluidBlockState(Fluid fluid) {
        FluidState fluidState = fluid.getDefaultState();
        BlockState state = fluidState.getBlockState();
        return state.getBlock() instanceof FluidBlock ? state : null;
    }

    public static StoredFluidComponent componentFromFluid(Fluid fluid, long droplets, SourceFluidAttributes attributes) {
        Identifier id = Registries.FLUID.getId(fluid);
        if (id == null || fluid == net.minecraft.fluid.Fluids.EMPTY) return StoredFluidComponent.EMPTY;
        return new StoredFluidComponent(id, droplets, attributes == null ? SourceFluidAttributes.EMPTY : attributes);
    }

    public static FluidVariant toVariant(StoredFluidComponent component) {
        if (component == null || component.isEmpty()) return FluidVariant.blank();
        Fluid fluid = Registries.FLUID.get(component.liquidId());
        return fluid == null || fluid == net.minecraft.fluid.Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid);
    }

    public static SourceFluidAttributes readPlacedSourceAttributes(World world, BlockPos pos) {
        return SourceFluidAttributes.EMPTY;
    }

    public static void rememberPlacedSourceAttributes(World world, BlockPos pos, SourceFluidAttributes attributes) {
        // No persistent world sidecar writes in core/content. Source attributes
        // are preserved on containers/items and inferred from the world when needed.
    }

    public static boolean isSourceFluidBlock(World world, BlockPos pos) {
        FluidState fluid = world.getFluidState(pos);
        return !fluid.isEmpty() && fluid.isStill();
    }

    public static Fluid fluidAt(World world, BlockPos pos) {
        return world.getFluidState(pos).getFluid();
    }
}
