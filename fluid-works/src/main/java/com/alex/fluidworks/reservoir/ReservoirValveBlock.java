package com.alex.fluidworks.reservoir;

import com.alex.fluidworks.fluid.FluidInteraction;
import com.mojang.serialization.MapCodec;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ReservoirValveBlock extends BlockWithEntity implements TieredReservoirPart {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    private final ReservoirTier tier;
    private final MapCodec<ReservoirValveBlock> codec;

    public ReservoirValveBlock(ReservoirTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
        this.codec = MapCodec.unit(this);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    public ReservoirTier tier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ReservoirValveBlockEntity(pos, state);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos,
                                      Direction direction) {
        if (world.getBlockEntity(pos) instanceof ReservoirValveBlockEntity valve
            && valve.controller() instanceof ReservoirControllerBlockEntity controller) {
            return controller.storage().comparatorOutput();
        }
        return 0;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof ReservoirValveBlockEntity valve)) return ActionResult.PASS;
        if (world.isClient()) {
            return FluidItemComponentHelper.capacity(stack) > 0 ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        ReservoirControllerBlockEntity controller = valve.controller();
        if (controller == null) {
            player.sendMessage(Text.translatable("message.fluidworks.reservoir.invalid"), true);
            return ActionResult.FAIL;
        }
        return FluidInteraction.transfer((ServerWorld) world, pos, stack, controller.storage());
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos, moved);
        ReservoirStructure.revalidateNearby(world, pos);
    }
}
