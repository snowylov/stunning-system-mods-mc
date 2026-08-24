package dev.liquidfabric.api.unofficial.needle.station;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class NeedleFillingStation {
    public static Block NEEDLE_FILLING_STATION;

    private NeedleFillingStation() {}

    public static void register() {
        NEEDLE_FILLING_STATION = Registry.register(
                Registries.BLOCK,
                UtilityApiMod.id("needle_filling_station"),
                new NeedleFillingStationBlock(AbstractBlock.Settings.create().strength(1.5F, 6.0F))
        );

        Registry.register(
                Registries.ITEM,
                UtilityApiMod.id("needle_filling_station"),
                new BlockItem(NEEDLE_FILLING_STATION, new Item.Settings())
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(NEEDLE_FILLING_STATION));
    }
}
