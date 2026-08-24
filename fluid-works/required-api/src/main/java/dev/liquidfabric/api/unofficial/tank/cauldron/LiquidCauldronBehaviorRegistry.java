package dev.liquidfabric.api.unofficial.tank.cauldron;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Registry for liquids allowed in Utility's compatibility cauldron layer.
 *
 * This stays intentionally small: only potion liquids and Utility registered
 * liquid containers use it. It does not replace vanilla cauldron behavior for
 * water/lava/powder snow and does not use mixins.
 */
public final class LiquidCauldronBehaviorRegistry {
    private static final Map<Identifier, Entry> ENTRIES = new LinkedHashMap<>();

    private LiquidCauldronBehaviorRegistry() {}

    public static void register(Identifier id, Predicate<Identifier> matcher, int color) {
        ENTRIES.put(id, new Entry(id, matcher, color));
    }

    public static void bootstrapDefaults() {
        register(UtilityApiMod.id("potion_liquids"), liquid -> liquid.equals(net.minecraft.util.Identifier.ofVanilla("potion")) || liquid.getPath().startsWith("potion/") || liquid.getPath().startsWith("potion_"), 0x8F3FEA);
        register(UtilityApiMod.id("milk"), liquid -> liquid.equals(UtilityApiMod.id("milk")), 0xF4F4F4);
        register(UtilityApiMod.id("chocolate_milk"), liquid -> liquid.equals(UtilityApiMod.id("chocolate_milk")), 0x7A4A2A);
        register(UtilityApiMod.id("hot_chocolate"), liquid -> liquid.equals(UtilityApiMod.id("hot_chocolate")), 0x8A4F30);
    }

    public static boolean canStore(Identifier liquid) {
        for (Entry entry : ENTRIES.values()) {
            if (entry.matches(liquid)) return true;
        }
        return false;
    }

    public static Map<Identifier, Entry> entries() {
        return Collections.unmodifiableMap(ENTRIES);
    }

    public record Entry(Identifier id, Predicate<Identifier> matcher, int color) {
        public boolean matches(Identifier liquid) {
            return matcher.test(liquid);
        }
    }
}
