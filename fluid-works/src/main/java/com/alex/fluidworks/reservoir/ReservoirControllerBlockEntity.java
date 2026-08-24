package com.alex.fluidworks.reservoir;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.fluid.FluidWorksStorage;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ReservoirControllerBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    private final FluidWorksStorage storage = new FluidWorksStorage(this, 0);
    private boolean formed;
    private BlockPos minimum;
    private BlockPos maximum;
    private int interiorBlocks;

    public ReservoirControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.RESERVOIR_CONTROLLER_BLOCK_ENTITY, pos, state);
    }

    public ReservoirTier tier() {
        return ((ReservoirControllerBlock) getCachedState().getBlock()).tier();
    }

    public FluidWorksStorage storage() {
        return storage;
    }

    public boolean formed() {
        return formed;
    }

    public int interiorBlocks() {
        return interiorBlocks;
    }

    public BlockPos minimum() {
        return minimum;
    }

    public BlockPos maximum() {
        return maximum;
    }

    public boolean rebuild() {
        return ReservoirStructure.validate(this);
    }

    void applyStructure(boolean formed, BlockPos minimum, BlockPos maximum, int interiorBlocks,
                        long capacity) {
        this.formed = formed;
        this.minimum = minimum;
        this.maximum = maximum;
        this.interiorBlocks = formed ? interiorBlocks : 0;
        storage.setCapacity(formed ? capacity : storage.amountView());
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        return formed ? storage : null;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        formed = view.getBoolean("Formed", false);
        interiorBlocks = Math.max(0, view.getInt("InteriorBlocks", 0));
        long savedCapacity = Math.max(0, view.getLong("Capacity", 0));
        storage.setCapacity(savedCapacity);
        Identifier id = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        Fluid fluid = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        storage.load(fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid),
            view.getLong("FluidAmount", 0));
        if (view.getBoolean("HasBounds", false)) {
            minimum = new BlockPos(view.getInt("MinX", 0), view.getInt("MinY", 0), view.getInt("MinZ", 0));
            maximum = new BlockPos(view.getInt("MaxX", 0), view.getInt("MaxY", 0), view.getInt("MaxZ", 0));
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putBoolean("Formed", formed);
        view.putInt("InteriorBlocks", interiorBlocks);
        view.putLong("Capacity", storage.getCapacityView());
        if (minimum != null && maximum != null) {
            view.putBoolean("HasBounds", true);
            view.putInt("MinX", minimum.getX());
            view.putInt("MinY", minimum.getY());
            view.putInt("MinZ", minimum.getZ());
            view.putInt("MaxX", maximum.getX());
            view.putInt("MaxY", maximum.getY());
            view.putInt("MaxZ", maximum.getZ());
        }
        if (!storage.variantView().isBlank() && storage.amountView() > 0) {
            view.putString("FluidId", Registries.FLUID.getId(storage.variantView().getFluid()).toString());
            view.putLong("FluidAmount", storage.amountView());
        }
    }
}
