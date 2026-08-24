package dev.liquidfabric.api.unofficial.needle;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModNeedles {
    public static BaseNeedleItem NEEDLE;
    public static SyringeItem SYRINGE;
    public static NeedleGunItem NEEDLE_GUN;
    public static EntityType<NeedleProjectileEntity> NEEDLE_PROJECTILE;

    private ModNeedles() {}

    public static void register() {
        NeedleEffectRegistry.bootstrapDefaults();

        NEEDLE = Registry.register(Registries.ITEM, UtilityApiMod.id("needle"), new BaseNeedleItem(new Item.Settings().maxCount(64)));
        SYRINGE = Registry.register(Registries.ITEM, UtilityApiMod.id("syringe"), new SyringeItem(new Item.Settings().maxCount(16)));
        NEEDLE_GUN = Registry.register(Registries.ITEM, UtilityApiMod.id("needle_gun"), new NeedleGunItem(new Item.Settings()));

        NEEDLE_PROJECTILE = Registry.register(
                Registries.ENTITY_TYPE,
                UtilityApiMod.id("needle_projectile"),
                EntityType.Builder.<NeedleProjectileEntity>create(NeedleProjectileEntity::new, SpawnGroup.MISC)
                        .dimensions(0.05f, 0.05f)
                        .maxTrackingRange(4)
                        .trackingTickInterval(20)
                        .build(UtilityApiMod.id("needle_projectile").toString())
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(NEEDLE_GUN);
            entries.add(NEEDLE);
            entries.add(SYRINGE);
        });
    }
}
