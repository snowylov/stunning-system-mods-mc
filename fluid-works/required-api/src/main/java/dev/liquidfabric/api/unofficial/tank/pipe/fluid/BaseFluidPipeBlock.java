package dev.liquidfabric.api.unofficial.tank.pipe.fluid;

import dev.liquidfabric.api.unofficial.helper.block.FluidPipeBlockHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BaseFluidPipeBlock extends BlockWithEntity {
    public static final MapCodec<BaseFluidPipeBlock> CODEC = createCodec(BaseFluidPipeBlock::new);
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    public static final BooleanProperty UP = BooleanProperty.of("up");
    public static final BooleanProperty DOWN = BooleanProperty.of("down");

    public BaseFluidPipeBlock(Settings settings) {
        super(settings.nonOpaque());
        setDefaultState(getDefaultState().with(NORTH,false).with(SOUTH,false).with(EAST,false).with(WEST,false).with(UP,false).with(DOWN,false));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new BaseFluidPipeBlockEntity(pos, state); }

    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH,SOUTH,EAST,WEST,UP,DOWN);
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        // Debounce instead of recomputing and notifying immediately. This prevents
        // neighbor-update storms when a pipe network is placed, broken, or moved by
        // another mod.
        FluidPipeBlockHelper.debounceInvalidateAround(world, pos);
    }

    public static BlockState compute(World world, BlockPos pos, BlockState state) {
        return FluidPipeBlockHelper.computeConnections(world, pos, state);
    }
}
