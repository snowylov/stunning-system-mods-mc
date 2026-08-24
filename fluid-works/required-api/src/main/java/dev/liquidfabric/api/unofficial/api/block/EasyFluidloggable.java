package dev.liquidfabric.api.unofficial.api.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidDrainable;
import net.minecraft.block.FluidFillable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Implement this on a block with a boolean fluidlogged property. The default
 * methods deliberately use vanilla FluidFillable/FluidDrainable entry points.
 */
public interface EasyFluidloggable extends FluidFillable, FluidDrainable {
    Fluid liquidFabricLoggedFluid(BlockState state);

    @Override
    default boolean canFillWithFluid(@Nullable LivingEntity filler, BlockView world, BlockPos pos,
                                     BlockState state, Fluid fluid) {
        return EasyFluidloggingHooks.canFill(filler, world, pos, state, fluid, liquidFabricLoggedFluid(state));
    }

    @Override
    default boolean tryFillWithFluid(WorldAccess world, BlockPos pos, BlockState state, FluidState fluidState) {
        return EasyFluidloggingHooks.tryFill(world, pos, state, fluidState, liquidFabricLoggedFluid(state));
    }

    @Override
    default ItemStack tryDrainFluid(@Nullable LivingEntity drainer, WorldAccess world, BlockPos pos, BlockState state) {
        return EasyFluidloggingHooks.tryDrain(drainer, world, pos, state, liquidFabricLoggedFluid(state));
    }

    @Override
    default Optional<SoundEvent> getBucketFillSound() {
        return Optional.empty();
    }
}
