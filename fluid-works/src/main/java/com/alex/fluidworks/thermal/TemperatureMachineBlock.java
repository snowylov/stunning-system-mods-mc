package com.alex.fluidworks.thermal;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Centered 12x12 thermal chest with a server-owned target temperature. */
public final class TemperatureMachineBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape BODY = createCuboidShape(2, 1, 2, 14, 13, 14);

    private final boolean heating;
    private final MapCodec<TemperatureMachineBlock> codec = MapCodec.unit(this);

    public TemperatureMachineBlock(boolean heating, Settings settings) {
        super(settings);
        this.heating = heating;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    public boolean heating() { return heating; }
    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return codec; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                                    ShapeContext context) { return shape(state.get(FACING)); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                                      ShapeContext context) { return shape(state.get(FACING)); }
    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TemperatureMachineBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                           BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ThermalContent.TEMPERATURE_MACHINE_BLOCK_ENTITY,
            TemperatureMachineBlockEntity::serverTick);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof TemperatureMachineBlockEntity machine) {
            player.openHandledScreen(machine);
        }
        return ActionResult.SUCCESS;
    }
    @Override protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!moved && world.getBlockEntity(pos) instanceof TemperatureMachineBlockEntity machine) {
            ItemScatterer.spawn(world, pos, machine);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    private static VoxelShape shape(Direction facing) {
        VoxelShape panel = switch (facing) {
            case NORTH -> createCuboidShape(4, 4, 1, 12, 10, 2);
            case SOUTH -> createCuboidShape(4, 4, 14, 12, 10, 15);
            case WEST -> createCuboidShape(1, 4, 4, 2, 10, 12);
            case EAST -> createCuboidShape(14, 4, 4, 15, 10, 12);
            default -> VoxelShapes.empty();
        };
        return VoxelShapes.union(BODY, panel);
    }
}
