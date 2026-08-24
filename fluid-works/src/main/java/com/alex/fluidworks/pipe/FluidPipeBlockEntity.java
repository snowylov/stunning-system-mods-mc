package com.alex.fluidworks.pipe;

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
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Shared, transaction-safe runtime for all Fluid Works pipe variants. */
public final class FluidPipeBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    private static final int METER_WINDOW_TICKS = 20;
    private static final double OVERFLOW_THRESHOLD = 0.75D;

    private final FluidWorksStorage storage;
    private int sideCursor;
    private Identifier filterId;
    private boolean poweredLastTick;
    private long meterWindowMoved;
    private long measuredPerTick;
    private int meterTicks;

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.FLUID_PIPE_BLOCK_ENTITY, pos, state);
        long capacity = state.getBlock() instanceof FluidPipeBlock pipe
            ? pipe.kind().bufferCapacity() : FluidUnits.BUCKET_DROPLETS;
        storage = new FluidWorksStorage(this, capacity);
    }

    public FluidWorksStorage storage() {
        return storage;
    }

    public long measuredRateMb() {
        return FluidUnits.dropletsToMb(measuredPerTick);
    }

    public int meterComparatorOutput(long maximumRate) {
        if (maximumRate <= 0 || measuredPerTick <= 0) return 0;
        return Math.max(1, Math.min(15,
            (int) Math.floor((double) measuredPerTick * 15.0D / maximumRate)));
    }

    public boolean setFilterFrom(ItemStack stack) {
        if (!FluidItemComponentHelper.hasFluid(stack)) return false;
        filterId = FluidItemComponentHelper.fluidId(stack);
        markDirty();
        return true;
    }

    public void clearFilter() {
        filterId = null;
        markDirty();
    }

    public Text filterName() {
        return filterId == null
            ? Text.translatable("message.fluidworks.pipe.filter_empty")
            : Text.translatable("message.fluidworks.pipe.filter_set", filterId.toString());
    }

    public static void serverTick(World world, BlockPos pos, BlockState state,
                                  FluidPipeBlockEntity pipe) {
        if (!(world instanceof ServerWorld serverWorld)
            || !(state.getBlock() instanceof FluidPipeBlock block)) return;

        boolean powered = serverWorld.isReceivingRedstonePower(pos);
        boolean active = state.get(FluidPipeBlock.ENABLED)
            && state.get(FluidPipeBlock.REDSTONE_MODE).permits(powered);
        long moved = 0;

        if (active) {
            moved = switch (block.kind()) {
                case EXTRACTION, DIODE -> pipe.transferDirectional(serverWorld, pos, state, block);
                case PULSE -> !pipe.poweredLastTick && powered
                    ? pipe.transferPulse(serverWorld, pos, state, block.transferRate()) : 0;
                case OVERFLOW -> pipe.transferOverflow(serverWorld, pos, state, block);
                default -> pipe.transferJunction(serverWorld, pos, state, block);
            };
        }

        pipe.poweredLastTick = powered;
        if (block.kind() == PipeKind.METER) pipe.updateMeter(moved);
    }

    private long transferDirectional(ServerWorld world, BlockPos pos, BlockState state,
                                     FluidPipeBlock block) {
        Direction output = state.get(FluidPipeBlock.FACING);
        Direction input = output.getOpposite();
        long pulled = pull(adjacent(world, pos, input), block.transferRate());
        long pushed = push(adjacent(world, pos, output), block.transferRate());
        return Math.max(pulled, pushed);
    }

    private long transferPulse(ServerWorld world, BlockPos pos, BlockState state, long amount) {
        Direction output = state.get(FluidPipeBlock.FACING);
        Storage<FluidVariant> source = adjacent(world, pos, output.getOpposite());
        Storage<FluidVariant> destination = adjacent(world, pos, output);
        if (source == null || destination == null) return 0;
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = StorageUtil.move(source, destination, this::matchesFilter, amount, transaction);
            if (moved != amount) return 0;
            transaction.commit();
            return moved;
        }
    }

    private long transferOverflow(ServerWorld world, BlockPos pos, BlockState state,
                                  FluidPipeBlock block) {
        Direction preferred = state.get(FluidPipeBlock.FACING).getOpposite();
        Storage<FluidVariant> source = adjacent(world, pos, preferred);
        long pulled = fullness(source) >= OVERFLOW_THRESHOLD ? pull(source, block.transferRate()) : 0;
        if (pulled == 0) {
            for (Direction side : orderedSides(preferred)) {
                source = adjacent(world, pos, side);
                if (fullness(source) >= OVERFLOW_THRESHOLD) {
                    pulled = pull(source, block.transferRate());
                    preferred = side;
                    break;
                }
            }
        }
        long pushed = pushOutputs(world, pos, state, block, preferred);
        return Math.max(pulled, pushed);
    }

    private long transferJunction(ServerWorld world, BlockPos pos, BlockState state,
                                  FluidPipeBlock block) {
        long pulled = 0;
        Direction pulledFrom = null;
        if (storage.amountView() < storage.getCapacityView()) {
            Direction reservedOutput = block.kind() == PipeKind.PRIORITY
                ? state.get(FluidPipeBlock.FACING) : null;
            for (Direction side : orderedSides(reservedOutput)) {
                long moved = pull(adjacent(world, pos, side), block.transferRate());
                if (moved > 0) {
                    pulled = moved;
                    pulledFrom = side;
                    break;
                }
            }
        }
        long pushed = pushOutputs(world, pos, state, block, pulledFrom);
        sideCursor = (sideCursor + 1) % Direction.values().length;
        return Math.max(pulled, pushed);
    }

    private long pushOutputs(ServerWorld world, BlockPos pos, BlockState state,
                             FluidPipeBlock block, Direction excluded) {
        long budget = block.transferRate();
        long movedTotal = 0;
        List<Direction> outputs = new ArrayList<>();
        if (block.kind() == PipeKind.PRIORITY) outputs.add(state.get(FluidPipeBlock.FACING));
        for (Direction side : orderedSides(excluded)) {
            if (!outputs.contains(side)) outputs.add(side);
        }
        for (Direction side : outputs) {
            if (side == excluded || budget <= 0) continue;
            long moved = push(adjacent(world, pos, side), budget);
            movedTotal += moved;
            budget -= moved;
        }
        return movedTotal;
    }

    private List<Direction> orderedSides(Direction excluded) {
        List<Direction> sides = new ArrayList<>(Direction.values().length);
        for (int i = 0; i < Direction.values().length; i++) {
            Direction side = Direction.values()[(sideCursor + i) % Direction.values().length];
            if (side != excluded) sides.add(side);
        }
        return sides;
    }

    private long pull(Storage<FluidVariant> source, long maximum) {
        if (source == null || maximum <= 0) return 0;
        FluidVariant filter = filterVariant();
        return filter.isBlank()
            ? FluidTransferHelper.tryMove(source, storage, maximum)
            : FluidTransferHelper.tryMoveMatching(source, storage, filter, maximum);
    }

    private long push(Storage<FluidVariant> destination, long maximum) {
        return FluidTransferHelper.tryMove(storage, destination, maximum);
    }

    private boolean matchesFilter(FluidVariant variant) {
        return filterId == null || Registries.FLUID.getId(variant.getFluid()).equals(filterId);
    }

    private FluidVariant filterVariant() {
        if (filterId == null) return FluidVariant.blank();
        Fluid fluid = Registries.FLUID.get(filterId);
        return fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid);
    }

    private static double fullness(Storage<FluidVariant> storage) {
        if (storage == null) return 0;
        long amount = 0;
        long capacity = 0;
        for (var view : storage) {
            amount += view.getAmount();
            capacity += view.getCapacity();
        }
        return capacity <= 0 ? 0 : (double) amount / capacity;
    }

    private static Storage<FluidVariant> adjacent(ServerWorld world, BlockPos pos, Direction side) {
        return FluidStorage.SIDED.find(world, pos.offset(side), side.getOpposite());
    }

    private void updateMeter(long moved) {
        meterWindowMoved += moved;
        meterTicks++;
        if (meterTicks >= METER_WINDOW_TICKS) {
            measuredPerTick = meterWindowMoved / METER_WINDOW_TICKS;
            meterWindowMoved = 0;
            meterTicks = 0;
            markDirty();
        }
    }

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        return storage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        sideCursor = Math.floorMod(view.getInt("SideCursor", 0), Direction.values().length);
        filterId = Identifier.tryParse(view.getString("FilterId", ""));
        poweredLastTick = view.getBoolean("PoweredLastTick", false);
        measuredPerTick = Math.max(0, view.getLong("MeasuredPerTick", 0));
        Identifier id = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        Fluid fluid = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        storage.load(fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid),
            view.getLong("FluidAmount", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("SideCursor", sideCursor);
        view.putBoolean("PoweredLastTick", poweredLastTick);
        view.putLong("MeasuredPerTick", measuredPerTick);
        if (filterId != null) view.putString("FilterId", filterId.toString());
        if (!storage.variantView().isBlank() && storage.amountView() > 0) {
            view.putString("FluidId", Registries.FLUID.getId(storage.variantView().getFluid()).toString());
            view.putLong("FluidAmount", storage.amountView());
        }
    }
}
