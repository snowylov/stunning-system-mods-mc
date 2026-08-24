package dev.liquidfabric.api.unofficial.api.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;

/**
 * Register this block in Java, then select its logged fluid with a
 * utilityapi/fluidloggable_blocks datapack JSON file.
 */
public class JsonFluidloggableBlock extends Block implements EasyFluidloggable {
    public JsonFluidloggableBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(EasyFluidloggingHooks.FLUIDLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(EasyFluidloggingHooks.FLUIDLOGGED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Fluid fluid = EasyFluidloggingHooks.configuredFluid(getDefaultState());
        return EasyFluidloggingHooks.placementState(context, getDefaultState(), fluid);
    }

    @Override
    public Fluid liquidFabricLoggedFluid(BlockState state) {
        return EasyFluidloggingHooks.configuredFluid(state);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return EasyFluidloggingHooks.fluidState(state, liquidFabricLoggedFluid(state));
    }
}
