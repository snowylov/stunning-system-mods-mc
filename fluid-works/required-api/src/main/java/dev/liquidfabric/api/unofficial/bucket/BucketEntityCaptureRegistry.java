package dev.liquidfabric.api.unofficial.bucket;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Registry-first fish-bucket style entity capture API.
 *
 * Compatibility rules:
 * - Entries are identified by id so addons can replace/remove their own hook.
 * - Higher priority wins when multiple mods target the same entity type.
 * - The actual vanilla bucket callback lives in BucketEntityCaptureHooks and is
 *   disabled by config unless a pack opts in.
 */
public final class BucketEntityCaptureRegistry {
    public interface FilledBucketFactory<T extends Entity> {
        ItemStack create(ServerWorld world, T entity);
    }

    public record Entry<T extends Entity>(
            Identifier id,
            EntityType<T> type,
            Predicate<T> predicate,
            FilledBucketFactory<T> factory,
            int priority
    ) {}

    private static final CopyOnWriteArrayList<Entry<?>> ENTRIES = new CopyOnWriteArrayList<>();

    private BucketEntityCaptureRegistry() {}

    public static <T extends Entity> void register(EntityType<T> type, Item bucketItem, Predicate<T> predicate) {
        Identifier id = Registries.ENTITY_TYPE.getId(type).withSuffixedPath("_bucket_capture");
        register(id, type, predicate, (world, entity) -> new ItemStack(bucketItem), 0);
    }

    public static <T extends Entity> void register(EntityType<T> type, Predicate<T> predicate, FilledBucketFactory<T> factory) {
        Identifier id = Registries.ENTITY_TYPE.getId(type).withSuffixedPath("_bucket_capture");
        register(id, type, predicate, factory, 0);
    }

    public static <T extends Entity> void register(Identifier id, EntityType<T> type, Item bucketItem, Predicate<T> predicate, int priority) {
        register(id, type, predicate, (world, entity) -> new ItemStack(bucketItem), priority);
    }

    public static <T extends Entity> void register(Identifier id, EntityType<T> type, Predicate<T> predicate, FilledBucketFactory<T> factory, int priority) {
        unregister(id);
        ENTRIES.add(new Entry<>(id, type, predicate, factory, priority));
        ENTRIES.sort(Comparator.comparingInt((Entry<?> e) -> e.priority()).reversed());
    }

    public static boolean unregister(Identifier id) {
        return ENTRIES.removeIf(entry -> entry.id().equals(id));
    }

    @SuppressWarnings("unchecked")
    public static Optional<ItemStack> tryCreate(ServerWorld world, Entity entity) {
        for (Entry<?> raw : ENTRIES) {
            Entry<Entity> entry = (Entry<Entity>) raw;
            if (entry.type() == entity.getType() && entry.predicate().test(entity)) {
                ItemStack stack = entry.factory().create(world, entity);
                if (!stack.isEmpty()) return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    public static List<Entry<?>> values() {
        return List.copyOf(ENTRIES);
    }
}
