package com.alex.fluidworks.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import com.alex.fluidworks.StorageCasesContent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** A retained-content, suitcase-shaped 48-slot storage block. */
public final class PortableCaseBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = Properties.OPEN;
    private static final VoxelShape NORTH_SHAPE = VoxelShapes.union(
        createCuboidShape(0, 0, 5, 16, 14, 11),
        createCuboidShape(6, 14, 6.5, 7, 16, 9.5),
        createCuboidShape(9, 14, 6.5, 10, 16, 9.5),
        createCuboidShape(7, 15, 6.5, 9, 16, 9.5));

    private final MapCodec<PortableCaseBlock> codec = MapCodec.unit(this);
    private final boolean foodOnly;
    private final boolean showsItems;

    public PortableCaseBlock(boolean foodOnly, boolean showsItems, Settings settings) {
        super(settings);
        this.foodOnly = foodOnly;
        this.showsItems = showsItems;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(OPEN, false));
    }

    public boolean foodOnly() { return foodOnly; }
    public boolean showsItems() { return showsItems; }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return codec; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }
    @Override public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }
    @Override protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PortableCaseBlockEntity(pos, state);
    }
    @Override public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,
            BlockState state, BlockEntityType<T> type) {
        return world.isClient() ? validateTicker(type, StorageCasesContent.PORTABLE_CASE_BLOCK_ENTITY,
            PortableCaseBlockEntity::clientTick) : null;
    }

    @Override protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                           PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof PortableCaseBlockEntity storage) {
            CaseMenuInventory menu = new CaseMenuInventory(storage);
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, ignored) ->
                GenericContainerScreenHandler.createGeneric9x6(syncId, playerInventory, menu),
                Text.translatable(getTranslationKey())));
        }
        return ActionResult.SUCCESS;
    }

    @Override protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (!moved && world.getBlockEntity(pos) instanceof PortableCaseBlockEntity storage) {
            ItemStack retained = new ItemStack(asItem());
            retained.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(
                foodOnly ? storage.prototypeStacks() : storage.stacksView()));
            if (foodOnly) {
                NbtCompound custom = new NbtCompound();
                custom.putIntArray("FluidWorksCaseCounts", storage.counts());
                retained.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(custom));
            }
            dropStack(world, pos, retained);
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                                    ShapeContext context) {
        return rotateShape(NORTH_SHAPE, state.get(FACING));
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                                      ShapeContext context) {
        return rotateShape(NORTH_SHAPE, state.get(FACING));
    }

    private static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        VoxelShape result = shape;
        int turns = switch (facing) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
        for (int i = 0; i < turns; i++) {
            VoxelShape next = VoxelShapes.empty();
            for (var box : result.getBoundingBoxes()) {
                next = VoxelShapes.union(next, createCuboidShape(
                    16 - box.maxZ * 16, box.minY * 16, box.minX * 16,
                    16 - box.minZ * 16, box.maxY * 16, box.maxX * 16));
            }
            result = next;
        }
        return result;
    }
}
