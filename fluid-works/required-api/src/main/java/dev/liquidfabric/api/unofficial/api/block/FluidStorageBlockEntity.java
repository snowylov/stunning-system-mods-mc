package dev.liquidfabric.api.unofficial.api.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;

/** Implement on block entities that want automatic API storage discovery. */
public interface FluidStorageBlockEntity {
    Storage<FluidVariant> liquidFabricStorage();
}
