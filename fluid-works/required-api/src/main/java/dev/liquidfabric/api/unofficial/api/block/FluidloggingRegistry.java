package dev.liquidfabric.api.unofficial.api.block;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Registry for mixin-free, one-fluid-per-block fluidlogging definitions. */
public final class FluidloggingRegistry {
    public record Entry(Identifier id, Block block, Fluid fluid, String propertyName) {}

    private static final CopyOnWriteArrayList<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private FluidloggingRegistry() {}

    public static void register(Identifier id, Block block, Fluid fluid, String propertyName) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(fluid, "fluid");
        if (propertyName == null || propertyName.isBlank()) throw new IllegalArgumentException("propertyName cannot be blank");
        unregister(id);
        ENTRIES.removeIf(entry -> entry.block() == block);
        ENTRIES.add(new Entry(id, block, fluid, propertyName));
    }

    public static boolean unregister(Identifier id) {
        return ENTRIES.removeIf(entry -> entry.id().equals(id));
    }

    public static Optional<Entry> find(Block block) {
        return ENTRIES.stream().filter(entry -> entry.block() == block).findFirst();
    }

    public static List<Entry> values() {
        return List.copyOf(ENTRIES);
    }
}
