package com.alex.fluidworks.block;

import com.mojang.serialization.MapCodec;
import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

/** Thin attached gauge that reads any adjacent Transfer API fluid storage. */
public final class FluidMonitorBlock extends Block {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    private final MapCodec<FluidMonitorBlock> codec = MapCodec.unit(this);

    public FluidMonitorBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
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
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         net.minecraft.block.ShapeContext context) {
        return shape(state.get(FACING));
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        Storage<FluidVariant> storage = findStorage(world, pos, state.get(FACING));
        if (storage == null) return 0;
        long amount = 0;
        long capacity = 0;
        for (var view : storage) {
            amount += view.getAmount();
            capacity += view.getCapacity();
        }
        return capacity <= 0 || amount <= 0 ? 0
            : Math.max(1, Math.min(15, (int) Math.floor(amount * 15.0D / capacity)));
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient()) {
            Storage<FluidVariant> storage = findStorage(world, pos, state.get(FACING));
            FluidVariant variant = FluidTransferHelper.firstStoredVariant(storage).orElse(FluidVariant.blank());
            if (storage == null || variant.isBlank()) {
                player.sendMessage(Text.translatable("message.fluidworks.monitor.empty"), true);
            } else {
                long amount = FluidTransferHelper.storedAmount(storage, variant);
                player.sendMessage(Text.translatable("message.fluidworks.monitor.reading",
                    Registries.FLUID.getId(variant.getFluid()).toString(), FluidUnits.dropletsToMb(amount)), true);
            }
        }
        return ActionResult.SUCCESS;
    }

    public static Storage<FluidVariant> findStorage(World world, BlockPos monitorPos, Direction facing) {
        BlockPos targetPos = monitorPos.offset(facing.getOpposite());
        return FluidStorage.SIDED.find(world, targetPos, facing);
    }

    private static VoxelShape shape(Direction direction) {
        return switch (direction) {
            case NORTH -> createCuboidShape(1, 1, 15, 15, 15, 16);
            case SOUTH -> createCuboidShape(1, 1, 0, 15, 15, 1);
            case WEST -> createCuboidShape(15, 1, 1, 16, 15, 15);
            case EAST -> createCuboidShape(0, 1, 1, 1, 15, 15);
            case UP -> createCuboidShape(1, 0, 1, 15, 1, 15);
            case DOWN -> createCuboidShape(1, 15, 1, 15, 16, 15);
        };
    }
}
