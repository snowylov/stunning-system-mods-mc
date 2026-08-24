package dev.liquidfabric.api.unofficial.core;

import dev.liquidfabric.api.unofficial.api.block.BlockFluidContainerDefinition;
import dev.liquidfabric.api.unofficial.api.block.BlockFluidContainerRegistry;
import dev.liquidfabric.api.unofficial.api.container.CustomFluidContainerItemRegistry;
import dev.liquidfabric.api.unofficial.api.container.FluidContainerDefinition;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/** Stable addon-facing registration surface for containers and block storage. */
public final class UtilityApiRegistries {
    private UtilityApiRegistries() {
    }

    public static void registerCustomFluidContainerItem(Identifier hookId, Item item,
                                                        FluidContainerDefinition definition) {
        CustomFluidContainerItemRegistry.register(hookId, item, definition);
    }

    public static boolean unregisterCustomFluidContainerItem(Identifier hookId) {
        return CustomFluidContainerItemRegistry.unregister(hookId);
    }

    public static void registerFluidContainerBlock(Identifier hookId, Block block,
                                                   BlockFluidContainerDefinition definition) {
        BlockFluidContainerRegistry.register(hookId, block, definition);
    }

    public static void registerFluidContainerBlock(Identifier hookId, Block block,
                                                   BlockFluidContainerDefinition definition,
                                                   BlockFluidContainerRegistry.StorageProvider provider) {
        BlockFluidContainerRegistry.register(hookId, block, definition, provider);
    }

    public static boolean unregisterFluidContainerBlock(Identifier hookId) {
        return BlockFluidContainerRegistry.unregister(hookId);
    }

    public static <T extends BlockEntity> void registerFluidStorage(BlockEntityType<T> type,
            BiFunction<T, @Nullable Direction, @Nullable Storage<FluidVariant>> provider) {
        FluidStorage.SIDED.registerForBlockEntity(provider::apply, type);
    }
}
