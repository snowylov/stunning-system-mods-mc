package dev.liquidfabric.api.unofficial.client;

import dev.liquidfabric.api.unofficial.api.container.CustomFluidContainerItemRegistry;
import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.LinkedHashSet;
import java.util.Set;

public final class UtilityItemColorProviders {
    private UtilityItemColorProviders() {
    }

    public static void register() {
        Set<Item> items = new LinkedHashSet<>();
        Registries.ITEM.forEach(item -> {
            if (item instanceof FluidOverlayItem) items.add(item);
        });
        CustomFluidContainerItemRegistry.values().forEach(entry -> items.add(entry.item()));
        items.forEach(item -> registerOverlayItem(item, CustomFluidContainerItemRegistry.find(item)
            .map(entry -> entry.definition().fluidOverlayTintIndex()).orElse(1)));
    }

    public static void registerOverlayItem(Item item, int tintIndex) {
        ColorProviderRegistry.ITEM.register((stack, layer) -> {
            if (layer != tintIndex) return -1;
            StoredFluidComponent stored = stack.getOrDefault(ModComponents.STORED_FLUID,
                StoredFluidComponent.EMPTY);
            if (stored.isEmpty()) return 0xFFFFFFFF;
            String id = stored.liquidId().toString();
            if (id.equals("minecraft:water")) return 0xFF3F76E4;
            if (id.equals("minecraft:lava")) return 0xFFFF6A00;
            int hash = id.hashCode();
            return 0xFF000000 | 0x404040 | (hash & 0xBFBFBF);
        }, item);
    }
}
