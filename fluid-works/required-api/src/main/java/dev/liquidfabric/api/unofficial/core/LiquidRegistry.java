package dev.liquidfabric.api.unofficial.core;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class LiquidRegistry {
    public record LiquidType(Identifier id, int color, boolean drinkable, boolean potionLike, boolean canBeNeedled) {}

    private static final Map<Identifier, LiquidType> TYPES = new LinkedHashMap<>();

    private LiquidRegistry() {}

    public static LiquidType register(Identifier id, int color, boolean drinkable, boolean potionLike) {
        return register(id, color, drinkable, potionLike, true);
    }

    public static LiquidType register(Identifier id, int color, boolean drinkable, boolean potionLike, boolean canBeNeedled) {
        LiquidType type = new LiquidType(id, color, drinkable, potionLike, canBeNeedled);
        TYPES.put(id, type);
        return type;
    }

    public static Optional<LiquidType> get(Identifier id) {
        return Optional.ofNullable(TYPES.get(id));
    }

    public static Collection<LiquidType> values() {
        return TYPES.values();
    }

    public static void bootstrapDefaults() {
        if (!TYPES.isEmpty()) return;
        register(Identifier.of("minecraft", "water"), 0x3F76E4, true, false, true);
        register(UtilityApiMod.id("milk"), 0xFFFFFF, true, false, true);
        register(UtilityApiMod.id("chocolate_milk"), 0x7B4A2B, true, false, true);
        register(UtilityApiMod.id("hot_chocolate"), 0x9C5A2E, true, false, true);
        register(UtilityApiMod.id("potion"), 0x8F4CFF, true, true, true);
    }
}
