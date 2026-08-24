package com.alex.fluidworks.machine;

import com.alex.fluidworks.ExpandedContent;
import com.alex.fluidworks.fluid.FluidWorksStorage;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Iterator;

public final class CoolingCauldronBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    public static final int COOLING_TICKS = 200;
    private final FluidWorksStorage storage = new FluidWorksStorage(this, FluidUnits.BUCKET_DROPLETS);
    private final Storage<FluidVariant> roseGoldOnly = new Storage<>() {
        @Override public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return resource != null && resource.equals(FluidVariant.of(ExpandedContent.ROSE_GOLD_FLUID.still))
                ? storage.insert(resource, maxAmount, transaction) : 0;
        }
        @Override public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return storage.extract(resource, maxAmount, transaction);
        }
        @Override public Iterator<StorageView<FluidVariant>> iterator() { return storage.iterator(); }
    };
    private int coolingTicks;

    public CoolingCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ExpandedContent.COOLING_CAULDRON_BLOCK_ENTITY, pos, state);
    }

    public Storage<FluidVariant> storage() { return roseGoldOnly; }
    @Override public Storage<FluidVariant> liquidFabricStorage() { return roseGoldOnly; }

    public static void serverTick(ServerWorld world, BlockPos pos, BlockState state,
                                  CoolingCauldronBlockEntity cauldron) {
        FluidVariant roseGold = FluidVariant.of(ExpandedContent.ROSE_GOLD_FLUID.still);
        if (!cauldron.storage.variantView().equals(roseGold)
            || cauldron.storage.amountView() < FluidUnits.BUCKET_DROPLETS) {
            if (cauldron.coolingTicks != 0) { cauldron.coolingTicks = 0; cauldron.markDirty(); }
            return;
        }
        if (++cauldron.coolingTicks < COOLING_TICKS) return;
        try (Transaction transaction = Transaction.openOuter()) {
            if (cauldron.storage.extract(roseGold, FluidUnits.BUCKET_DROPLETS, transaction)
                != FluidUnits.BUCKET_DROPLETS) return;
            transaction.commit();
        }
        cauldron.coolingTicks = 0;
        cauldron.markDirty();
        Block.dropStack(world, pos.up(), new ItemStack(ExpandedContent.ROSE_GOLD_BLOCK));
    }

    @Override protected void readData(ReadView view) {
        super.readData(view);
        Identifier id = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        Fluid fluid = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        storage.load(fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid),
            view.getLong("FluidAmount", 0));
        coolingTicks = view.getInt("CoolingTicks", 0);
    }

    @Override protected void writeData(WriteView view) {
        super.writeData(view);
        if (!storage.variantView().isBlank() && storage.amountView() > 0) {
            view.putString("FluidId", Registries.FLUID.getId(storage.variantView().getFluid()).toString());
            view.putLong("FluidAmount", storage.amountView());
        }
        view.putInt("CoolingTicks", coolingTicks);
    }
}
