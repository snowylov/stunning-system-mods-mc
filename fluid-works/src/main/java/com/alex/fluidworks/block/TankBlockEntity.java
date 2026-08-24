package com.alex.fluidworks.block;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.fluid.FluidWorksStorage;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.Registries;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TankBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    private final FluidWorksStorage storage;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.TANK_BLOCK_ENTITY, pos, state);
        long capacity = state.getBlock() instanceof TankBlock tank ? tank.capacityDroplets()
            : state.getBlock() instanceof StackableTankBlock ? StackableTankBlock.CAPACITY_PER_BLOCK : 0;
        storage = new FluidWorksStorage(this, capacity);
    }

    public FluidWorksStorage storage() {
        return storage;
    }

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        return getCachedState().getBlock() instanceof StackableTankBlock
            ? new VerticalTankStorage(columnStorages()) : storage;
    }

    private List<FluidWorksStorage> columnStorages() {
        if (world == null) return List.of(storage);
        BlockPos.Mutable cursor = pos.mutableCopy();
        int scanned = 0;
        while (scanned++ < 64 && world.getBlockState(cursor.down()).getBlock() instanceof StackableTankBlock) {
            cursor.move(net.minecraft.util.math.Direction.DOWN);
        }
        List<FluidWorksStorage> result = new ArrayList<>();
        scanned = 0;
        while (scanned++ < 64 && world.getBlockState(cursor).getBlock() instanceof StackableTankBlock) {
            if (world.getBlockEntity(cursor) instanceof TankBlockEntity tank) result.add(tank.storage());
            cursor.move(net.minecraft.util.math.Direction.UP);
        }
        return result.isEmpty() ? List.of(storage) : result;
    }

    private static final class VerticalTankStorage implements Storage<FluidVariant> {
        private final List<FluidWorksStorage> parts;

        private VerticalTankStorage(List<FluidWorksStorage> parts) {
            this.parts = parts;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource == null || resource.isBlank() || maxAmount <= 0 || hasDifferentFluid(resource)) return 0;
            long inserted = 0;
            for (FluidWorksStorage part : parts) {
                if (inserted >= maxAmount) break;
                inserted += part.insert(resource, maxAmount - inserted, transaction);
            }
            return inserted;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource == null || resource.isBlank() || maxAmount <= 0) return 0;
            long extracted = 0;
            for (int i = parts.size() - 1; i >= 0 && extracted < maxAmount; i--) {
                extracted += parts.get(i).extract(resource, maxAmount - extracted, transaction);
            }
            return extracted;
        }

        private boolean hasDifferentFluid(FluidVariant resource) {
            for (FluidWorksStorage part : parts) {
                if (!part.variantView().isBlank() && part.amountView() > 0
                    && !part.variantView().equals(resource)) return true;
            }
            return false;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            List<StorageView<FluidVariant>> views = new ArrayList<>();
            for (FluidWorksStorage part : parts) part.forEach(views::add);
            return views.iterator();
        }
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
