package com.alex.fluidworks.reservoir;

import com.alex.fluidworks.FluidWorks;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;

public final class ReservoirValveBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    private BlockPos controllerPos;

    public ReservoirValveBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.RESERVOIR_VALVE_BLOCK_ENTITY, pos, state);
    }

    public ReservoirTier tier() {
        return ((ReservoirValveBlock) getCachedState().getBlock()).tier();
    }

    public void link(BlockPos controllerPos) {
        this.controllerPos = controllerPos == null ? null : controllerPos.toImmutable();
        markDirty();
    }

    public ReservoirControllerBlockEntity controller() {
        if (world == null) return null;
        if (controllerPos != null
            && world.getBlockEntity(controllerPos) instanceof ReservoirControllerBlockEntity controller
            && controller.formed() && controller.tier() == tier()) {
            return controller;
        }
        ReservoirControllerBlockEntity found = ReservoirStructure.findController(world, pos, tier());
        if (found != null) link(found.getPos());
        return found;
    }

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        ReservoirControllerBlockEntity controller = controller();
        return controller == null ? null : controller.storage();
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        if (view.getBoolean("HasController", false)) {
            controllerPos = new BlockPos(view.getInt("ControllerX", 0),
                view.getInt("ControllerY", 0), view.getInt("ControllerZ", 0));
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (controllerPos != null) {
            view.putBoolean("HasController", true);
            view.putInt("ControllerX", controllerPos.getX());
            view.putInt("ControllerY", controllerPos.getY());
            view.putInt("ControllerZ", controllerPos.getZ());
        }
    }
}
