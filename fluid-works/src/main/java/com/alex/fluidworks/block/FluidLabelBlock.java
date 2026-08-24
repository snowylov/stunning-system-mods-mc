package com.alex.fluidworks.block;

import com.mojang.serialization.MapCodec;
import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.Registries;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** A persistent label that captures the adjacent tank's current fluid identity. */
public final class FluidLabelBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    private final MapCodec<FluidLabelBlock> codec = MapCodec.unit(this);

    public FluidLabelBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
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
        return getDefaultState().with(FACING, context.getSide());
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidLabelBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         net.minecraft.block.ShapeContext context) {
        Direction direction = state.get(FACING);
        return switch (direction) {
            case NORTH -> createCuboidShape(4, 5, 15, 12, 11, 16);
            case SOUTH -> createCuboidShape(4, 5, 0, 12, 11, 1);
            case WEST -> createCuboidShape(15, 5, 4, 16, 11, 12);
            case EAST -> createCuboidShape(0, 5, 4, 1, 11, 12);
            case UP -> createCuboidShape(4, 0, 5, 12, 1, 11);
            case DOWN -> createCuboidShape(4, 15, 5, 12, 16, 11);
        };
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof FluidLabelBlockEntity label) {
            if (player.isSneaking()) {
                label.clearLabel();
                player.sendMessage(Text.translatable("message.fluidworks.label.cleared"), true);
                return ActionResult.SUCCESS;
            }
            Storage<FluidVariant> storage = FluidMonitorBlock.findStorage(world, pos, state.get(FACING));
            FluidVariant variant = FluidTransferHelper.firstStoredVariant(storage).orElse(FluidVariant.blank());
            if (!variant.isBlank()) label.setFluidId(Registries.FLUID.getId(variant.getFluid()));
            player.sendMessage(label.fluidId() == null
                ? Text.translatable("message.fluidworks.label.empty")
                : Text.translatable("message.fluidworks.label.set", label.fluidId().toString()), true);
        }
        return ActionResult.SUCCESS;
    }
}
