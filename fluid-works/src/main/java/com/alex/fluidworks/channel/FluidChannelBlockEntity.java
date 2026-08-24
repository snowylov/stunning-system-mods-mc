package com.alex.fluidworks.channel;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.fluid.FluidWorksStorage;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
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

public final class FluidChannelBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    private final FluidWorksStorage storage = new FluidWorksStorage(this, FluidChannelBlock.CAPACITY);

    public FluidChannelBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.FLUID_CHANNEL_BLOCK_ENTITY, pos, state);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state,
                                  FluidChannelBlockEntity channel) {
        if (!(world instanceof ServerWorld serverWorld)
            || !(state.getBlock() instanceof FluidChannelBlock)) return;
        Direction output = state.get(FluidChannelBlock.FACING);
        Direction input = output.getOpposite();
        Storage<FluidVariant> source = FluidStorage.SIDED.find(serverWorld, pos.offset(input), output);
        Storage<FluidVariant> destination = FluidStorage.SIDED.find(serverWorld, pos.offset(output), input);
        FluidTransferHelper.tryMove(source, channel.storage, FluidChannelBlock.TRANSFER_RATE);
        FluidTransferHelper.tryMove(channel.storage, destination, FluidChannelBlock.TRANSFER_RATE);

        boolean filled = channel.storage.amountView() > 0;
        if (state.get(FluidChannelBlock.FILLED) != filled) {
            serverWorld.setBlockState(pos, state.with(FluidChannelBlock.FILLED, filled), Block.NOTIFY_LISTENERS);
        }
    }

    public FluidWorksStorage storage() {
        return storage;
    }

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        return storage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Identifier id = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        Fluid fluid = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        storage.load(fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid),
            view.getLong("FluidAmount", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (!storage.variantView().isBlank() && storage.amountView() > 0) {
            view.putString("FluidId", Registries.FLUID.getId(storage.variantView().getFluid()).toString());
            view.putLong("FluidAmount", storage.amountView());
        }
    }
}
