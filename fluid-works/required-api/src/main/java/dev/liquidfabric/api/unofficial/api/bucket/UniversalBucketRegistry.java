package dev.liquidfabric.api.unofficial.api.bucket;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Maps a fluid to an already-registered component-backed universal bucket item. */
public final class UniversalBucketRegistry {
    public record Entry(Identifier id, Identifier fluidId, Item bucketItem, int priority) {}

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private UniversalBucketRegistry() {}

    public static void register(Identifier id, Identifier fluidId, Item bucketItem, int priority) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fluidId, "fluidId");
        Objects.requireNonNull(bucketItem, "bucketItem");
        unregister(id);
        ENTRIES.add(new Entry(id, fluidId, bucketItem, priority));
        ENTRIES.sort(Comparator.comparingInt(Entry::priority).reversed());
    }

    public static boolean unregister(Identifier id) {
        return ENTRIES.removeIf(entry -> entry.id().equals(id));
    }

    public static Optional<Entry> find(Identifier fluidId) {
        return ENTRIES.stream().filter(entry -> entry.fluidId().equals(fluidId)).findFirst();
    }

    public static ItemStack createFilledStack(Fluid fluid, SourceFluidAttributes attributes) {
        Identifier fluidId = Registries.FLUID.getId(fluid);
        if (fluidId == null) return ItemStack.EMPTY;
        return find(fluidId).map(entry -> {
            ItemStack stack = new ItemStack(entry.bucketItem());
            long capacity = FluidItemComponentHelper.capacity(stack);
            if (capacity <= 0) return ItemStack.EMPTY;
            FluidItemComponentHelper.set(stack, fluidId, capacity, attributes);
            return FluidItemComponentHelper.hasFluid(stack) ? stack : ItemStack.EMPTY;
        }).orElse(ItemStack.EMPTY);
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES);
    }
}
