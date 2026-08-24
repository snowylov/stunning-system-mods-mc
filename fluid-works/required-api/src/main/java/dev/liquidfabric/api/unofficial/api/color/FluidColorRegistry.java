package dev.liquidfabric.api.unofficial.api.color;

import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public color registry for bucket/container overlays, gauges, meters, recipes,
 * and addon UI. This avoids each addon inventing incompatible fluid color logic.
 */
public final class FluidColorRegistry {
    public static final int DEFAULT_COLOR = 0xFF_FFFFFF;
    private static final Map<Identifier, Integer> COLORS = new ConcurrentHashMap<>();

    private FluidColorRegistry() {}

    public static void register(Identifier id, int rgb) {
        COLORS.put(id, normalize(rgb));
    }

    public static void unregister(Identifier id) {
        COLORS.remove(id);
    }

    public static OptionalInt get(Identifier id) {
        Integer color = COLORS.get(id);
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }

    public static int resolve(Identifier id, int fallbackRgb) {
        return COLORS.getOrDefault(id, normalize(fallbackRgb));
    }

    public static int resolve(StoredFluidComponent component, int fallbackRgb) {
        return component == null || component.isEmpty() ? fallbackRgb : resolve(component.liquidId(), fallbackRgb);
    }

    public static int resolve(FluidVariant variant, int fallbackRgb) {
        if (variant == null || variant.isBlank()) return fallbackRgb;
        Identifier id = Registries.FLUID.getId(variant.getFluid());
        return resolve(id, fallbackRgb);
    }

    public static void bootstrapDefaults() {
        register(Identifier.of("minecraft", "water"), 0x3F76E4);
        register(Identifier.of("minecraft", "lava"), 0xFF6A00);
        register(Identifier.of("minecraft", "milk"), 0xF7F7F0);
        register(Identifier.of("liquid-fabric-api-unofficial-fabric-api", "milk"), 0xF7F7F0);
        register(Identifier.of("liquid-fabric-api-unofficial-fabric-api", "chocolate_milk"), 0x7A4A2A);
        register(Identifier.of("liquid-fabric-api-unofficial-fabric-api", "hot_chocolate"), 0x8A4D2E);
    }

    private static int normalize(int rgb) {
        return rgb & 0x00_FFFFFF;
    }
}
