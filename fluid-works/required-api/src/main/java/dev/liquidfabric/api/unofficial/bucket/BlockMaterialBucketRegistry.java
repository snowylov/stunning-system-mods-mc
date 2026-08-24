package dev.liquidfabric.api.unofficial.bucket;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tag/data friendly block-material bucket registry.
 *
 * This is for "scoopable block materials" that are not real Fluid API fluids:
 * resin clumps, mud, slimes, tar chunks, packed gels, etc.
 *
 * Compatibility rules:
 * - Tags are the extension point, not hardcoded block checks.
 * - Entries are id-addressable so addons can replace/remove only their own hook.
 * - Higher priority wins when multiple mods intentionally target the same block tag.
 */
public final class BlockMaterialBucketRegistry {
    public record Entry(Identifier id, TagKey<Block> pickupTag, Item bucketItem, Block placementBlock, int priority) {}

    public static final TagKey<Block> RESIN_PICKUP = TagKey.of(RegistryKeys.BLOCK, UtilityApiMod.id("bucket_pickup/resin"));
    public static final TagKey<Block> MUD_PICKUP = TagKey.of(RegistryKeys.BLOCK, UtilityApiMod.id("bucket_pickup/mud"));

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private BlockMaterialBucketRegistry() {}

    public static void register(TagKey<Block> pickupTag, Item bucketItem, Block placementBlock) {
        register(UtilityApiMod.id("bucket_material/" + pickupTag.id().getPath().replace('/', '_')), pickupTag, bucketItem, placementBlock, 0);
    }

    public static void register(Identifier id, TagKey<Block> pickupTag, Item bucketItem, Block placementBlock, int priority) {
        unregister(id);
        ENTRIES.add(new Entry(id, pickupTag, bucketItem, placementBlock, priority));
        ENTRIES.sort(Comparator.comparingInt(Entry::priority).reversed());
    }

    public static boolean unregister(Identifier id) {
        return ENTRIES.removeIf(entry -> entry.id().equals(id));
    }

    public static Optional<Entry> match(BlockState state) {
        return ENTRIES.stream().filter(entry -> state.isIn(entry.pickupTag())).findFirst();
    }

    public static void bootstrapVanillaLikeDefaults() {
        // Resin/mud bucket items register themselves from their content bootstrap.
        // Kept empty on purpose so the API can load before optional content modules.
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES);
    }
}
