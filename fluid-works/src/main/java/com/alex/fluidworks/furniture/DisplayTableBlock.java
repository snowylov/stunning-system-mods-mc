package com.alex.fluidworks.furniture;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

/** A two-dimensional, at-most-3x3 table surface whose group retains four outer legs. */
public final class DisplayTableBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty NORTH = BooleanProperty.of("north");
    public static final BooleanProperty EAST = BooleanProperty.of("east");
    public static final BooleanProperty SOUTH = BooleanProperty.of("south");
    public static final BooleanProperty WEST = BooleanProperty.of("west");
    private final FurnitureKind kind;
    private final MapCodec<DisplayTableBlock> codec = MapCodec.unit(this);

    public DisplayTableBlock(FurnitureKind kind, Settings settings) {
        super(settings);
        this.kind = kind;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH)
            .with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false));
    }

    public FurnitureKind kind() { return kind; }
    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return codec; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, NORTH, EAST, SOUTH, WEST);
    }

    @Override public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos pos = context.getBlockPos();
        if (wouldExceedThree(context.getWorld(), pos)) return null;
        return connections(getDefaultState().with(FACING,
            context.getHorizontalPlayerFacing().getOpposite()), context.getWorld(), pos);
    }

    @Override protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world,
            ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState, Random random) {
        return direction.getAxis().isHorizontal() ? connections(state, world, pos) : state;
    }

    private BlockState connections(BlockState state, BlockView world, BlockPos pos) {
        return state.with(NORTH, world.getBlockState(pos.north()).isOf(this))
            .with(EAST, world.getBlockState(pos.east()).isOf(this))
            .with(SOUTH, world.getBlockState(pos.south()).isOf(this))
            .with(WEST, world.getBlockState(pos.west()).isOf(this));
    }

    private boolean wouldExceedThree(BlockView world, BlockPos pos) {
        int minX = pos.getX(), maxX = pos.getX(), minZ = pos.getZ(), maxZ = pos.getZ();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        queue.add(pos); seen.add(pos);
        while (!queue.isEmpty()) {
            BlockPos here = queue.removeFirst();
            minX = Math.min(minX, here.getX()); maxX = Math.max(maxX, here.getX());
            minZ = Math.min(minZ, here.getZ()); maxZ = Math.max(maxZ, here.getZ());
            for (Direction d : Direction.Type.HORIZONTAL) {
                BlockPos next = here.offset(d);
                if (seen.add(next) && world.getBlockState(next).isOf(this)) queue.add(next);
            }
        }
        return maxX - minX >= 3 || maxZ - minZ >= 3;
    }

    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayTableBlockEntity(pos, state);
    }

    @Override protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world,
            BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (hit.getSide() != Direction.UP || !(world.getBlockEntity(pos) instanceof DisplayTableBlockEntity table))
            return ActionResult.PASS;
        int slot = stack.getItem() instanceof FilledMapItem ? DisplayTableBlockEntity.MAP_SLOT
            : stack.contains(DataComponentTypes.FOOD) ? DisplayTableBlockEntity.FOOD_SLOT : -1;
        if (slot < 0 || !table.getStack(slot).isEmpty()) return ActionResult.PASS;
        if (!world.isClient()) {
            table.setStack(slot, stack.copyWithCount(1));
            if (!player.isInCreativeMode()) stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
            PlayerEntity player, BlockHitResult hit) {
        if (hit.getSide() != Direction.UP || !(world.getBlockEntity(pos) instanceof DisplayTableBlockEntity table))
            return ActionResult.PASS;
        int slot = !table.getStack(DisplayTableBlockEntity.FOOD_SLOT).isEmpty()
            ? DisplayTableBlockEntity.FOOD_SLOT : DisplayTableBlockEntity.MAP_SLOT;
        if (table.getStack(slot).isEmpty()) return ActionResult.PASS;
        if (!world.isClient()) player.getInventory().offerOrDrop(table.removeStack(slot));
        return ActionResult.SUCCESS;
    }

    @Override protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!moved && world.getBlockEntity(pos) instanceof DisplayTableBlockEntity table)
            ItemScatterer.spawn(world, pos, table);
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
            ShapeContext context) { return shape(state); }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
            ShapeContext context) { return shape(state); }

    private static VoxelShape shape(BlockState state) {
        VoxelShape result = createCuboidShape(0, 12, 0, 16, 16, 16);
        if (!state.get(NORTH) && !state.get(WEST)) result = VoxelShapes.union(result, createCuboidShape(1,0,1,4,12,4));
        if (!state.get(NORTH) && !state.get(EAST)) result = VoxelShapes.union(result, createCuboidShape(12,0,1,15,12,4));
        if (!state.get(SOUTH) && !state.get(WEST)) result = VoxelShapes.union(result, createCuboidShape(1,0,12,4,12,15));
        if (!state.get(SOUTH) && !state.get(EAST)) result = VoxelShapes.union(result, createCuboidShape(12,0,12,15,12,15));
        return result;
    }
}
