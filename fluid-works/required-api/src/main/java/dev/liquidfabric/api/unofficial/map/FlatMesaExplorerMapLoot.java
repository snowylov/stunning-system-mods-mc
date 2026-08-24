package dev.liquidfabric.api.unofficial.map;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.util.Identifier;

/**
 * Adds Flat Mesa Explorer Maps to abandoned mineshaft loot.
 *
 * Vanilla uses the same abandoned_mineshaft chest table for badlands mineshafts,
 * so this injection is intentionally rare. The map itself identifies it as a
 * Badlands/Flat Mesa explorer item.
 */
public final class FlatMesaExplorerMapLoot {
    private static boolean registered;
    private static final Identifier ABANDONED_MINESHAFT = Identifier.ofVanilla("chests/abandoned_mineshaft");

    private FlatMesaExplorerMapLoot() {}

    public static void register() {
        if (registered) return;
        registered = true;

        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) return;
            if (!key.getValue().equals(ABANDONED_MINESHAFT)) return;

            LootPool pool = LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1.0F))
                    .conditionally(RandomChanceLootCondition.builder(0.18F))
                    .with(ItemEntry.builder(ModUtilityMaps.FLAT_MESA_EXPLORER_MAP)
                            .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(1.0F))))
                    .build();

            tableBuilder.pool(pool);
        });
    }
}
