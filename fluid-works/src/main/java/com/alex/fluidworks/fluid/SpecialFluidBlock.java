package com.alex.fluidworks.fluid;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import org.jetbrains.annotations.Nullable;

/** Fluid block that applies cold terrain effects and participates in JSON mixing. */
public final class SpecialFluidBlock extends FluidBlock {
    private final SpecialFluidFamily family;

    public SpecialFluidBlock(FlowableFluid fluid, SpecialFluidFamily family, Settings settings) {
        super(fluid, settings);
        this.family = family;
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState,
                                boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        updateSurroundings(world, pos);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                                  @Nullable WireOrientation wireOrientation, boolean notify) {
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
        updateSurroundings(world, pos);
    }

    private void updateSurroundings(World world, BlockPos pos) {
        if (world.isClient()) return;
        if (FluidMixingRecipeManager.tryMix(world, pos)) return;
        if (!family.cold) return;
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos target = pos.add(x, y, z);
                    BlockState targetState = world.getBlockState(target);
                    if (targetState.isOf(Blocks.WATER)) {
                        world.setBlockState(target, Blocks.ICE.getDefaultState(), Block.NOTIFY_ALL);
                        continue;
                    }
                    BlockPos above = target.up();
                    if (targetState.isSolidBlock(world, target)
                        && world.getBlockState(above).isAir()
                        && !above.equals(pos)) {
                        world.setBlockState(above, Blocks.SNOW.getDefaultState(), Block.NOTIFY_ALL);
                    }
                }
            }
        }
    }
}
