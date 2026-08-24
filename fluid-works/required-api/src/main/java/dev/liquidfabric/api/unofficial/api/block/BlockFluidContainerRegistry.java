package dev.liquidfabric.api.unofficial.api.block;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Additive hook for blocks that expose fluid storage and optional in-block rendering metadata. */
public final class BlockFluidContainerRegistry {
    @FunctionalInterface
    public interface StorageProvider {
        @Nullable Storage<FluidVariant> find(BlockEntity blockEntity, @Nullable Direction side);
    }

    public record Entry(Identifier id, Block block, BlockFluidContainerDefinition definition,
                        @Nullable StorageProvider storageProvider) {}

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private BlockFluidContainerRegistry() {}

    public static void register(Identifier id, Block block, BlockFluidContainerDefinition definition) {
        register(id, block, definition, (blockEntity, side) -> blockEntity instanceof FluidStorageBlockEntity fluidBlock
                ? fluidBlock.liquidFabricStorage() : null);
    }

    public static void register(Identifier id, Block block, BlockFluidContainerDefinition definition,
                                @Nullable StorageProvider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(definition, "definition");
        unregister(id);
        ENTRIES.removeIf(entry -> entry.block() == block);
        ENTRIES.add(new Entry(id, block, definition, provider));
    }

    public static boolean unregister(Identifier id) {
        return ENTRIES.removeIf(entry -> entry.id().equals(id));
    }

    public static Optional<Entry> find(Block block) {
        return ENTRIES.stream().filter(entry -> entry.block() == block).findFirst();
    }

    public static @Nullable Storage<FluidVariant> findStorage(BlockEntity blockEntity, @Nullable Direction side) {
        if (blockEntity == null) return null;
        return find(blockEntity.getCachedState().getBlock())
                .map(Entry::storageProvider)
                .map(provider -> provider == null ? null : provider.find(blockEntity, side))
                .orElse(null);
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES);
    }
}
