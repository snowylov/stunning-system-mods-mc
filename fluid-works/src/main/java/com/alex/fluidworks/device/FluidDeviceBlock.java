package com.alex.fluidworks.device;

import com.alex.fluidworks.FluidWorks;
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
import net.minecraft.item.ItemStack;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** One directional block implementation shared by the purpose-built fluid machines. */
public final class FluidDeviceBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty ENABLED = BooleanProperty.of("enabled");

    private final FluidDeviceKind kind;
    private final MapCodec<FluidDeviceBlock> codec = MapCodec.unit(this);

    public FluidDeviceBlock(FluidDeviceKind kind, Settings settings) {
        super(settings);
        this.kind = kind;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(ENABLED, true));
    }

    public FluidDeviceKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getSide());
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return rotate(state, mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        return rotateFromNorth(baseShape(kind), state.get(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidDeviceBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, FluidWorks.FLUID_DEVICE_BLOCK_ENTITY,
            FluidDeviceBlockEntity::serverTick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient()) {
            if (player.isSneaking() && world.getBlockEntity(pos) instanceof FluidDeviceBlockEntity device) {
                player.sendMessage(device.statusText(), true);
            } else {
                boolean enabled = !state.get(ENABLED);
                world.setBlockState(pos, state.with(ENABLED, enabled), Block.NOTIFY_ALL);
                player.sendMessage(Text.translatable(enabled
                    ? "message.fluidworks.device.enabled" : "message.fluidworks.device.disabled"), true);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof FluidDeviceBlockEntity device)) return ActionResult.PASS;
        if (kind == FluidDeviceKind.FLUID_SEPARATOR && FluidItemComponentHelper.hasFluid(stack)
            && (world.isClient() || device.setFilterFrom(stack))) {
            if (!world.isClient()) player.sendMessage(device.statusText(), true);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof FluidDeviceBlockEntity device
            ? device.comparatorOutput(world, pos, state) : 0;
    }

    private static VoxelShape core() {
        return createCuboidShape(3, 3, 3, 13, 13, 13);
    }

    private static VoxelShape baseShape(FluidDeviceKind kind) {
        return switch (kind) {
            case SPRINKLER -> boxes(
                box(3, 3, 12, 13, 13, 16), box(6, 6, 4, 10, 10, 12), box(3, 3, 1, 13, 13, 4));
            case VACUUM_DRAIN -> boxes(
                box(2, 2, 0, 14, 14, 4), box(5, 5, 4, 11, 11, 16),
                box(0, 0, 0, 3, 16, 2), box(13, 0, 0, 16, 16, 2));
            case FLUID_CANNON -> boxes(
                box(3, 3, 6, 13, 13, 15), box(5, 5, 0, 11, 11, 8), box(4, 4, 0, 12, 12, 2));
            case SPILL_TRAY -> boxes(
                box(1, 1, 13, 15, 15, 16), box(0, 0, 12, 2, 16, 16),
                box(14, 0, 12, 16, 16, 16), box(2, 0, 12, 14, 2, 16),
                box(2, 14, 12, 14, 16, 16));
            case PRESSURE_SENSOR -> boxes(
                box(2, 2, 13, 14, 14, 16), box(5, 5, 10, 11, 11, 13), box(7, 2, 9, 9, 8, 10));
            case EMERGENCY_SHUTOFF -> boxes(
                box(5, 5, 0, 11, 11, 16), box(2, 2, 5, 14, 14, 11), box(7, 0, 7, 9, 16, 9));
            case SAMPLING_VALVE -> boxes(
                box(5, 5, 0, 11, 11, 16), box(3, 3, 6, 13, 13, 10), box(7, 0, 7, 9, 7, 9));
            case FLUID_ROUTER -> boxes(
                box(3, 3, 3, 13, 13, 13), box(5, 5, 0, 11, 11, 16), box(0, 5, 5, 16, 11, 11));
            case HEAT_EXCHANGER -> boxes(
                box(2, 2, 3, 14, 14, 13), box(4, 4, 0, 7, 7, 16),
                box(9, 9, 0, 12, 12, 16), box(1, 1, 6, 15, 15, 10));
            case FLUID_SEPARATOR -> boxes(
                box(2, 2, 4, 14, 14, 14), box(5, 5, 0, 11, 11, 5),
                box(0, 5, 8, 16, 11, 12), box(7, 0, 7, 9, 16, 9));
            case MIST_NOZZLE -> boxes(
                box(4, 4, 12, 12, 12, 16), box(6, 6, 3, 10, 10, 12), box(4, 4, 1, 12, 12, 4));
            case DRAIN_GRATE -> boxes(
                box(1, 1, 13, 15, 3, 16), box(1, 5, 13, 15, 7, 16),
                box(1, 9, 13, 15, 11, 16), box(1, 13, 13, 15, 15, 16),
                box(6, 6, 10, 10, 10, 13));
            case PIPE_COVER -> boxes(
                box(2, 2, 13, 14, 5, 16), box(2, 11, 13, 14, 14, 16),
                box(2, 5, 13, 5, 11, 16), box(11, 5, 13, 14, 11, 16),
                box(6, 6, 0, 10, 10, 13));
            case FLUID_TRAP -> boxes(
                box(2, 2, 12, 14, 14, 16), box(4, 4, 5, 12, 12, 12), box(6, 6, 1, 10, 10, 5));
            case REMOTE_TANK_LINK -> boxes(
                box(3, 3, 3, 13, 13, 13), box(6, 6, 0, 10, 10, 16),
                box(1, 7, 7, 15, 9, 9), box(7, 1, 7, 9, 15, 9));
        };
    }

    private static VoxelShape box(double minX, double minY, double minZ,
                                  double maxX, double maxY, double maxZ) {
        return createCuboidShape(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static VoxelShape boxes(VoxelShape... boxes) {
        return VoxelShapes.union(boxes[0], java.util.Arrays.copyOfRange(boxes, 1, boxes.length));
    }

    private static VoxelShape rotateFromNorth(VoxelShape northShape, Direction facing) {
        if (facing == Direction.NORTH) return northShape;
        VoxelShape[] result = {VoxelShapes.empty()};
        northShape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double[][] corners = {
                {minX, minY, minZ}, {minX, minY, maxZ}, {minX, maxY, minZ}, {minX, maxY, maxZ},
                {maxX, minY, minZ}, {maxX, minY, maxZ}, {maxX, maxY, minZ}, {maxX, maxY, maxZ}
            };
            double outMinX = 1, outMinY = 1, outMinZ = 1;
            double outMaxX = 0, outMaxY = 0, outMaxZ = 0;
            for (double[] corner : corners) {
                double[] transformed = transform(corner[0], corner[1], corner[2], facing);
                outMinX = Math.min(outMinX, transformed[0]);
                outMinY = Math.min(outMinY, transformed[1]);
                outMinZ = Math.min(outMinZ, transformed[2]);
                outMaxX = Math.max(outMaxX, transformed[0]);
                outMaxY = Math.max(outMaxY, transformed[1]);
                outMaxZ = Math.max(outMaxZ, transformed[2]);
            }
            result[0] = VoxelShapes.union(result[0], createCuboidShape(
                outMinX * 16, outMinY * 16, outMinZ * 16,
                outMaxX * 16, outMaxY * 16, outMaxZ * 16));
        });
        return result[0];
    }

    private static double[] transform(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[] {x, y, z};
            case EAST -> new double[] {1 - z, y, x};
            case SOUTH -> new double[] {1 - x, y, 1 - z};
            case WEST -> new double[] {z, y, 1 - x};
            case UP -> new double[] {x, z, 1 - y};
            case DOWN -> new double[] {x, 1 - z, y};
        };
    }

    private static VoxelShape tube(Direction direction, double alongMin, double alongMax,
                                   double crossMin, double crossMax) {
        return switch (direction.getAxis()) {
            case X -> createCuboidShape(alongMin, crossMin, crossMin, alongMax, crossMax, crossMax);
            case Y -> createCuboidShape(crossMin, alongMin, crossMin, crossMax, alongMax, crossMax);
            case Z -> createCuboidShape(crossMin, crossMin, alongMin, crossMax, crossMax, alongMax);
        };
    }

    private static VoxelShape crossTube(Direction facing) {
        return switch (facing.getAxis()) {
            case X -> createCuboidShape(5, 0, 5, 11, 16, 11);
            case Y -> createCuboidShape(0, 5, 5, 16, 11, 11);
            case Z -> createCuboidShape(0, 5, 5, 16, 11, 11);
        };
    }

    private static VoxelShape backPlate(Direction facing, double depth) {
        return switch (facing) {
            case NORTH -> createCuboidShape(1, 1, 16 - depth, 15, 15, 16);
            case SOUTH -> createCuboidShape(1, 1, 0, 15, 15, depth);
            case WEST -> createCuboidShape(16 - depth, 1, 1, 16, 15, 15);
            case EAST -> createCuboidShape(0, 1, 1, depth, 15, 15);
            case UP -> createCuboidShape(1, 0, 1, 15, depth, 15);
            case DOWN -> createCuboidShape(1, 16 - depth, 1, 15, 16, 15);
        };
    }

    private static VoxelShape rim(Direction facing) {
        VoxelShape plate = backPlate(facing, 4);
        return VoxelShapes.union(plate, tube(facing, 3, 6, 1, 3), tube(facing, 3, 6, 13, 15));
    }
}
