package com.alex.fluidworks.thermal;

import com.alex.fluidworks.fluid.FluidWorksStorage;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** One-bucket thermal buffer with a fixed 250 mB/t directional transfer. */
public final class HeaterPipeBlockEntity extends BlockEntity
        implements FluidStorageBlockEntity, ThermalFluidCarrier {
    public static final long TRANSFER_RATE = FluidUnits.mbToDroplets(250);
    public static final long CAPACITY = FluidUnits.BUCKET_DROPLETS;

    private final FluidWorksStorage storage = new FluidWorksStorage(this, CAPACITY);
    private int fluidTemperature = ThermalApiBridge.STANDARD;

    public HeaterPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ThermalContent.HEATER_PIPE_BLOCK_ENTITY, pos, state);
    }

    public FluidWorksStorage storage() { return storage; }
    @Override public Storage<FluidVariant> liquidFabricStorage() { return storage; }
    @Override public int fluidTemperature() { return fluidTemperature; }

    @Override
    public void receiveTemperature(int temperature, long movedAmount) {
        if (movedAmount <= 0) return;
        long before = Math.max(0, storage.amountView() - movedAmount);
        if (before == 0) fluidTemperature = temperature;
        else fluidTemperature = (int) Math.round(
            ((double) fluidTemperature * before + (double) temperature * movedAmount) / (before + movedAmount));
        markDirty();
    }

    @Override
    public void approachTemperature(int target, int maximumStep) {
        int difference = target - fluidTemperature;
        if (difference == 0 || maximumStep <= 0) return;
        fluidTemperature += Math.max(-maximumStep, Math.min(maximumStep, difference));
        markDirty();
    }

    public static void serverTick(World world, BlockPos pos, BlockState state,
                                  HeaterPipeBlockEntity pipe) {
        if (!(world instanceof ServerWorld serverWorld)
            || !(state.getBlock() instanceof HeaterPipeBlock block)) return;

        Direction output = state.get(HeaterPipeBlock.FACING);
        Direction input = output.getOpposite();
        BlockPos inputPos = pos.offset(input);
        Storage<FluidVariant> source = FluidStorage.SIDED.find(serverWorld, inputPos, output);
        long pulled = source == null ? 0 : FluidTransferHelper.tryMove(source, pipe.storage, TRANSFER_RATE);
        if (pulled > 0) {
            pipe.receiveTemperature(temperatureAt(serverWorld, inputPos), pulled);
        }

        BlockPos outputPos = pos.offset(output);
        Storage<FluidVariant> destination = FluidStorage.SIDED.find(serverWorld, outputPos, input);
        long pushed = destination == null ? 0
            : FluidTransferHelper.tryMove(pipe.storage, destination, TRANSFER_RATE);
        if (pushed > 0 && serverWorld.getBlockEntity(outputPos) instanceof ThermalFluidCarrier carrier) {
            carrier.receiveTemperature(pipe.fluidTemperature, pushed);
        }

        int step = block.material().conductionStep();
        pipe.approachTemperature(temperatureAt(serverWorld, inputPos), step);
        pipe.approachTemperature(temperatureAt(serverWorld, outputPos), step);
        if (pipe.storage.amountView() <= 0) pipe.approachTemperature(ThermalApiBridge.STANDARD, step);
        ThermalApiBridge.setBlockTemperature(serverWorld, pos, pipe.fluidTemperature);
    }

    private static int temperatureAt(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof ThermalFluidCarrier carrier) {
            return carrier.fluidTemperature();
        }
        return ThermalApiBridge.getBlockTemperature(world, pos);
    }

    @Override protected void readData(ReadView view) {
        super.readData(view);
        Identifier id = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        Fluid fluid = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        storage.load(fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid),
            view.getLong("FluidAmount", 0));
        fluidTemperature = view.getInt("FluidTemperature", ThermalApiBridge.STANDARD);
    }

    @Override protected void writeData(WriteView view) {
        super.writeData(view);
        if (!storage.variantView().isBlank() && storage.amountView() > 0) {
            view.putString("FluidId", Registries.FLUID.getId(storage.variantView().getFluid()).toString());
            view.putLong("FluidAmount", storage.amountView());
        }
        if (fluidTemperature != ThermalApiBridge.STANDARD) {
            view.putInt("FluidTemperature", fluidTemperature);
        }
    }
}
