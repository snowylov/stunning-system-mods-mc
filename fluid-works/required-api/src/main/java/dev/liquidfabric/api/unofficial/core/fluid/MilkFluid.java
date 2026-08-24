package dev.liquidfabric.api.unofficial.core.fluid;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.WorldAccess;

/**
 * UtilityAPI milk as a real placeable fluid.
 *
 * Compatibility intent:
 * - Does not replace vanilla blocks.
 * - Keeps vanilla Items.MILK_BUCKET as the pickup/placement container.
 * - Uses explicit interaction hooks/mixins instead of global registry replacement.
 */
public abstract class MilkFluid extends FlowableFluid {
    @Override
    public Fluid getFlowing() {
        return ModUtilityFluids.FLOWING_MILK;
    }

    @Override
    public Fluid getStill() {
        return ModUtilityFluids.MILK;
    }

    @Override
    public Item getBucketItem() {
        return Items.MILK_BUCKET;
    }

    @Override
    protected boolean isInfinite(WorldView world) {
        return false;
    }

    @Override
    protected void beforeBreakingBlock(WorldAccess world, BlockPos pos, BlockState state) {
        // Milk is non-destructive. It should not break blocks when flowing into them.
    }

    @Override
    protected int getFlowSpeed(WorldView world) {
        return 4;
    }

    @Override
    protected int getLevelDecreasePerBlock(WorldView world) {
        return 1;
    }

    @Override
    public int getTickRate(WorldView world) {
        return 5;
    }

    @Override
    protected float getBlastResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState toBlockState(FluidState state) {
        return ModUtilityFluids.MILK_BLOCK.getDefaultState().with(FluidBlock.LEVEL, getBlockStateLevel(state));
    }

    @Override
    protected boolean isFlowBlocked(BlockView world, BlockPos pos, BlockState state) {
        return false;
    }

    public static final class Still extends MilkFluid {
        @Override
        public boolean isStill(FluidState state) {
            return true;
        }

        @Override
        public int getLevel(FluidState state) {
            return 8;
        }
    }

    public static final class Flowing extends MilkFluid {
        @Override
        protected void appendProperties(StateManager.Builder<Fluid, FluidState> builder) {
            super.appendProperties(builder);
            builder.add(LEVEL);
        }

        @Override
        public boolean isStill(FluidState state) {
            return false;
        }

        @Override
        public int getLevel(FluidState state) {
            return state.get(LEVEL);
        }
    }
}
