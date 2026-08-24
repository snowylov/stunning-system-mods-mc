package com.alex.fluidworks.device;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.fluid.FluidWorksStorage;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.block.BlockState;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Persistent, transaction-safe behavior shared by all directional fluid devices. */
public final class FluidDeviceBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    private static final int FLOWING_LEVEL = 6;
    private static final int REMOTE_LINK_RANGE = 64;

    private final FluidWorksStorage storage;
    private int operationTicks;
    private int outputCursor;
    private boolean poweredLastTick;
    private long lastTransfer;
    private Identifier filterId;

    public FluidDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.FLUID_DEVICE_BLOCK_ENTITY, pos, state);
        long capacity = state.getBlock() instanceof FluidDeviceBlock device
            ? device.kind().capacity() : FluidUnits.BUCKET_DROPLETS;
        storage = new FluidWorksStorage(this, capacity);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state,
                                  FluidDeviceBlockEntity device) {
        if (!(world instanceof ServerWorld serverWorld)
            || !(state.getBlock() instanceof FluidDeviceBlock block)) return;

        FluidDeviceKind kind = block.kind();
        boolean powered = serverWorld.isReceivingRedstonePower(pos);
        boolean risingEdge = powered && !device.poweredLastTick;
        device.poweredLastTick = powered;
        if (!state.get(FluidDeviceBlock.ENABLED)) return;

        device.operationTicks++;
        if (device.operationTicks < kind.interval()) return;
        device.operationTicks = 0;

        Direction facing = state.get(FluidDeviceBlock.FACING);
        device.lastTransfer = switch (kind) {
            case SPRINKLER -> device.runSprinkler(serverWorld, pos, facing);
            case VACUUM_DRAIN -> device.collectNearby(serverWorld, pos, facing, 2);
            case FLUID_CANNON -> risingEdge ? device.runCannon(serverWorld, pos, facing) : 0;
            case SPILL_TRAY, DRAIN_GRATE -> device.collectAt(serverWorld, pos.offset(facing));
            case PRESSURE_SENSOR -> device.adjacentAmount(serverWorld, pos, facing.getOpposite());
            case EMERGENCY_SHUTOFF -> powered ? 0
                : device.transferDirectional(serverWorld, pos, facing, kind.operationAmount());
            case SAMPLING_VALVE -> risingEdge
                ? device.transferExact(serverWorld, pos, facing, kind.operationAmount()) : 0;
            case FLUID_ROUTER -> device.route(serverWorld, pos, facing, false);
            case HEAT_EXCHANGER -> device.transferDirectional(serverWorld, pos, facing,
                kind.operationAmount());
            case FLUID_SEPARATOR -> device.route(serverWorld, pos, facing, true);
            case MIST_NOZZLE -> device.runMistNozzle(serverWorld, pos, facing);
            case PIPE_COVER -> device.transferDirectional(serverWorld, pos, facing,
                kind.operationAmount());
            case FLUID_TRAP -> (risingEdge || device.hasTriggerEntity(serverWorld, pos, facing))
                ? device.placeFlowing(serverWorld, pos.offset(facing), kind.operationAmount()) : 0;
            case REMOTE_TANK_LINK -> powered ? device.runRemoteLink(serverWorld, pos, facing) : 0;
        };
        device.markDirty();
        serverWorld.updateComparators(pos, block);
    }

    public boolean setFilterFrom(ItemStack stack) {
        if (!FluidItemComponentHelper.hasFluid(stack)) return false;
        Identifier newFilter = FluidItemComponentHelper.fluidId(stack);
        if (newFilter == null) return false;
        filterId = newFilter;
        markDirty();
        return true;
    }

    public Text statusText() {
        FluidDeviceKind kind = getCachedState().getBlock() instanceof FluidDeviceBlock block
            ? block.kind() : FluidDeviceKind.PIPE_COVER;
        String fluid = storage.variantView().isBlank() ? "minecraft:empty"
            : Registries.FLUID.getId(storage.variantView().getFluid()).toString();
        return Text.translatable("message.fluidworks.device.status", kind.id(), fluid,
            FluidUnits.dropletsToMb(storage.amountView()), FluidUnits.dropletsToMb(lastTransfer));
    }

    public int comparatorOutput(World world, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof FluidDeviceBlock block
            && block.kind() == FluidDeviceKind.PRESSURE_SENSOR) {
            Storage<FluidVariant> target = adjacent(world, pos, state.get(FluidDeviceBlock.FACING).getOpposite());
            return fullnessComparator(target);
        }
        return storage.comparatorOutput();
    }

    private long runSprinkler(ServerWorld world, BlockPos pos, Direction facing) {
        pullFrom(world, pos, facing.getOpposite(), FluidUnits.BUCKET_DROPLETS);
        BlockPos center = pos.offset(facing);
        long moved = placeFlowing(world, center, FluidUnits.mbToDroplets(250));
        if (moved == 0) return 0;
        long total = moved;
        for (Direction side : perpendicular(facing)) {
            if (storage.amountView() < FluidUnits.mbToDroplets(250)) break;
            total += placeFlowing(world, center.offset(side), FluidUnits.mbToDroplets(250));
        }
        return total;
    }

    private long runCannon(ServerWorld world, BlockPos pos, Direction facing) {
        pullFrom(world, pos, facing.getOpposite(), FluidUnits.BUCKET_DROPLETS);
        BlockPos target = pos.offset(facing);
        for (int distance = 2; distance <= 8; distance++) {
            BlockPos candidate = pos.offset(facing, distance);
            if (!world.getBlockState(candidate).isAir()) break;
            target = candidate;
        }
        return placeFlowing(world, target, FluidUnits.BUCKET_DROPLETS);
    }

    private long runMistNozzle(ServerWorld world, BlockPos pos, Direction facing) {
        pullFrom(world, pos, facing.getOpposite(), FluidUnits.mbToDroplets(250));
        long amount = extractInternal(FluidUnits.mbToDroplets(25));
        if (amount <= 0) return 0;
        double x = pos.getX() + 0.5D + facing.getOffsetX() * 0.7D;
        double y = pos.getY() + 0.5D + facing.getOffsetY() * 0.7D;
        double z = pos.getZ() + 0.5D + facing.getOffsetZ() * 0.7D;
        world.spawnParticles(ParticleTypes.SPLASH, x, y, z, 12,
            0.35D, 0.35D, 0.35D, 0.05D);
        return amount;
    }

    private long collectNearby(ServerWorld world, BlockPos pos, Direction facing, int radius) {
        BlockPos center = pos.offset(facing);
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    long collected = collectAt(world, center.add(x, y, z));
                    if (collected > 0) return collected;
                }
            }
        }
        return 0;
    }

    private long collectAt(ServerWorld world, BlockPos target) {
        FluidState fluidState = world.getFluidState(target);
        if (fluidState.isEmpty() || !(world.getBlockState(target).getBlock() instanceof FluidBlock)) return 0;
        long amount = fluidState.isStill() ? FluidUnits.BUCKET_DROPLETS : FluidUnits.mbToDroplets(250);
        Fluid fluid = fluidState.getFluid();
        if (fluid instanceof FlowableFluid flowable) fluid = flowable.getStill(false).getFluid();
        FluidVariant variant = FluidVariant.of(fluid);
        try (Transaction transaction = Transaction.openOuter()) {
            if (storage.insert(variant, amount, transaction) != amount) return 0;
            transaction.commit();
        }
        world.breakBlock(target, false);
        return amount;
    }

    private long placeFlowing(ServerWorld world, BlockPos target, long amount) {
        if (!world.getBlockState(target).isAir() || storage.variantView().isBlank()
            || !(storage.variantView().getFluid() instanceof FlowableFluid flowable)) return 0;
        long extracted = extractInternal(amount);
        if (extracted != amount) return 0;
        world.setBlockState(target, flowable.getFlowing(FLOWING_LEVEL, false).getBlockState());
        return extracted;
    }

    private long transferDirectional(ServerWorld world, BlockPos pos, Direction facing, long maximum) {
        long pulled = pullFrom(world, pos, facing.getOpposite(), maximum);
        long pushed = pushTo(world, pos, facing, maximum);
        return Math.max(pulled, pushed);
    }

    private long transferExact(ServerWorld world, BlockPos pos, Direction facing, long amount) {
        Storage<FluidVariant> source = adjacent(world, pos, facing.getOpposite());
        Storage<FluidVariant> destination = adjacent(world, pos, facing);
        if (source == null || destination == null) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = StorageUtil.move(source, destination, variant -> true, amount, transaction);
            if (moved != amount) return 0;
            transaction.commit();
            return moved;
        }
    }

    private long route(ServerWorld world, BlockPos pos, Direction facing, boolean filtered) {
        Storage<FluidVariant> source = adjacent(world, pos, facing.getOpposite());
        long pulled = filtered && filterId != null
            ? FluidTransferHelper.tryMoveMatching(source, storage, filterVariant(), FluidUnits.mbToDroplets(500))
            : FluidTransferHelper.tryMove(source, storage, FluidUnits.mbToDroplets(500));

        List<Direction> outputs = new ArrayList<>();
        outputs.add(facing);
        for (Direction direction : Direction.values()) {
            if (direction != facing && direction != facing.getOpposite()) outputs.add(direction);
        }
        long pushed = 0;
        for (int i = 0; i < outputs.size(); i++) {
            Direction output = outputs.get((outputCursor + i) % outputs.size());
            pushed = pushTo(world, pos, output, FluidUnits.mbToDroplets(500));
            if (pushed > 0) {
                outputCursor = (outputCursor + i + 1) % outputs.size();
                break;
            }
        }
        return Math.max(pulled, pushed);
    }

    private long runRemoteLink(ServerWorld world, BlockPos pos, Direction facing) {
        pullFrom(world, pos, facing.getOpposite(), FluidUnits.BUCKET_DROPLETS);
        for (int distance = 1; distance <= REMOTE_LINK_RANGE; distance++) {
            BlockPos otherPos = pos.offset(facing, distance);
            if (!world.isPosLoaded(otherPos)) break;
            if (world.getBlockEntity(otherPos) instanceof FluidDeviceBlockEntity other
                && world.getBlockState(otherPos).getBlock() instanceof FluidDeviceBlock otherBlock
                && otherBlock.kind() == FluidDeviceKind.REMOTE_TANK_LINK) {
                Direction otherFacing = world.getBlockState(otherPos).get(FluidDeviceBlock.FACING);
                Storage<FluidVariant> destination = adjacent(world, otherPos, otherFacing.getOpposite());
                return FluidTransferHelper.tryMove(storage, destination, FluidUnits.BUCKET_DROPLETS);
            }
        }
        return 0;
    }

    private boolean hasTriggerEntity(ServerWorld world, BlockPos pos, Direction facing) {
        BlockPos target = pos.offset(facing);
        return !world.getOtherEntities(null, new Box(target), entity -> true).isEmpty();
    }

    private long adjacentAmount(World world, BlockPos pos, Direction side) {
        Storage<FluidVariant> target = adjacent(world, pos, side);
        if (target == null) return 0;
        long amount = 0;
        for (var view : target) amount += view.getAmount();
        return amount;
    }

    private long pullFrom(ServerWorld world, BlockPos pos, Direction side, long maximum) {
        return FluidTransferHelper.tryMove(adjacent(world, pos, side), storage, maximum);
    }

    private long pushTo(ServerWorld world, BlockPos pos, Direction side, long maximum) {
        return FluidTransferHelper.tryMove(storage, adjacent(world, pos, side), maximum);
    }

    private long extractInternal(long amount) {
        if (amount <= 0 || storage.variantView().isBlank()) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(storage.variantView(), amount, transaction);
            if (extracted != amount) return 0;
            transaction.commit();
            return extracted;
        }
    }

    private FluidVariant filterVariant() {
        if (filterId == null) return FluidVariant.blank();
        Fluid fluid = Registries.FLUID.get(filterId);
        return fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid);
    }

    private static Storage<FluidVariant> adjacent(World world, BlockPos pos, Direction side) {
        return FluidStorage.SIDED.find(world, pos.offset(side), side.getOpposite());
    }

    private static int fullnessComparator(Storage<FluidVariant> target) {
        if (target == null) return 0;
        long amount = 0;
        long capacity = 0;
        for (var view : target) {
            amount += view.getAmount();
            capacity += view.getCapacity();
        }
        return amount <= 0 || capacity <= 0 ? 0
            : Math.max(1, Math.min(15, (int) Math.floor(amount * 15.0D / capacity)));
    }

    private static List<Direction> perpendicular(Direction facing) {
        List<Direction> result = new ArrayList<>(4);
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != facing.getAxis()) result.add(direction);
        }
        return result;
    }

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        return storage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        operationTicks = Math.max(0, view.getInt("OperationTicks", 0));
        outputCursor = Math.max(0, view.getInt("OutputCursor", 0));
        poweredLastTick = view.getBoolean("PoweredLastTick", false);
        lastTransfer = Math.max(0, view.getLong("LastTransfer", 0));
        filterId = Identifier.tryParse(view.getString("FilterId", ""));
        Identifier id = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        Fluid fluid = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        storage.load(fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid),
            view.getLong("FluidAmount", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("OperationTicks", operationTicks);
        view.putInt("OutputCursor", outputCursor);
        view.putBoolean("PoweredLastTick", poweredLastTick);
        view.putLong("LastTransfer", lastTransfer);
        if (filterId != null) view.putString("FilterId", filterId.toString());
        if (!storage.variantView().isBlank() && storage.amountView() > 0) {
            view.putString("FluidId", Registries.FLUID.getId(storage.variantView().getFluid()).toString());
            view.putLong("FluidAmount", storage.amountView());
        }
    }
}
