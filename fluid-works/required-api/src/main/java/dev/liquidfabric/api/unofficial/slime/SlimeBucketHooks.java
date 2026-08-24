package dev.liquidfabric.api.unofficial.slime;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.bucket.BucketEntityCaptureRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Small slime bucket registration.
 *
 * Capture is routed through the generic BucketEntityCaptureRegistry so addon mods
 * can add their own entity buckets without mixins or global vanilla replacement.
 */
public final class SlimeBucketHooks {
    public static SlimeBucketItem SLIME_BUCKET;

    private SlimeBucketHooks() {}

    public static void register() {
        SLIME_BUCKET = Registry.register(
                Registries.ITEM,
                UtilityApiMod.id("slime_bucket"),
                new SlimeBucketItem(new Item.Settings().maxCount(1))
        );

        BucketEntityCaptureRegistry.register(
                UtilityApiMod.id("small_slime_bucket_capture"),
                EntityType.SLIME,
                SLIME_BUCKET,
                slime -> slime instanceof SlimeEntity slimeEntity && slimeEntity.getSize() == 1,
                100
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(SLIME_BUCKET));
    }
}
