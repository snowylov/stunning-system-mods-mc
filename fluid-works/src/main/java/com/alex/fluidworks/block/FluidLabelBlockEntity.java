package com.alex.fluidworks.block;

import com.alex.fluidworks.FluidWorks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class FluidLabelBlockEntity extends BlockEntity {
    private Identifier fluidId;

    public FluidLabelBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.FLUID_LABEL_BLOCK_ENTITY, pos, state);
    }

    public Identifier fluidId() {
        return fluidId;
    }

    public void setFluidId(Identifier fluidId) {
        this.fluidId = fluidId;
        markDirty();
    }

    public void clearLabel() {
        fluidId = null;
        markDirty();
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        fluidId = Identifier.tryParse(view.getString("FluidId", ""));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (fluidId != null) view.putString("FluidId", fluidId.toString());
    }
}
