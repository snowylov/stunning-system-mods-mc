package dev.liquidfabric.api.unofficial.map;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModUtilityMaps {
    public static FlatMesaExplorerMapItem FLAT_MESA_EXPLORER_MAP;

    private ModUtilityMaps() {}

    public static void register() {
        FLAT_MESA_EXPLORER_MAP = Registry.register(
                Registries.ITEM,
                UtilityApiMod.id("flat_mesa_explorer_map"),
                new FlatMesaExplorerMapItem(new Item.Settings())
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(FLAT_MESA_EXPLORER_MAP));
    }
}
