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
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Directional metal pipe that transfers both liquid and its carried temperature. */
public final class HeaterPipeBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.FACING;

    private final HeaterPipeMaterial material;
    private final MapCodec<HeaterPipeBlock> codec = MapCodec.unit(this);

    public HeaterPipeBlock(HeaterPipeMaterial material, Settings settings) {
        super(settings);
        this.material = material;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    public HeaterPipeMaterial material() { return material; }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return codec; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getSide());
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HeaterPipeBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                           BlockEntityType<T> type) {
        return world.isClient() ? null
            : validateTicker(type, ThermalContent.HEATER_PIPE_BLOCK_ENTITY, HeaterPipeBlockEntity::serverTick);
    }
    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                            PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof HeaterPipeBlockEntity pipe) {
            player.sendMessage(Text.translatable("message.fluidworks.heater_pipe_temperature",
                pipe.fluidTemperature()), true);
        }
        return ActionResult.SUCCESS;
    }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                                    ShapeContext context) {
        return shape(state.get(FACING));
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                                      ShapeContext context) {
        return shape(state.get(FACING));
    }

    private static VoxelShape shape(Direction facing) {
        return switch (facing.getAxis()) {
            case X -> VoxelShapes.union(createCuboidShape(0, 5, 5, 16, 11, 11),
                createCuboidShape(0, 3, 3, 2, 13, 13), createCuboidShape(14, 3, 3, 16, 13, 13));
            case Y -> VoxelShapes.union(createCuboidShape(5, 0, 5, 11, 16, 11),
                createCuboidShape(3, 0, 3, 13, 2, 13), createCuboidShape(3, 14, 3, 13, 16, 13));
            case Z -> VoxelShapes.union(createCuboidShape(5, 5, 0, 11, 11, 16),
                createCuboidShape(3, 3, 0, 13, 13, 2), createCuboidShape(3, 3, 14, 13, 13, 16));
        };
    }
}
