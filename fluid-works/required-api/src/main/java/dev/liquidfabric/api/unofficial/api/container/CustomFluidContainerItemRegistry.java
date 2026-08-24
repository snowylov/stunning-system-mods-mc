package dev.liquidfabric.api.unofficial.api.container;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry hook that opts any item into component-backed fluid storage.
 * Register during mod initialization, before the client item color providers run.
 */
public final class CustomFluidContainerItemRegistry {
    public record Entry(Identifier id, Item item, FluidContainerDefinition definition) {}

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private CustomFluidContainerItemRegistry() {}

    public static void register(Identifier id, Item item, FluidContainerDefinition definition) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(definition, "definition");
        unregister(id);
        ENTRIES.removeIf(entry -> entry.item() == item);
        ENTRIES.add(new Entry(id, item, definition));
    }

    public static boolean unregister(Identifier id) {
        return ENTRIES.removeIf(entry -> entry.id().equals(id));
    }

    public static Optional<Entry> find(Item item) {
        return ENTRIES.stream().filter(entry -> entry.item() == item).findFirst();
    }

    public static Optional<Entry> find(ItemStack stack) {
        return stack == null || stack.isEmpty() ? Optional.empty() : find(stack.getItem());
    }

    public static boolean isRegistered(Item item) {
        return find(item).isPresent();
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES);
    }
}
