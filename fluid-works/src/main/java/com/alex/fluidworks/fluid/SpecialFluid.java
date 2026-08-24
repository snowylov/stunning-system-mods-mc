package com.alex.fluidworks.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

/** Non-infinite, water-speed source/flowing implementation for special liquids. */
public abstract class SpecialFluid extends FlowableFluid {
    protected final SpecialFluidFamily family;

    protected SpecialFluid(SpecialFluidFamily family) {
        this.family = family;
    }

    @Override public Fluid getFlowing() { return family.flowing; }
    @Override public Fluid getStill() { return family.still; }
    @Override public Item getBucketItem() { return family.bucket; }
    @Override protected boolean isInfinite(ServerWorld world) { return false; }
    @Override protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) { }
    @Override protected int getMaxFlowDistance(WorldView world) { return 4; }
    @Override protected int getLevelDecreasePerBlock(WorldView world) { return 1; }
    @Override public int getTickRate(WorldView world) { return 5; }
    @Override protected float getBlastResistance() { return 100.0F; }
    @Override public boolean canBeReplacedWith(FluidState state, BlockView world, BlockPos pos,
                                               Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !fluid.matchesType(this);
    }
    @Override protected BlockState toBlockState(FluidState state) {
        return family.block.getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
    }
    @Override protected boolean isFlowBlocked(BlockView world, BlockPos pos, Direction direction) {
        return false;
    }

    public static final class Still extends SpecialFluid {
        public Still(SpecialFluidFamily family) { super(family); }
        @Override public boolean isStill(FluidState state) { return true; }
        @Override public int getLevel(FluidState state) { return 8; }
    }

    public static final class Flowing extends SpecialFluid {
        public Flowing(SpecialFluidFamily family) { super(family); }
        @Override protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }
        @Override public boolean isStill(FluidState state) { return false; }
        @Override public int getLevel(FluidState state) { return state.get(LEVEL); }
    }
}
