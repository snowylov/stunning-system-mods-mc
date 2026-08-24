package com.alex.fluidworks.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

/** A molten-fluid block whose contact recipes are supplied by reloadable JSON data. */
public final class MixingMetalFluidBlock extends FluidBlock {
    public MixingMetalFluidBlock(FlowableFluid fluid, MetalFluidFamily family, Settings settings) {
        super(fluid, settings);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState,
                                boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        tryMix(world, pos);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                                  @Nullable WireOrientation wireOrientation, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
        tryMix(world, pos);
    }

    private void tryMix(World world, BlockPos pos) {
        FluidMixingRecipeManager.tryMix(world, pos);
    }
}
