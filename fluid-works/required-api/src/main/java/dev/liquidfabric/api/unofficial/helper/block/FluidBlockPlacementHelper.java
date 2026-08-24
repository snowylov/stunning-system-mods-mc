package dev.liquidfabric.api.unofficial.helper.block;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.source.FluidSourceAttributeUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;


/**
 * Server-authoritative placement/pickup helpers for fluid blocks.
 */
public final class FluidBlockPlacementHelper {
    private FluidBlockPlacementHelper() {}

    public static boolean canPlace(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || !state.getFluidState().isEmpty();
    }

    public static boolean placeFluid(World world, BlockPos pos, Fluid fluid, SourceFluidAttributes attributes, int flags) {
        if (world.isClient || fluid == null || fluid == net.minecraft.fluid.Fluids.EMPTY) return false;
        FluidState state = fluid.getDefaultState();
        BlockState blockState = state.getBlockState();
        if (!(blockState.getBlock() instanceof FluidBlock)) return false;
        if (!canPlace(world, pos)) return false;

        world.setBlockState(pos, blockState, flags);
        // Source attributes remain item/container metadata. This helper no longer
        // writes persistent sidecar world data; that avoids stale entries and fluid
        // mover/copy compatibility issues.
        return true;
    }

    public static boolean removeSourceFluid(World world, BlockPos pos) {
        if (world.isClient) return false;
        FluidState state = world.getFluidState(pos);
        if (state.isEmpty() || !state.isStill()) return false;
        world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 11);
        return true;
    }

    public static boolean isStillFluid(World world, BlockPos pos) {
        FluidState state = world.getFluidState(pos);
        return !state.isEmpty() && state.isStill();
    }

    public static Fluid fluidAt(World world, BlockPos pos) {
        return world.getFluidState(pos).getFluid();
    }

    public static SourceFluidAttributes placedAttributes(World world, BlockPos pos) {
        return SourceFluidAttributes.EMPTY;
    }

    public static SourceFluidAttributes bestEffortSourceAttributes(World world, BlockPos pos) {
        return FluidSourceAttributeUtil.infer(world, pos);
    }

    public static String fluidIdString(World world, BlockPos pos) {
        Fluid fluid = fluidAt(world, pos);
        return String.valueOf(Registries.FLUID.getId(fluid));
    }

    public static boolean sameFluid(World world, BlockPos a, BlockPos b) {
        return fluidAt(world, a) == fluidAt(world, b);
    }

    public static int countConnectedSources(World world, BlockPos origin, int radius) {
        if (!isStillFluid(world, origin)) return 0;
        Fluid fluid = fluidAt(world, origin);
        int r = Math.max(0, Math.min(radius, 16));
        int count = 0;
        for (BlockPos pos : BlockPos.iterateOutwards(origin, r, r, r)) {
            FluidState state = world.getFluidState(pos);
            if (state.isStill() && state.getFluid() == fluid) count++;
        }
        return count;
    }
}
