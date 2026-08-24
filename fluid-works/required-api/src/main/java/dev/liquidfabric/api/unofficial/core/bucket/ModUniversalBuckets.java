package dev.liquidfabric.api.unofficial.core.bucket;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModUniversalBuckets {
    public static UniversalFluidBucketItem UNIVERSAL_FLUID_BUCKET;

    private ModUniversalBuckets() {}

    public static void register() {
        UNIVERSAL_FLUID_BUCKET = Registry.register(
                Registries.ITEM,
                UtilityApiMod.id("fluid_bucket"),
                new UniversalFluidBucketItem(new Item.Settings())
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(UNIVERSAL_FLUID_BUCKET));
    }
}
