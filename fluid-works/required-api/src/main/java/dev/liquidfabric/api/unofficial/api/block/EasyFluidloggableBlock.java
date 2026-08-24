package dev.liquidfabric.api.unofficial.api.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/** Ready-to-register block base for one chosen logged fluid, including modded fluids. */
public class EasyFluidloggableBlock extends Block implements EasyFluidloggable {
    private final Supplier<? extends Fluid> loggedFluid;

    public EasyFluidloggableBlock(Settings settings, Supplier<? extends Fluid> loggedFluid) {
        super(settings);
        this.loggedFluid = Objects.requireNonNull(loggedFluid, "loggedFluid");
        setDefaultState(getDefaultState().with(EasyFluidloggingHooks.FLUIDLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(EasyFluidloggingHooks.FLUIDLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return EasyFluidloggingHooks.placementState(context, getDefaultState(), loggedFluid.get());
    }

    @Override
    public Fluid liquidFabricLoggedFluid(BlockState state) {
        return loggedFluid.get();
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return EasyFluidloggingHooks.fluidState(state, loggedFluid.get());
    }

    @Override
    public Optional<SoundEvent> getBucketFillSound() {
        return loggedFluid.get().getBucketFillSound();
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
                                                    BlockPos pos, Direction direction, BlockPos neighborPos,
                                                    BlockState neighborState, Random random) {
        if (!getFluidState(state).isEmpty()) {
            Fluid fluid = loggedFluid.get();
            tickView.scheduleFluidTick(pos, fluid, fluid.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }
}
